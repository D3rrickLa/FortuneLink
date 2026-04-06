package com.laderrco.fortunelink.portfolio.api.web.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.CreateAccountRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.UpdateAccountRequest;
import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.queries.GetAllAccountsQuery;
import com.laderrco.fortunelink.portfolio.application.services.AccountLifecycleService;
import com.laderrco.fortunelink.portfolio.application.services.AccountQueryService;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts")
public class AccountController {

  private final AccountLifecycleService lifecycleService;
  private final AccountQueryService accountQueryService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AccountView createAccount(
      @PathVariable String portfolioId, @AuthenticatedUser UserId userId,
      @RequestBody @Valid CreateAccountRequest request) {

    return lifecycleService.createAccount(new CreateAccountCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        request.accountName(),
        request.accountType(),
        request.strategy(),
        Currency.of(request.currency())));
  }

  @GetMapping
  public List<AccountView> getAllAccounts(@PathVariable String portfolioId, @AuthenticatedUser UserId userId) {
    return accountQueryService.getAllAccounts(new GetAllAccountsQuery(
        PortfolioId.fromString(portfolioId), userId));
  }

  @GetMapping("/{accountId}")
  public AccountView getAccount(
      @PathVariable String portfolioId, @AuthenticatedUser UserId userId, @PathVariable String accountId) {
    return accountQueryService.getAccountSummary(new GetAccountSummaryQuery(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId)));
  }

  @PutMapping("/{accountId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateAccount(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @RequestBody @Valid UpdateAccountRequest request) {
    lifecycleService.updateAccount(new UpdateAccountCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId),
        request.accountName()));
  }

  @DeleteMapping("/{accountId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void closeAccount(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId) {
    lifecycleService.deleteAccount(new DeleteAccountCommand(
        PortfolioId.fromString(portfolioId),
        userId,
        AccountId.fromString(accountId)));
  }
}