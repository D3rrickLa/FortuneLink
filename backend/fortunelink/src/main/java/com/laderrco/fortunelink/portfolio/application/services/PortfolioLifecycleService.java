package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.commands.*;
import com.laderrco.fortunelink.portfolio.application.exceptions.*;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioServiceUtils;
import com.laderrco.fortunelink.portfolio.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioAlreadyDeletedException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

// ONLY PORTFOLIO + ACCOUNT LIFECYCLE STUFF, NOT TRANSACTION
@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioLifecycleService {
    private final PortfolioRepository portfolioRepository;
    private final PortfolioViewMapper portfolioViewMapper;

    private final MarketDataService marketDataService;
    private final PortfolioValuationService portfolioValuationService;

    private final PortfolioLifecycleCommandValidator validator;
    private final AccountViewBuilder accountViewBuilder;

    private static final int MAX_PORTFOLIOS_PER_USER = 1; // MVP constraint
    private static final String DEFAULT_NAME = "Default Account";

    public PortfolioView createPortfolio(CreatePortfolioCommand command) {
        validate(command, validator::validate, "createPortfolio");

        long currentCount = portfolioRepository.countByUserId(command.userId());

        if (currentCount >= MAX_PORTFOLIOS_PER_USER) {
            throw new PortfolioLimitReachedException("Already reached max allowed portfolio limit");
        }

        Portfolio portfolio = Portfolio.createNew(command.userId(), command.name(),
                command.description(), command.currency());

        if (command.createDefaultAccount()) {

            portfolio.createAccount(
                    DEFAULT_NAME,
                    AccountType.NON_REGISTERED_INVESTMENT,
                    command.currency(),
                    command.defaultStrategy());
        }

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return portfolioViewMapper.toNewPortfolioView(savedPortfolio);

    }

    public PortfolioView updatePortfolio(UpdatePortfolioCommand command) {
        validate(command, validator::validate, "updatePortfolio");

        Portfolio existingPortfolio = getPortfolio(command.portfolioId(), command.userId());

        existingPortfolio.updateDetails(command.name(), command.description());
        existingPortfolio.updateDisplayCurrency(command.currency());

        Portfolio saved = portfolioRepository.save(existingPortfolio);

        Set<AssetSymbol> symbols = PortfolioServiceUtils.extractSymbols(saved);
        Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);
        Money totalValue = portfolioValuationService.calculateTotalValue(saved, saved.getDisplayCurrency(), quoteCache);

        List<AccountView> accountViews = saved.getAccounts().stream()
                .map(account -> accountViewBuilder.build(account, quoteCache))
                .toList();

        return portfolioViewMapper.toPortfolioView(saved, accountViews, totalValue);

    }

    public void deletePortfolio(DeletePortfolioCommand command) {
        validate(command, validator::validate, "deletePortfolio");

        if (!command.confirmed()) {
            throw new PortfolioDeletionRequiresConfirmationException();
        }

        Portfolio portfolio = getPortfolio(command.portfolioId(), command.userId());

        if (command.softDelete()) {
            try {
                portfolio.markAsDeleted(command.userId());
                portfolioRepository.save(portfolio);
            } catch (PortfolioAlreadyDeletedException e) {
                throw new PortfolioDeletionException("Portfolio already deleted");
            } catch (IllegalStateException e) {
                // Catch-all — something unexpected from markAsDeleted
                throw new PortfolioDeletionException("Cannot delete portfolio: " + e.getMessage());
            }
        } else {
            // intentionally bypasses the markAsDeleted checks
            // if a user wants to 'start over' they don't want to close
            // all the accounts
            portfolioRepository.delete(command.portfolioId());
        }

    }

    public AccountView createAccount(CreateAccountCommand command) {
        validate(command, validator::validate, "createAccount");

        Portfolio portfolio = getPortfolio(command.portfolioId(), command.userId());

        Account account = portfolio.createAccount(
                command.accountName(),
                command.accountType(),
                command.baseCurrency(),
                command.strategy());

        portfolioRepository.save(portfolio);

        return portfolioViewMapper.toNewAccountView(account);
    }

    public void updateAccount(UpdateAccountCommand command) {
        validate(command, validator::validate, "updateAccount");

        Portfolio portfolio = getPortfolio(command.portfolioId(), command.userId());

        portfolio.renameAccount(command.accountId(), command.accountName());

        portfolioRepository.save(portfolio);
    }

    public void deleteAccount(DeleteAccountCommand command) {
        validate(command, validator::validate, "deleteAccount");
        // always soft deletes
        Portfolio portfolio = getPortfolio(command.portfolioId(), command.userId());

        portfolio.closeAccount(command.accountId());
        portfolioRepository.save(portfolio);
    }

    private Portfolio getPortfolio(PortfolioId portfolioId, UserId userId) {
        return portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() ->
                        new PortfolioNotFoundException("Portfolio not found or access denied for ID: " + portfolioId));
    }

    private <T> void validate(T command, Function<T, ValidationResult> validationLogic, String methodName) {
        ValidationResult result = validationLogic.apply(command);
        if (!result.isValid()) {
            String msg = String.format("Invalid %s command", methodName);
            throw new InvalidCommandException(msg, result.errors());
        }
    }
}
