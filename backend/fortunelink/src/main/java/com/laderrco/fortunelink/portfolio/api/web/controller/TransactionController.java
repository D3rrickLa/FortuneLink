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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * All endpoints follow the same authentication pattern: @AuthenticatedUser UserId. All
 * mutations return the created/modified TransactionView.
 * <p>
 * This controller manages the ledger for a specific investment account,handling everything from
 * trade execution (BUY/SELL) to corporate actions (SPLITS) and cash management
 * (DEPOSIT/WITHDRAWAL).
 */
@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/transactions")
@RequiredArgsConstructor
@Validated
@Tag(name = "Transactions", description = "Full lifecycle management for trades, cash movements, and income events.")
public class TransactionController {

  private final TransactionService transactionService;
  private final TransactionQueryService transactionQueryService;

  // =========================================================================
  // Trade transactions (affect positions AND cash)
  // =========================================================================

  /**
   * Records a new asset purchase.
   * <p>
   * Decreases the account's cash balance and increases the position quantity for the given symbol.
   */
  @PostMapping("/buy")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a BUY", description = "Decreases cash and increases asset quantity.")
  public TransactionView recordBuy(@PathVariable String portfolioId, @PathVariable String accountId,
      @Parameter(hidden = true) @AuthenticatedUser UserId userId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
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

  /**
   * Records an asset sale.
   * <p>
   * Increases the account's cash balance and decreases the position quantity. This action triggers
   * realized gain/loss calculations.
   */
  @PostMapping("/sell")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a SELL", description = "Increases cash and decreases asset quantity. Triggers realized gain/loss calculation.")
  public TransactionView recordSell(@PathVariable String portfolioId,
      @PathVariable String accountId, @Parameter(hidden = true) @AuthenticatedUser UserId userId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
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

  /**
   * Records a stock split or reverse split.
   * <p>
   * Adjusts the quantity and cost basis of a position based on the provided ratio (e.g., 2:1 for a
   * split, 1:10 for a reverse split). No cash impact.
   */
  @PostMapping("/split")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a SPLIT", description = "Adjusts asset quantity and basis via a ratio. No cash impact.")
  public TransactionView recordSplit(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordSplitRequest request) {

    return transactionService.recordSplit(
        new RecordSplitCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId), request.symbol(),
            new Ratio(request.numerator(), request.denominator()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  /**
   * Records a return of capital distribution.
   * <p>
   * Reduces the cost basis of the position. If the basis reaches zero, subsequent distributions are
   * treated as capital gains.
   */
  @PostMapping("/return-of-capital")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record Return of Capital", description = "Reduces asset cost basis. Triggers cash increase.")
  public TransactionView recordReturnOfCapital(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
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

  /**
   * Records a manual cash deposit into the account.
   */
  @PostMapping("/deposit")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a Deposit", description = "Increases account cash balance.")
  public TransactionView recordDeposit(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordDepositRequest request) {

    return transactionService.recordDeposit(
        new RecordDepositCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId), Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  /**
   * Records a manual cash withdrawal from the account.
   */
  @PostMapping("/withdrawal")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a Withdrawal", description = "Decreases account cash balance.")
  public TransactionView recordWithdrawal(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordWithdrawalRequest request) {

    return transactionService.recordWithdrawal(
        new RecordWithdrawalCommand(validateUuid(idempotencyKey),
            PortfolioId.fromString(portfolioId), userId, AccountId.fromString(accountId),
            Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  /**
   * Records a standalone fee (e.g., account maintenance, wire fee).
   */
  @PostMapping("/fee")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a Fee", description = "Records an expense/fee against the cash balance.")
  public TransactionView recordFee(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordStandaloneFeeRequest request) {

    return transactionService.recordFee(
        new RecordFeeCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId), Money.of(request.amount(), request.currency()),
            request.feeType(), resolveTransactionDate(request.transactionDate()),
            emptyIfNull(request.notes())));
  }

  /**
   * Records a cash transfer into the account from an external source.
   */
  @PostMapping("/transfer-in")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record Transfer In", description = "Records cash moving into the account.")
  public TransactionView recordTransferIn(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordTransferInRequest request) {

    return transactionService.recordTransferIn(
        new RecordTransferInCommand(validateUuid(idempotencyKey),
            PortfolioId.fromString(portfolioId), userId, AccountId.fromString(accountId),
            Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  /**
   * Records a cash transfer out of the account to an external destination.
   */
  @PostMapping("/transfer-out")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record Transfer Out", description = "Records cash moving out of the account.")
  public TransactionView recordTransferOut(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
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

  /**
   * Records interest income.
   * <p>
   * Can be associated with a specific asset or the cash balance itself.
   */
  @PostMapping("/interest")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record Interest", description = "Records interest earned on cash or a specific asset.")
  public TransactionView recordInterest(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordInterestRequest request) {

    return transactionService.recordInterest(
        new RecordInterestCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId),
            request.isAssetInterest() ? request.assetSymbol() : null,
            Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  /**
   * Records a cash dividend payment for a specific asset holding.
   */
  @PostMapping("/dividend")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a Dividend", description = "Records cash income from an asset dividend.")
  public TransactionView recordDividend(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordDividendRequest request) {

    return transactionService.recordDividend(
        new RecordDividendCommand(validateUuid(idempotencyKey), PortfolioId.fromString(portfolioId),
            userId, AccountId.fromString(accountId), request.assetSymbol(),
            Money.of(request.amount(), request.currency()),
            resolveTransactionDate(request.transactionDate()), emptyIfNull(request.notes())));
  }

  /**
   * Records a Dividend Reinvestment (DRIP).
   * <p>
   * This is a composite action that records a dividend and uses the proceeds to purchase additional
   * shares of the same asset.
   */
  @PostMapping("/drip")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record a DRIP", description = "Dividend reinvestment; creates an income and buy event.")
  public TransactionView recordDividendReinvestment(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Optional UUID for safe retries") @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
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
   */
  @GetMapping
  @Operation(summary = "Get transaction history", description = "Paginated list of transactions with optional filtering.")
  public Page<TransactionView> getTransactionHistory(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @Parameter(description = "Filter by ticker symbol") @RequestParam(required = false) String symbol,
      @Parameter(description = "Filter by start date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
      @Parameter(description = "Filter by end date") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
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
   * transaction does not exist or belongs to a different user's account.
   */
  @GetMapping("/{transactionId}")
  @Operation(summary = "Get single transaction", description = "Retrieves a transaction by its unique ID.")
  @ApiResponses({@ApiResponse(responseCode = "200", description = "Transaction found"),
      @ApiResponse(responseCode = "404", description = "Transaction not found or access denied")})
  public TransactionView getTransaction(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @PathVariable String transactionId) {

    return transactionQueryService.getTransaction(
        new GetTransactionByIdQuery(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId), TransactionId.fromString(transactionId)));
  }

  // =========================================================================
  // Exclusion lifecycle (mutations on existing transactions)

  /**
   * Excludes a transaction from position and capital gains calculations.
   * <p>
   * Cash balance is NOT reversed. Triggers an async position recalculation for the affected
   * symbol.
   */
  @PatchMapping("/{transactionId}/exclude")
  @Operation(summary = "Exclude transaction", description = "Soft-removes a transaction from P&L and position calculations.")
  public TransactionView excludeTransaction(
      @Parameter(description = "UUID for safe retries") @RequestHeader("Idempotency-Key") String idempotencyKey,
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
  @Operation(summary = "Restore transaction", description = "Re-activates a previously excluded transaction.")
  public TransactionView restoreTransaction(
      @Parameter(description = "UUID for safe retries") @RequestHeader("Idempotency-Key") String idempotencyKey,
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