package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.commands.*;
import com.laderrco.fortunelink.portfolio.application.exceptions.*;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioAccessUtils;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.UpdatePortfolioResult;
import com.laderrco.fortunelink.portfolio.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioAlreadyDeletedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;

import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioLifecycleService {
  private static final Logger log = LoggerFactory.getLogger(PortfolioLifecycleService.class);
  private static final int MAX_PORTFOLIOS_PER_USER = 1;
  private static final String DEFAULT_NAME = "Default Account";

  private final PortfolioRepository portfolioRepository;
  private final TransactionRepository transactionRepository;
  private final PortfolioViewMapper portfolioViewMapper;

  private final MarketDataService marketDataService;
  private final PortfolioValuationService portfolioValuationService;

  private final PortfolioLifecycleCommandValidator validator;
  private final TransactionTemplate transactionTemplate;
  private final AccountViewBuilder accountViewBuilder;
  private final PortfolioLoader portfolioLoader;

  public PortfolioView createPortfolio(CreatePortfolioCommand command) {
    validate(command, validator::validate, "createPortfolio");

    // Optimistic check: reduces DB load but relies on DB constraint for absolute
    // safety
    long currentCount = portfolioRepository.countByUserId(command.userId());
    if (currentCount >= MAX_PORTFOLIOS_PER_USER) {
      throw new PortfolioLimitReachedException("Already reached max allowed portfolio limit");
    }

    Portfolio portfolio = Portfolio.createNew(command.userId(), command.name(),
        command.description(), command.currency());

    if (command.createDefaultAccount()) {
      portfolio.createAccount(DEFAULT_NAME, command.defaultAccountType(),
          command.currency(), command.defaultStrategy());
    }

    try {
      Portfolio savedPortfolio = portfolioRepository.save(portfolio);
      return portfolioViewMapper.toNewPortfolioView(savedPortfolio);
    } catch (DataIntegrityViolationException e) {
      // Handle race condition if DB unique constraint is hit
      throw new PortfolioLimitReachedException("Portfolio already exists for this user");
    }
  }

  /**
   * NOT_SUPPORTED ensures the class-level @Transactional is suspended.
   * This allows the TransactionTemplate to commit and release the connection
   * immediately after the write (P1), before we hit potentially slow external
   * services.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public PortfolioView updatePortfolio(UpdatePortfolioCommand command) {
    validate(command, validator::validate, "updatePortfolio");

    // refactored like this as saved.getAccounts().stream() from a
    // PortfolioEntity.java
    // will throw LazyInitializationException as the session is gone.
    var writeResult = transactionTemplate.execute(status -> {
      Portfolio existing = portfolioLoader.loadUserPortfolio(command.portfolioId(), command.userId());

      existing.updateDetails(command.name(), command.description());
      existing.updateDisplayCurrency(command.currency());

      Portfolio saved = portfolioRepository.save(existing);

      // This prevents LazyInitializationException in extractSymbols and
      // buildAccountViews
      List<AccountId> accountIds = saved.getAccounts().stream()
          .peek(acc -> acc.getPositions().size()) // Trigger lazy load for positions
          .map(Account::getAccountId)
          .toList();

      // could make this a map to reduce overhead?
      return new UpdatePortfolioResult(saved, accountIds);
    });

    Portfolio saved = writeResult.portfolio();
    List<AccountId> accountIds = writeResult.accountIds();

    // P2: Fee Enrichment
    Map<AccountId, Map<AssetSymbol, Money>> feeCache = Map.of();
    if (!accountIds.isEmpty()) {
      try {
        feeCache = transactionRepository.sumBuyFeesByAccountAndSymbol(accountIds);
      } catch (Exception e) {
        log.error("Failed to fetch fee data for portfolio {}.", saved.getPortfolioId(), e);
      }
    }

    // P3: External Market Data (No connection held here)
    Set<AssetSymbol> symbols = PortfolioAccessUtils.extractSymbols(saved);
    Map<AssetSymbol, MarketAssetQuote> quoteCache;

    Money totalValue;
    boolean isStale = false;

    try {
      quoteCache = marketDataService.getBatchQuotes(symbols);
      totalValue = portfolioValuationService.calculateTotalValue(
          saved, saved.getDisplayCurrency(), quoteCache);

    } catch (Exception e) {

      log.warn("Market data unavailable for portfolio {}. Returning stale view.",
          saved.getPortfolioId());

      quoteCache = Map.of();
      totalValue = Money.zero(saved.getDisplayCurrency());
      isStale = true;
    }

    // P4: View Mapping
    List<AccountView> accountViews = buildAccountViews(saved.getAccounts(), quoteCache, feeCache);
    return portfolioViewMapper.toPortfolioView(saved, accountViews, totalValue, isStale);

  }

  public void reopenAccount(ReopenAccountCommand command) {
    validate(command, validator::validate, "reopenAccount");

    transactionTemplate.executeWithoutResult(status -> {
      Portfolio portfolio = portfolioLoader.loadUserPortfolio(
          command.portfolioId(), command.userId());
      try {
        portfolio.reopenAccount(command.accountId());
        portfolioRepository.save(portfolio);
      } catch (IllegalStateException e) {
        status.setRollbackOnly();
        throw new AccountCannotBeReopenedException("Cannot reopen account: " + e.getMessage());
      }
    });
  }

  public void deletePortfolio(DeletePortfolioCommand command) {
    validate(command, validator::validate, "deletePortfolio");

    // NOTE: To allow hard-deleting a soft-deleted portfolio, we bypass the loader
    // and use the repository directly.
    Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(),
        command.userId()).orElseThrow(
            () -> new PortfolioNotFoundException("Portfolio not found for ID: " + command.portfolioId()));

    if (command.softDelete()) {
      try {
        portfolio.markAsDeleted(command.userId());
        portfolioRepository.save(portfolio);
      } catch (PortfolioAlreadyDeletedException | PortfolioNotEmptyException e) {
        throw new PortfolioDeletionException(e.getMessage());
      }
    } else {
      portfolioRepository.delete(command.portfolioId());
    }
  }

  public AccountView createAccount(CreateAccountCommand command) {
    validate(command, validator::validate, "createAccount");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(command.portfolioId(), command.userId());
    Account account = portfolio.createAccount(command.accountName(), command.accountType(),
        command.baseCurrency(), command.strategy());

    portfolioRepository.save(portfolio);

    return portfolioViewMapper.toNewAccountView(account);
  }

  public void updateAccount(UpdateAccountCommand command) {
    validate(command, validator::validate, "updateAccount");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(command.portfolioId(),
        command.userId());
    portfolio.renameAccount(command.accountId(), command.accountName());

    portfolioRepository.save(portfolio);
  }

  // always soft deletes
  public void deleteAccount(DeleteAccountCommand command) {
    validate(command, validator::validate, "deleteAccount");

    Portfolio portfolio = portfolioLoader.loadUserPortfolio(command.portfolioId(), command.userId());

    try {
      portfolio.closeAccount(command.accountId());

    } catch (IllegalStateException e) {
      throw new AccountCannotBeClosedException("Cannot close account: " + e.getMessage());
    }
    portfolioRepository.save(portfolio);
  }

  private <T> void validate(T command, Function<T, ValidationResult> validationLogic,
      String methodName) {
    ValidationResult result = validationLogic.apply(command);
    if (!result.isValid()) {
      String msg = String.format("Invalid %s command", methodName);
      throw new InvalidCommandException(msg, result.errors());
    }
  }

  private List<AccountView> buildAccountViews(
      Collection<Account> accounts,
      Map<AssetSymbol, MarketAssetQuote> quotes,
      Map<AccountId, Map<AssetSymbol, Money>> fees) {

    return accounts.stream()
        .map(account -> accountViewBuilder.build(
            account,
            quotes,
            fees.getOrDefault(account.getAccountId(), Map.of())))
        .toList();
  }
}
