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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Accounts", description = "Lifecycle and query operations for portfolio accounts")
public class AccountController {
  private final AccountLifecycleService lifecycleService;
  private final AccountQueryService accountQueryService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new account", description = "Initializes a new account within a portfolio with a specific currency and strategy.")
  @ApiResponse(responseCode = "201", description = "Account created successfully")
  public AccountView createAccount(
      @PathVariable @Schema(example = "p-123") String portfolioId,
      @AuthenticatedUser UserId userId,
      @RequestBody @Valid CreateAccountRequest request) {

    return lifecycleService.createAccount(
        new CreateAccountCommand(PortfolioId.fromString(portfolioId), userId, request.accountName(),
            request.accountType(), request.strategy(), Currency.of(request.currency())));
  }

  @PutMapping("/{accountId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Update account details", description = "Updates mutable properties of an account like its display name.")
  public void updateAccount(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId,
      @RequestBody @Valid UpdateAccountRequest request) {

    lifecycleService.updateAccount(
        new UpdateAccountCommand(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId), request.accountName()));
  }

  @DeleteMapping("/{accountId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Close an account", description = "Transitions account to CLOSED state. Requires zero balance and no open positions.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Account closed"),
      @ApiResponse(responseCode = "409", description = "Conflict: Account has remaining balance or positions")
  })
  public void closeAccount(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId) {
    lifecycleService.deleteAccount(
        new DeleteAccountCommand(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId)));
  }

  @PatchMapping("/{accountId}/reopen")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Reopen a closed account", description = "Transitions a CLOSED account back to ACTIVE, preserving all history.")
  public void reopenAccount(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId) {

    lifecycleService.reopenAccount(new ReopenAccountCommand(AccountId.fromString(accountId),
        PortfolioId.fromString(portfolioId), userId));
  }

  @GetMapping
  @Operation(summary = "List all accounts", description = "Returns all accounts in the portfolio. Note: Fetches fresh market data; cache results where possible.")
  @Parameter(name = "pageable", hidden = true) // Hides the complex Spring object
  @Parameter(in = ParameterIn.QUERY, name = "page", description = "Zero-based page index", schema = @Schema(type = "integer", defaultValue = "0"))
  @Parameter(in = ParameterIn.QUERY, name = "size", description = "Page size", schema = @Schema(type = "integer", defaultValue = "20"))
  public Page<AccountView> getAllAccounts(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      Pageable pageable) {
    return accountQueryService.getAllAccounts(
        new GetAllAccountsQuery(PortfolioId.fromString(portfolioId), userId,
            pageable.getPageNumber(), pageable.getPageSize()));
  }

  @GetMapping("/{accountId}")
  @Operation(summary = "Get account summary", description = "Returns a single account with current positions and live market valuation.")
  public AccountView getAccount(
      @PathVariable String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable String accountId) {
    return accountQueryService.getAccountSummary(
        new GetAccountSummaryQuery(PortfolioId.fromString(portfolioId), userId,
            AccountId.fromString(accountId)));
  }
}