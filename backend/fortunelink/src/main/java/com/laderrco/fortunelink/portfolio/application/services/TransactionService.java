package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.commands.ExcludeTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.RestoreTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDividendCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDividendReinvestmentCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordInterestCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordReturnOfCaptialCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSplitCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordTransferInCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordTransferOutCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio.application.events.PositionRecalculationRequestedEvent;
import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import com.laderrco.fortunelink.portfolio.application.utils.annotations.IdentifiedTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.utils.annotations.TransactionCommand;
import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.PortfolioContext;
import com.laderrco.fortunelink.portfolio.application.validators.TransactionCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.MarketAssetInfoRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;
import com.laderrco.fortunelink.portfolio.infrastructure.config.cachedidempotency.IdempotencyCache;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ConcurrentModificationException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
TransactionService-> TransactionRecordingService (create transaction) -> PositionTransactionApplier
-> Account/Position
*/
@Service
@Transactional
@RequiredArgsConstructor
@Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
public class TransactionService {
  private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
  private static final String BUY_FEE_CACHE = "fees:buy";

  private final PortfolioRepository portfolioRepository;
  private final AccountHealthService accountHealthService;
  private final TransactionRepository transactionRepository;
  private final MarketAssetInfoRepository infoRepository;
  private final TransactionViewMapper transactionViewMapper;
  private final TransactionCommandValidator validator;
  private final ApplicationEventPublisher eventPublisher;
  private final PortfolioLoader portfolioLoader;
  private final ExchangeRateService exchangeRateService;
  private final TransactionRecordingService transactionRecordingService;
  private final CacheManager cacheManager;
  private final IdempotencyCache idempotencyCache; // Inject the Caffeine bean

  @Recover
  public TransactionView recover(ObjectOptimisticLockingFailureException ex, TransactionCommand cmd) {
    log.warn("Triggering recovery");
    return handleOptimisticLockFailure(ex, cmd.accountId());
  }

  @Recover
  public TransactionView recover(ObjectOptimisticLockingFailureException ex,
      IdentifiedTransactionCommand cmd) {
    // This catches the Exclude/Restore path explicitly
    log.warn("Triggering recovery on exclusion/recover transaction");
    return handleOptimisticLockFailure(ex, cmd.accountId());
  }

  public TransactionView recordPurchase(RecordPurchaseCommand command) {
    return execute(command, validator::validate, "recordPurchase", ctx -> {
      AssetSymbol symbol = new AssetSymbol(command.symbol());
      AssetType resolvedType = resolveAssetType(symbol, command.assetType());
      Price price = resolvePrice(command.price(), ctx.account().getAccountCurrency());
      return transactionRecordingService.recordBuy(ctx.account(), symbol, resolvedType,
          command.quantity(), price, command.fees(), command.notes(), command.transactionDate(),
          command.skipCashCheck());
    });
  }

  public TransactionView recordSale(RecordSaleCommand command) {
    return execute(command, validator::validate, "recordSale", ctx -> {
      AssetSymbol symbol = new AssetSymbol(command.symbol());
      if (!ctx.account().hasPosition(symbol)) {
        throw new InsufficientQuantityException("No position found for: " + command.symbol());
      }
      Price price = resolvePrice(command.price(), ctx.account().getAccountCurrency());
      return transactionRecordingService.recordSell(ctx.account(), symbol, command.quantity(),
          price, command.fees(), command.notes(), command.transactionDate());
    });
  }

  public TransactionView recordDeposit(RecordDepositCommand command) {
    return execute(command, validator::validate, "recordDeposit",
        ctx -> transactionRecordingService.recordDeposit(ctx.account(), command.amount(),
            command.notes(), command.transactionDate()));
  }

  public TransactionView recordWithdrawal(RecordWithdrawalCommand command) {
    return execute(command, validator::validate, "recordWithdrawal",
        ctx -> transactionRecordingService.recordWithdrawal(ctx.account(), command.amount(),
            command.notes(), command.transactionDate()));
  }

  public TransactionView recordFee(RecordFeeCommand command) {
    return execute(command, validator::validate, "recordFee",
        ctx -> transactionRecordingService.recordFee(ctx.account(), command.amount(),
            command.feeType(), command.notes(), command.transactionDate()));
  }

  public TransactionView recordInterest(RecordInterestCommand command) {
    return execute(command, validator::validate, "recordInterest", ctx -> {
      AssetSymbol symbol = command.isAssetInterest() ? new AssetSymbol(command.assetSymbol()) : null;
      return transactionRecordingService.recordInterest(ctx.account(), symbol, command.amount(),
          command.notes(), command.transactionDate());
    });
  }

  public TransactionView recordDividend(RecordDividendCommand command) {
    return execute(command, validator::validate, "recordDividend", ctx -> {
      warnIfDuplicateExists(command.accountId(), TransactionType.DIVIDEND_REINVEST,
          new AssetSymbol(command.assetSymbol()), command.transactionDate());
      return transactionRecordingService.recordDividend(ctx.account(),
          new AssetSymbol(command.assetSymbol()), command.amount(), command.notes(),
          command.transactionDate());
    });
  }

  public TransactionView recordDividendReinvestment(RecordDividendReinvestmentCommand command) {
    return execute(command, validator::validate, "recordDividendReinvestment", ctx -> {
      AssetSymbol symbol = new AssetSymbol(command.assetSymbol());
      warnIfDuplicateExists(command.accountId(), TransactionType.DIVIDEND, symbol,
          command.transactionDate());
      return transactionRecordingService.recordDividendReinvestment(ctx.account(), symbol,
          command.execution().sharesPurchased(), command.execution().pricePerShare(),
          command.notes(), command.transactionDate());
    });
  }

  public TransactionView recordSplit(RecordSplitCommand command) {
    return execute(command, validator::validate, "recordSplit", ctx -> {
      AssetSymbol symbol = new AssetSymbol(command.symbol());
      if (!ctx.account().hasPosition(symbol)) {
        throw new InsufficientQuantityException(
            "Cannot split a non-existent position: " + command.symbol());
      }
      return transactionRecordingService.recordSplit(ctx.account(), symbol, command.ratio(),
          command.notes(), command.transactionDate());
    });
  }

  public TransactionView recordReturnOfCapital(RecordReturnOfCaptialCommand command) {
    return execute(command, validator::validate, "recordReturnOfCapital",
        ctx -> transactionRecordingService.recordReturnOfCapital(ctx.account(),
            new AssetSymbol(command.assetSymbol()), command.heldQuantity(),
            command.distributionPerUnit(), command.notes(), command.transactionDate()));
  }

  public TransactionView recordTransferIn(RecordTransferInCommand command) {
    return execute(command, validator::validate, "recordTransferIn",
        ctx -> transactionRecordingService.recordTransferIn(ctx.account(), command.amount(),
            command.notes(), command.transactionDate()));
  }

  public TransactionView recordTransferOut(RecordTransferOutCommand command) {
    return execute(command, validator::validate, "recordTransferOut",
        ctx -> transactionRecordingService.recordTransferOut(ctx.account(), command.amount(),
            command.notes(), command.transactionDate()));
  }

  public TransactionView excludeTransaction(ExcludeTransactionCommand command) {
    ValidationUtils.validate(command, validator::validate, "excludeTransaction");

    return executeWithIdempotency(command, () -> {
      Transaction existing = loadTransaction(command);

      // This only throws if the transaction was excluded by a DIFFERENT idempotency
      // key
      if (existing.isExcluded()) {
        throw new InvalidTransactionException("Transaction already excluded");
      }

      Transaction excluded = existing.markAsExcluded(command.userId(), command.reason());
      transactionRepository.save(excluded, command.portfolioId(), command.idempotencyKey());

      publishRecalculationIfRequired(existing, command);
      if (existing.transactionType() == TransactionType.BUY) {
        evictBuyFeeCache(command.accountId());
      }

      return transactionViewMapper.toTransactionView(excluded);
    });
  }

  public TransactionView restoreTransaction(RestoreTransactionCommand command) {
    ValidationUtils.validate(command, validator::validate, "restoreTransaction");

    return executeWithIdempotency(command, () -> {
      Transaction existing = loadTransaction(command);

      if (!existing.isExcluded()) {
        throw new InvalidTransactionException("Transaction is not excluded");
      }

      Transaction restored = existing.restore();
      transactionRepository.save(restored, command.portfolioId(), command.idempotencyKey());

      publishRecalculationIfRequired(existing, command);
      if (existing.transactionType() == TransactionType.BUY) {
        evictBuyFeeCache(command.accountId());
      }

      return transactionViewMapper.toTransactionView(restored);
    });
  }

  // -------------------------------------------------------------------------
  // Private infrastructure
  // -------------------------------------------------------------------------

  private <C extends TransactionCommand> TransactionView execute(C command,
      Function<C, ValidationResult> validationFn, String operationName,
      Function<PortfolioContext, Transaction> recordFn) {
    ValidationUtils.validate(command, validationFn, operationName);

    return executeWithIdempotency(command, () -> {
      PortfolioContext ctx = getPortfolioContext(command);
      Transaction tx = recordFn.apply(ctx);
      persistChanges(ctx, tx, command.idempotencyKey());
      return transactionViewMapper.toTransactionView(tx);
    });
  }

  private PortfolioContext getPortfolioContext(TransactionCommand command) {
    Portfolio portfolio = portfolioLoader.loadUserPortfolio(command.portfolioId(),
        command.userId());
    Account account = portfolio.getAccount(command.accountId());
    return new PortfolioContext(portfolio, account);
  }

  /**
   * Persists both the portfolio aggregate and the new transaction. The
   * portfolioId is taken
   * directly from the in-memory context — no DB lookup.
   */
  private void persistChanges(PortfolioContext ctx, Transaction tx, UUID idempotencyKey) {
    portfolioRepository.save(ctx.portfolio());
    // Pass portfolioId from context, eliminates the findPortfolioIdByAccountId
    // secondary query that previously fired on every single transaction insert.
    transactionRepository.save(tx, ctx.portfolio().getPortfolioId(), idempotencyKey);

    // Fee totals change only on BUY transactions that carry fees.
    // Evict so the next portfolio read reflects the updated ACB.
    if (tx.transactionType() == TransactionType.BUY && !tx.fees().isEmpty()) {
      evictBuyFeeCache(tx.accountId());
    }
  }

  private Transaction loadTransaction(IdentifiedTransactionCommand command) {
    return transactionRepository.findByIdAndPortfolioIdAndUserIdAndAccountId(
        command.transactionId(), command.portfolioId(), command.userId(), command.accountId())
        .orElseThrow(() -> new TransactionNotFoundException(command.transactionId()));
  }

  private void publishRecalculationIfRequired(Transaction tx, TransactionCommand command) {
    if (tx.transactionType().affectsHoldings() && tx.execution() != null) {
      eventPublisher.publishEvent(
          new PositionRecalculationRequestedEvent(command.portfolioId(), command.userId(),
              command.accountId(), tx.execution().asset()));
    }
  }

  private Price resolvePrice(Price commandPrice, Currency accountCurrency) {
    if (commandPrice.currency().equals(accountCurrency)) {
      return commandPrice;
    }
    return new Price(exchangeRateService.convert(commandPrice.pricePerUnit(), accountCurrency));
  }

  /**
   * Resolution order: 1. DB/cache, authoritative for known symbols 2. Client
   * hint, trusted only
   * when structurally valid (not CASH, not null) 3. STOCK, safe fallback of last
   * resort
   * <p>
   * A client claiming AAPL is CRYPTO will be corrected once the symbol is seeded
   * into
   * market_asset_info. Until then, their hint is used.
   */
  private AssetType resolveAssetType(AssetSymbol symbol, AssetType clientHint) {
    return infoRepository.findBySymbol(symbol).map(MarketAssetInfo::type)
        .orElseGet(() -> sanitizeAssetTypeHint(clientHint));
  }

  private AssetType sanitizeAssetTypeHint(AssetType hint) {
    if (hint == null || hint == AssetType.CASH || hint == AssetType.OTHER) {
      return AssetType.STOCK;
    }
    return hint;
  }

  private void evictBuyFeeCache(AccountId accountId) {
    Cache cache = cacheManager.getCache(BUY_FEE_CACHE);
    if (cache != null) {
      cache.evict(accountId.id().toString());
    }
  }

  /**
   * Warns (not throws) if a DIVIDEND transaction exists for the same symbol
   * within 24 hours of this
   * DRIP event.
   * <p>
   * DRIP and DIVIDEND are mutually exclusive for the same event: -
   * DIVIDEND_REINVEST = broker
   * automatically reinvests, no cash lands - DIVIDEND = cash lands in account,
   * user reinvests
   * manually (records as separate BUY)
   * <p>
   * Recording both for the same event will overstate cash balance. This check is
   * a runtime warning
   * only — enforcement is the caller's responsibility. Callers that intentionally
   * bypass this
   * (e.g., CSV import correction flows) should be aware of the accounting
   * implication.
   */
  private void warnIfDuplicateExists(AccountId accountId, TransactionType transactionType,
      AssetSymbol assetSymbol, Instant transactionDate) {

    Instant windowStart = transactionDate.minus(24, ChronoUnit.HOURS);
    Instant windowEnd = transactionDate.plus(24, ChronoUnit.HOURS);

    boolean hasConflict = transactionRepository.existsConflict(accountId, transactionType,
        assetSymbol, windowStart, windowEnd);

    if (hasConflict) {
      log.warn("DRIP recorded for symbol={} on {} but a DIVIDEND transaction exists "
          + "within 24 hours for the same symbol in accountId={}. "
          + "If this is the same event, the DIVIDEND transaction will overstate "
          + "cash balance. Review transaction history before proceeding.", assetSymbol,
          transactionDate, accountId);
    }
  }

  private TransactionView executeWithIdempotency(TransactionCommand command,
      Supplier<TransactionView> businessLogic) {
    UUID key = command.idempotencyKey();

    // 1. Level 1 & 2: Check Cache and DB
    if (key != null) {
      TransactionView cached = idempotencyCache.get(key.toString());
      if (cached != null) {
        return cached;
      }

      Optional<Transaction> existing = transactionRepository.findByIdempotencyKeyAndPortfolioId(key,
          command.portfolioId());
      if (existing.isPresent()) {
        log.info("Duplicate transaction detected for key {} in portfolio {}", key, command.portfolioId());
        TransactionView view = transactionViewMapper.toTransactionView(existing.get());
        idempotencyCache.put(key.toString(), view);
        return view;
      }
    }

    TransactionView result = businessLogic.get();

    if (key != null) {
      idempotencyCache.put(key.toString(), result);
    }

    return result;
  }

  private TransactionView handleOptimisticLockFailure(Exception ex, AccountId accountId) {
    log.error("Optimistic lock exhausted. Marking account {} as stale.", accountId, ex);
    accountHealthService.markStale(accountId);
    throw new ConcurrentModificationException("Portfolio was modified concurrently.", ex);
  }
}