package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.ExcludeTransactionRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.FeeRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordDRIPRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordDepositRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordDividendRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordInterestRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordPurchaseRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordReturnOfCapitalRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordSaleRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordSplitRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordStandaloneFeeRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordTransferInRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordTransferOutRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.transactions.RecordWithdrawalRequest;
import com.laderrco.fortunelink.portfolio.application.commands.ExcludeTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.RestoreTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDividendCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDividendReinvestmentCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordInterestCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordReturnOfCapitalCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSplitCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordTransferInCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordTransferOutCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionByIdQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio.application.services.TransactionQueryService;
import com.laderrco.fortunelink.portfolio.application.services.TransactionService;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee.FeeMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Full transaction lifecycle controller.
 * <p>
 * All endpoints follow the same authentication pattern: @AuthenticatedUser UserId userId. All
 * mutations return the created/modified TransactionView. All paths:
 * /api/v1/portfolios/{portfolioId}/accounts/{accountId}/transactions/...
 * <p>
 * Transaction type → endpoint mapping: BUY → POST /buy SELL → POST /sell DEPOSIT → POST /deposit
 * WITHDRAWAL → POST /withdrawal FEE → POST /fee INTEREST → POST /interest DIVIDEND → POST /dividend
 * DIVIDEND_REINVEST → POST /drip SPLIT → POST /split RETURN_OF_CAPITAL → POST /return-of-capital
 * TRANSFER_IN → POST /transfer-in TRANSFER_OUT → POST /transfer-out (exclusion) → PATCH
 * /{id}/exclude (restore) → PATCH /{id}/restore
 * <p>
 * Reads: GET / → paginated history with optional filters GET /{id} → single transaction by ID
 */
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

  private final TransactionService transactionService;
  private final TransactionQueryService transactionQueryService;

  // =========================================================================
  // Trade transactions (affect positions AND cash)
  // =========================================================================

  @PostMapping("/buy")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordBuy(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordPurchaseRequest request) {

    Instant validatedDate = resolveTransactionDate(request.transactionDate());
    List<Fee> fees = mapFees(request.fees(), validatedDate);

    return transactionService.recordPurchase(
        new RecordPurchaseCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId), request.symbol(), request.type(),
            new Quantity(request.quantity()),
            Price.of(request.price(), Currency.of(request.currency())), fees, validatedDate,
            emptyIfNull(request.notes()), false));
  }

  @PostMapping("/sell")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordSell(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordSaleRequest request) {

    Instant validatedDate = resolveTransactionDate(request.transactionDate());
    List<Fee> fees = mapFees(request.fees(), validatedDate);

    return transactionService.recordSale(
        new RecordSaleCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId), request.symbol(),
            new Quantity(request.quantity()),
            Price.of(request.price(), Currency.of(request.currency())), fees, validatedDate,
            emptyIfNull(request.notes())));
  }

  @PostMapping("/split")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordSplit(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordSplitRequest request) {

    return transactionService.recordSplit(
        new RecordSplitCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId), request.symbol(),
            new Ratio(request.numerator(), request.denominator()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  @PostMapping("/return-of-capital")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordReturnOfCapital(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordReturnOfCapitalRequest request) {

    return transactionService.recordReturnOfCapital(
        new RecordReturnOfCapitalCommand(validateUuid(idempotencyKey),
            PortfolioId.fromString(portfolioId), userId, AccountId.fromString(accountId),
            request.assetSymbol(),
            Price.of(request.distributionPerUnit(), Currency.of(request.currency())),
            new Quantity(request.heldQuantity()), resolveTransactionDate(request.transactionDate()),
            emptyIfNull(request.notes())));
  }

  // =========================================================================
  // Cash-only transactions (affect cash balance, no position change)
  // =========================================================================

  @PostMapping("/deposit")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordDeposit(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordDepositRequest request) {

    return transactionService.recordDeposit(
        new RecordDepositCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId), Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  @PostMapping("/withdrawal")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordWithdrawal(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordWithdrawalRequest request) {

    return transactionService.recordWithdrawal(
        new RecordWithdrawalCommand(validateUuid(idempotencyKey),
            PortfolioId.fromString(portfolioId), userId, AccountId.fromString(accountId),
            Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  @PostMapping("/fee")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordFee(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordStandaloneFeeRequest request) {

    return transactionService.recordFee(
        new RecordFeeCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId), Money.of(request.amount(), request.currency()),
            request.feeType(), resolveTransactionDate(request.transactionDate()),
            emptyIfNull(request.notes())));
  }

  @PostMapping("/transfer-in")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordTransferIn(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordTransferInRequest request) {

    return transactionService.recordTransferIn(
        new RecordTransferInCommand(validateUuid(idempotencyKey),
            PortfolioId.fromString(portfolioId), userId, AccountId.fromString(accountId),
            Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  @PostMapping("/transfer-out")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordTransferOut(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordTransferOutRequest request) {

    return transactionService.recordTransferOut(
        new RecordTransferOutCommand(validateUuid(idempotencyKey),
            PortfolioId.fromString(portfolioId), userId, AccountId.fromString(accountId),
            Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  // =========================================================================
  // Income transactions (cash in, associated with a holding)
  // =========================================================================

  @PostMapping("/interest")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordInterest(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordInterestRequest request) {

    // RecordInterestCommand.cashInterest() / .assetInterest() are the factories
    // but the command accepts null assetSymbol for cash-level interest
    return transactionService.recordInterest(
        new RecordInterestCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId),
            request.isAssetInterest() ? request.assetSymbol() : null,
            Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  @PostMapping("/dividend")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordDividend(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordDividendRequest request) {

    return transactionService.recordDividend(
        new RecordDividendCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId), request.assetSymbol(),
            Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  @PostMapping("/drip")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordDividendReinvestment(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordDRIPRequest request) {

    return transactionService.recordDividendReinvestment(
        new RecordDividendReinvestmentCommand(validateUuid(idempotencyKey),
            PortfolioId.fromString(portfolioId), userId, AccountId.fromString(accountId),
            request.assetSymbol(), new RecordDividendReinvestmentCommand.DripExecution(
            new Quantity(request.sharesPurchased()),
            Price.of(request.pricePerShare(), Currency.of(request.currency()))),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  // =========================================================================
  // Read operations
  // =========================================================================

  /**
   * Returns a paginated list of transactions for an account.
   * <p>
   * Supports optional filtering by symbol and date range. Results are sorted by occurredAt
   * descending (most recent first). Page size is capped at 100 per request.
   * <p>
   * All filters are optional and can be combined: ?symbol=AAPL → AAPL transactions only
   * ?startDate=...&endDate=... → date range ?symbol=AAPL&startDate=... → AAPL in range
   */
  @GetMapping
  public Page<TransactionView> getTransactionHistory(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestParam(required = false) String symbol,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

    return transactionQueryService.getTransactionHistory(
        new GetTransactionHistoryQuery(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId), symbol != null ? new AssetSymbol(symbol.trim()) : null,
            startDate, endDate, page, size));
  }

  /**
   * Returns a single transaction by ID.
   * <p>
   * The transaction must belong to the specified account and portfolio. Returns 404 if the
   * transaction does not exist or belongs to a different user's account , we do not differentiate
   * to avoid information leakage.
   */
  @GetMapping("/{transactionId}")
  public TransactionView getTransaction(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @PathVariable String transactionId) {

    return transactionQueryService.getTransaction(
        new GetTransactionByIdQuery(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId), TransactionId.fromString(transactionId)));
  }

  // =========================================================================
  // Exclusion lifecycle (mutations on existing transactions)
  // =========================================================================

  /**
   * Excludes a transaction from position and capital gains calculations.
   * <p>
   * Cash balance is NOT reversed , see ExcludeTransactionCommand for rationale. Triggers an async
   * position recalculation for the affected symbol.
   */
  @PatchMapping("/{transactionId}/exclude")
  public TransactionView excludeTransaction(@RequestHeader("Idempotency-Key") String idempotencyKey,
      @PathVariable String portfolioId, @AuthenticatedUser UserId userId,
      @PathVariable String accountId, @PathVariable String transactionId,
      @RequestBody @Valid ExcludeTransactionRequest request) {

    return transactionService.excludeTransaction(
        new ExcludeTransactionCommand(validateUuid(idempotencyKey),
            PortfolioId.fromString(portfolioId), userId, AccountId.fromString(accountId),
            TransactionId.fromString(transactionId), request.reason()));
  }

  /**
   * Restores a previously excluded transaction back into calculations. Triggers an async position
   * recalculation for the affected symbol.
   */
  @PatchMapping("/{transactionId}/restore")
  public TransactionView restoreTransaction(@RequestHeader("Idempotency-Key") String idempotencyKey,
      @PathVariable String portfolioId, @AuthenticatedUser UserId userId,
      @PathVariable String accountId, @PathVariable String transactionId) {

    return transactionService.restoreTransaction(
        new RestoreTransactionCommand(validateUuid(idempotencyKey),
            PortfolioId.fromString(portfolioId), userId, AccountId.fromString(accountId),
            TransactionId.fromString(transactionId)));
  }

  // =========================================================================
  // Private helpers
  // =========================================================================

  private List<Fee> mapFees(List<FeeRequest> feeRequests, Instant txDate) {
    if (feeRequests == null || feeRequests.isEmpty()) {
      return List.of();
    }

    return feeRequests.stream().map(
        f -> Fee.of(f.feeType(), Money.of(f.amount(), f.currency()), txDate,
            new FeeMetadata(Map.of()))).toList();
  }

  private UUID validateUuid(String idempotencyKey) {
    return idempotencyKey != null ? UUID.fromString(idempotencyKey) : UUID.randomUUID();
  }

  private String emptyIfNull(String value) {
    return value != null ? value : "";
  }

  private Instant resolveTransactionDate(Instant requested) {
    return requested != null ? requested : Instant.now();
  }
}