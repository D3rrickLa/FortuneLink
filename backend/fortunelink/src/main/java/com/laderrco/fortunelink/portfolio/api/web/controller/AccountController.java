package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.api.web.dto.requests.CreateAccountRequest;
import com.laderrco.fortunelink.portfolio.api.web.dto.requests.UpdateAccountRequest;
import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.ReopenAccountCommand;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts")
public class AccountController {
  private final AccountLifecycleService lifecycleService;
  private final AccountQueryService accountQueryService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public AccountView createAccount(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @RequestBody @Valid CreateAccountRequest request) {

    return lifecycleService.createAccount(
        new CreateAccountCommand(PortfolioId.fromString(portfolioId), userId, request.accountName(),
            request.accountType(), request.strategy(), Currency.of(request.currency())));
  }

  @PutMapping("/{accountId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void updateAccount(@PathVariable String portfolioId, @AuthenticatedUser UserId userId,
      @PathVariable String accountId, @RequestBody @Valid UpdateAccountRequest request) {

    lifecycleService.updateAccount(
        new UpdateAccountCommand(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId), request.accountName()));
  }

  /**
   * Closes an account (soft delete / lifecycle transition to CLOSED).
   * <p>
   * Requires the account to have zero positions and zero cash balance. Returns 409
   * ACCOUNT_CANNOT_BE_CLOSED if these conditions are not met.
   * <p>
   * A closed account can be reopened via PATCH /{accountId}/reopen. Closed accounts remain visible
   * in read queries and their transaction history is preserved permanently.
   */
  @DeleteMapping("/{accountId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void closeAccount(@PathVariable String portfolioId, @AuthenticatedUser UserId userId,
      @PathVariable String accountId) {
    lifecycleService.deleteAccount(
        new DeleteAccountCommand(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId)));
  }

  /**
   * Reopens a previously closed account.
   * <p>
   * The account transitions from CLOSED back to ACTIVE. All historical data (transactions,
   * positions, realized gains) is preserved exactly as it was.
   * <p>
   * Use cases: - Account was closed in error - Seasonal account (e.g. annual RRSP contribution
   * account) - Migration: closed old account, then re-importing historical data
   * <p>
   * Returns 409 ACCOUNT_CANNOT_BE_REOPENED if the account is not in CLOSED state (e.g. trying to
   * reopen an ACTIVE or REPLAYING account).
   */
  @PatchMapping("/{accountId}/reopen")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reopenAccount(@PathVariable String portfolioId, @AuthenticatedUser UserId userId,
      @PathVariable String accountId) {

    lifecycleService.reopenAccount(new ReopenAccountCommand(AccountId.fromString(accountId),
        PortfolioId.fromString(portfolioId), userId));
  }

  /**
   * Returns all accounts in a portfolio with their current positions and balances.
   * <p>
   * This fires one batch market data call for all symbols across all accounts. It is not paginated
   * , the MVP enforces one portfolio per user and the expected account count per portfolio is low
   * (< 20). Add pagination when the multi-portfolio feature ships.
   * <p>
   * Performance note: each call fetches fresh market quotes for all positions. Cache aggressively
   * on the client , the underlying Redis TTL is 5 minutes.
   */
  @GetMapping
  // Spring automatically populates this from query params
  public Page<AccountView> getAllAccounts(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, Pageable pageable) {
    return accountQueryService.getAllAccounts(
        new GetAllAccountsQuery(PortfolioId.fromString(portfolioId), userId,
            pageable.getPageNumber(), pageable.getPageSize()));
  }

  /**
   * Returns a single account with its current positions, balances, and market values.
   * <p>
   * Fires a targeted market data batch call scoped to this account's symbols only. Use this for the
   * account detail page. Prefer /portfolios/{id} for a full overview.
   */
  @GetMapping("/{accountId}")
  public AccountView getAccount(@PathVariable String portfolioId, @AuthenticatedUser UserId userId,
      @PathVariable String accountId) {
    return accountQueryService.getAccountSummary(
        new GetAccountSummaryQuery(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId)));
  }
}