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
import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.services.TransactionQueryService;
import com.laderrco.fortunelink.portfolio.application.services.TransactionService;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.*;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

  private final TransactionService transactionService;
  private final TransactionQueryService transactionQueryService;
  private final AuthenticationUserService authService;

  @PostMapping("/buy")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordBuy(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @RequestBody @Valid RecordPurchaseRequest request) {

    List<Fee> fees = mapFees(request.fees(), Currency.of(request.currency()));

    return transactionService.recordPurchase(new RecordPurchaseCommand(
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
  }

  @PostMapping("/sell")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordSell(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @RequestBody @Valid RecordSaleRequest request) {

    List<Fee> fees = mapFees(request.fees(), Currency.of(request.currency()));

    return transactionService.recordSale(new RecordSaleCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        request.symbol(),
        new Quantity(request.quantity()),
        Price.of(request.price(), Currency.of(request.currency())),
        fees,
        request.transactionDate(),
        request.notes() != null ? request.notes() : ""));
  }

  @PostMapping("/deposit")
  @ResponseStatus(HttpStatus.CREATED)
  public TransactionView recordDeposit(
      @PathVariable String portfolioId,
      @PathVariable String accountId,
      @RequestBody @Valid RecordDepositRequest request) {
    UserId userId = resolveUserId();

    return transactionService.recordDeposit(new RecordDepositCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        Money.of(request.amount(), request.currency()),
        request.transactionDate(),
        request.notes() != null ? request.notes() : ""));
  }

  @GetMapping
  public Page<TransactionView> getTransactionHistory(
      @PathVariable String portfolioId,
      @PathVariable String accountId,
      @RequestParam(required = false) String symbol,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    UserId userId = resolveUserId();

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

  @PatchMapping("/{transactionId}/exclude")
  public TransactionView excludeTransaction(
      @PathVariable String portfolioId,
      @PathVariable String accountId,
      @PathVariable String transactionId,
      @RequestBody ExcludeTransactionRequest request) {
    UserId userId = resolveUserId();

    return transactionService.excludeTransaction(new ExcludeTransactionCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        TransactionId.fromString(transactionId),
        request.reason()));
  }

  @PatchMapping("/{transactionId}/restore")
  public TransactionView restoreTransaction(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @PathVariable String transactionId) {

    return transactionService.restoreTransaction(new RestoreTransactionCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        TransactionId.fromString(transactionId)));
  }

  private UserId resolveUserId() {
    return UserId.fromString(authService.getCurrentUser().toString());
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