package com.laderrco.fortunelink.portfolio.api.web.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.*;
import com.laderrco.fortunelink.portfolio.application.commands.ExcludeTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.RestoreTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio.application.services.TransactionQueryService;
import com.laderrco.fortunelink.portfolio.application.services.TransactionService;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.*;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import com.laderrco.fortunelink.portfolio.infrastructure.config.cachedidempotency.IdempotencyCache;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/transactions")
public class TransactionController {
  private final TransactionService transactionService;
  private final TransactionQueryService transactionQueryService;
  private final IdempotencyCache idempotencyCache;

  @PostMapping("/buy")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordBuy(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordPurchaseRequest request) {

    String cacheKey = (idempotencyKey != null) ? "idemp:" + userId + ":" + idempotencyKey : null;
    if (cacheKey != null) {
      TransactionView cached = idempotencyCache.get(cacheKey);
      if (cached != null)
        return cached;
    }

    List<Fee> fees = mapFees(request.fees(), Currency.of(request.currency()));

    TransactionView result = transactionService.recordPurchase(new RecordPurchaseCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        request.symbol(),
        request.type(),
        new Quantity(request.quantity()),
        Price.of(request.price(), Currency.of(request.currency())),
        fees,
        request.transactionDate(),
        request.notes() != null ? request.notes() : "",
        false));

    if (cacheKey != null) {
      idempotencyCache.put(cacheKey, result);
    }

    return result;
  }

  @PostMapping("/sell")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordSell(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordSaleRequest request) {

    String cacheKey = (idempotencyKey != null) ? "idemp:" + userId + ":" + idempotencyKey : null;
    if (cacheKey != null) {
      TransactionView cached = idempotencyCache.get(cacheKey);
      if (cached != null)
        return cached;
    }

    List<Fee> fees = mapFees(request.fees(), Currency.of(request.currency()));

    TransactionView result = transactionService.recordSale(new RecordSaleCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        request.symbol(),
        new Quantity(request.quantity()),
        Price.of(request.price(), Currency.of(request.currency())),
        fees,
        request.transactionDate(),
        request.notes() != null ? request.notes() : ""));

    if (cacheKey != null)
      idempotencyCache.put(cacheKey, result);
    return result;
  }

  @PostMapping("/deposit")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordDeposit(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid RecordDepositRequest request) {

    String cacheKey = (idempotencyKey != null) ? "idemp:" + userId + ":" + idempotencyKey : null;

    if (cacheKey != null) {
      TransactionView cached = idempotencyCache.get(cacheKey);
      if (cached != null)
        return cached;
    }

    TransactionView result = transactionService.recordDeposit(new RecordDepositCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        Money.of(request.amount(), request.currency()),
        request.transactionDate(),
        request.notes() != null ? request.notes() : ""));

    if (cacheKey != null)
      idempotencyCache.put(cacheKey, result);
    return result;
  }

  @PatchMapping("/{transactionId}/exclude")
  public TransactionView excludeTransaction(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @PathVariable String transactionId,
      @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
      @RequestBody @Valid ExcludeTransactionRequest request) {
    String cacheKey = (idempotencyKey != null) ? "idemp:" + userId + ":" + idempotencyKey : null;

    if (cacheKey != null) {
      TransactionView cached = idempotencyCache.get(cacheKey);
      if (cached != null)
        return cached;
    }

    TransactionView result = transactionService.excludeTransaction(new ExcludeTransactionCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        TransactionId.fromString(transactionId),
        request.reason()));

    if (cacheKey != null)
      idempotencyCache.put(cacheKey, result);
    return result;
  }

  @PatchMapping("/{transactionId}/restore")
  public TransactionView restoreTransaction(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @PathVariable String transactionId,
      @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
    String cacheKey = (idempotencyKey != null) ? "idemp:" + userId + ":" + idempotencyKey : null;

    if (cacheKey != null) {
      TransactionView cached = idempotencyCache.get(cacheKey);
      if (cached != null)
        return cached;
    }

    TransactionView result = transactionService.restoreTransaction(new RestoreTransactionCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        TransactionId.fromString(transactionId)));

    if (cacheKey != null)
      idempotencyCache.put(idempotencyKey, result);
    return result;
  }

  @GetMapping
  public Page<TransactionView> getTransactionHistory(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @RequestParam(required = false) String symbol,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    return transactionQueryService.getTransactionHistory(new GetTransactionHistoryQuery(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        symbol != null ? new AssetSymbol(symbol) : null,
        startDate,
        endDate,
        page,
        size));
  }

  private List<Fee> mapFees(List<FeeRequest> feeRequests, Currency defaultCurrency) {
    if (feeRequests == null || feeRequests.isEmpty())
      return List.of();
    return feeRequests.stream()
        .map(f -> Fee.of(f.feeType(),
            Money.of(f.amount(), f.currency()),
            Instant.now()))
        .toList();
  }
}