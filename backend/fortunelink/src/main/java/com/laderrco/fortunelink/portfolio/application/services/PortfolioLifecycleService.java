package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.commands.*;
import com.laderrco.fortunelink.portfolio.application.exceptions.*;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioAlreadyDeletedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioLifecycleService {
  private static final int MAX_PORTFOLIOS_PER_USER = 1;
  private static final String DEFAULT_NAME = "Default Account";

  private final PortfolioRepository portfolioRepository;
  private final TransactionRepository transactionRepository;
  private final PortfolioViewMapper portfolioViewMapper;

  private final MarketDataService marketDataService;
  private final PortfolioValuationService portfolioValuationService;

  private final PortfolioLifecycleCommandValidator validator;
  private final AccountViewBuilder accountViewBuilder;

  public PortfolioView createPortfolio(CreatePortfolioCommand command) {
    validate(command, validator::validate, "createPortfolio");

    // Bug 13: countByUserId MUST exclude soft-deleted portfolios.
    // See PortfolioRepository.countByUserId() contract comment.
    long currentCount = portfolioRepository.countByUserId(command.userId());
    if (currentCount >= MAX_PORTFOLIOS_PER_USER) {
      throw new PortfolioLimitReachedException("Already reached max allowed portfolio limit");
    }

    Portfolio portfolio = Portfolio.createNew(command.userId(), command.name(),
        command.description(), command.currency());

    if (command.createDefaultAccount()) {

      portfolio.createAccount(DEFAULT_NAME, AccountType.NON_REGISTERED_INVESTMENT,
          command.currency(), command.defaultStrategy());
    }

    Portfolio savedPortfolio = portfolioRepository.save(portfolio);
    return portfolioViewMapper.toNewPortfolioView(savedPortfolio);

  }

  public PortfolioView updatePortfolio(UpdatePortfolioCommand command) {

    validate(command, validator::validate, "updatePortfolio");

    Portfolio existingPortfolio = loadUserPortfolio(command.portfolioId(), command.userId());

    existingPortfolio.updateDetails(command.name(), command.description());
    existingPortfolio.updateDisplayCurrency(command.currency());

    Portfolio saved = portfolioRepository.save(existingPortfolio);

    Set<AssetSymbol> symbols = PortfolioAccessUtils.extractSymbols(saved);
    Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);

    Money totalValue = portfolioValuationService.calculateTotalValue(
        saved,
        saved.getDisplayCurrency(),
        quoteCache);

    // NEW: batch fee query
    List<AccountId> accountIds = saved.getAccounts().stream()
        .map(Account::getAccountId)
        .toList();

    Map<AccountId, Map<AssetSymbol, Money>> feeCache = transactionRepository
        .sumBuyFeesByAccountAndSymbol(accountIds);

    List<AccountView> accountViews = saved.getAccounts().stream()
        .map(account -> accountViewBuilder.build(
            account,
            quoteCache,
            feeCache.getOrDefault(account.getAccountId(), Map.of())))
        .toList();

    return portfolioViewMapper.toPortfolioView(saved, accountViews, totalValue);
  }

  public void deletePortfolio(DeletePortfolioCommand command) {
    validate(command, validator::validate, "deletePortfolio");

    if (!command.confirmed()) {
      throw new PortfolioDeletionRequiresConfirmationException();
    }

    // TODO: confirm is this is true/needed
    // NOTE: deletePortfolio intentionally calls the raw repository lookup,
    // not loadUserPortfolio(), because we need to allow the user to hard-delete
    // a portfolio that is already soft-deleted (cleanup path).
    Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(), command.userId())
        .orElseThrow(() -> new PortfolioNotFoundException(
            "Portfolio not found or access denied for ID: " + command.portfolioId()));

    if (command.softDelete()) {
      try {
        portfolio.markAsDeleted(command.userId());
        portfolioRepository.save(portfolio);
      } catch (PortfolioAlreadyDeletedException e) {
        throw new PortfolioDeletionException("Portfolio already deleted");
      } catch (PortfolioNotEmptyException e) {
        throw new PortfolioDeletionException(e.getMessage());
      } catch (IllegalStateException e) {
        throw new PortfolioDeletionException("Cannot delete portfolio: " + e.getMessage());
      }
    } else {
      // Hard delete intentionally bypasses the markAsDeleted checks.
      // The user wants to start over and doesn't want to close all accounts first.
      portfolioRepository.delete(command.portfolioId());
    }

  }

  public AccountView createAccount(CreateAccountCommand command) {
    validate(command, validator::validate, "createAccount");

    // Bug 15 fix: blocked by loadUserPortfolio() - cannot add account to deleted
    // portfolio.
    Portfolio portfolio = loadUserPortfolio(command.portfolioId(), command.userId());
    Account account = portfolio.createAccount(command.accountName(), command.accountType(),
        command.baseCurrency(), command.strategy());

    portfolioRepository.save(portfolio);

    return portfolioViewMapper.toNewAccountView(account);
  }

  public void updateAccount(UpdateAccountCommand command) {
    validate(command, validator::validate, "updateAccount");

    // Bug 15 fix: blocked by loadUserPortfolio() - cannot rename account on deleted
    // portfolio.
    Portfolio portfolio = loadUserPortfolio(command.portfolioId(), command.userId());
    portfolio.renameAccount(command.accountId(), command.accountName());

    portfolioRepository.save(portfolio);
  }

  // always soft deletes
  public void deleteAccount(DeleteAccountCommand command) {
    validate(command, validator::validate, "deleteAccount");
    // Bug 15 fix: blocked by loadUserPortfolio() - cannot close account on deleted
    // portfolio.
    Portfolio portfolio = loadUserPortfolio(command.portfolioId(), command.userId());
    try {
      portfolio.closeAccount(command.accountId());

    } catch (IllegalStateException e) {
      // Bug 4 fix: IllegalStateException from account.close() becomes a typed
      // application exception, not a raw 500.
      throw new AccountCannotBeClosedException("Cannot close account: " + e.getMessage());
    }
    portfolioRepository.save(portfolio);
  }

  private Portfolio loadUserPortfolio(PortfolioId portfolioId, UserId userId) {
    Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
        .orElseThrow(() -> new PortfolioNotFoundException(portfolioId));

    if (portfolio.isDeleted()) {
      throw new PortfolioNotFoundException(portfolioId);
    }
    return portfolio;
  }

  private <T> void validate(T command, Function<T, ValidationResult> validationLogic,
      String methodName) {
    ValidationResult result = validationLogic.apply(command);
    if (!result.isValid()) {
      String msg = String.format("Invalid %s command", methodName);
      throw new InvalidCommandException(msg, result.errors());
    }
  }
}
