package com.laderrco.fortunelink.portfolio.application.services;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioDeletionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioDeletionRequiresConfirmationException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioLimitReachedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// ONLY PORTFOLIO + ACCOUNT LIFECYCLE STUFF, NOT TRANSACTION
@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioLifecycleService {
    private final PortfolioRepository portfolioRepository;
    private final PortfolioViewMapper portfolioViewMapper;

    private final MarketDataService marketDataService;

    private final PortfolioLifecycleCommandValidator validator;

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

        // Portfolio has existing positions - need real quotes
        Set<AssetSymbol> symbols = extractSymbols(saved);
        Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);
        return portfolioViewMapper.toPortfolioView(saved, saved.getDisplayCurrency(), quoteCache);

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

            } catch (IllegalStateException e) {
                if (e.getMessage().contains("Portfolio is already deleted")) {
                    throw new PortfolioDeletionException("Portfolio already deleted");
                } else {
                    throw new PortfolioNotEmptyException("Portfolio is not empty and can't be deleted");
                }
            }
        } else {
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

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(), command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found or access denied for ID: " + command.portfolioId()));

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
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found or access denied for ID: " + portfolioId));
        return portfolio;
    }

    private Set<AssetSymbol> extractSymbols(Portfolio portfolio) {
        return portfolio.getAccounts().stream()
                .flatMap(account -> account.getPositionEntries().stream().map(Map.Entry::getKey))
                .collect(Collectors.toSet());
    }

    private <T> void validate(T command, Function<T, ValidationResult> validationLogic, String methodName) {
        ValidationResult result = validationLogic.apply(command);
        if (!result.isValid()) {
            String msg = String.format("Invalid %s command", methodName);
            throw new InvalidTransactionException(msg, result.errors());
        }
    }
}
