package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortfolioDtoMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortfolioHttpMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreateAccountRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreatePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.DeletePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetAccountRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetUsersPortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AccountHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AssetHoldingHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PortfolioHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAssetQueryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AssetView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioApplicationService;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.security.AuthenticatedUser;

import jakarta.annotation.Nonnull;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
NOTE: the get portoflio stuff are suppor to be a lightweight get method showing identitiy, ownership and jsut basic details
we have other speicifc method for like accounts and such
*/
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolios")
public class PortfolioController {
    private final PortfolioApplicationService portfolioApplicationService;
    private final PortfolioQueryService portfolioQueryService;
    private final PortfolioDtoMapper portfolioDtoMapper;
    private final PortfolioHttpMapper requestMapper;

    @GetMapping("health-check")
    public ResponseEntity<String> check() {
        return ResponseEntity.ok("Backend is reachable!");
    }

    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioHttpResponse> getPortfolio(@Nonnull @PathVariable String portfolioId, @AuthenticatedUser UUID userId) {
        GetPortfolioByIdQuery query = requestMapper.toCommand(portfolioId, userId);
        PortfolioView portfolioView = portfolioQueryService.getPortfolioById(query);
        return ResponseEntity.ok(portfolioDtoMapper.toPortfolioResponse(portfolioView));
    }

    // THIS IS BROKEN
    // LONG TERM: DON'T USE PortfolioHttpREsponse, USE A NEW ONE CALLED PortfolioSummaryHttpResponse
    /*
     * [
     * {
     * "id": "PortfolioId[portfolioId=34544865-3624-4516-9f67-179231a5c32b]",
     * "userId": null,
     * "name": null,
     * "description": null,
     * "accounts": null,
     * "totalValue": 0E-34,
     * "totalValueCurrency": "USD",
     * "createdDate": null,
     * "lastUpdated": "2026-01-29T01:10:47.594363"
     * }
     * ]
     */
    @GetMapping("/user/me")
    public ResponseEntity<List<PortfolioHttpResponse>> getUserPortfolios(@AuthenticatedUser UUID userId) {
        GetPortfoliosByUserIdQuery query = requestMapper.toCommand(new GetUsersPortfolioRequest(userId.toString()));
        List<PortfolioSummaryView> portfolios = portfolioQueryService.getUserPortfolioSummaries(query);

        List<PortfolioHttpResponse> response = portfolios.stream()
            .map(portfolioDtoMapper::toPortfolioResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<PortfolioHttpResponse> createPortfolio(@AuthenticatedUser UUID userId, @Valid @RequestBody CreatePortfolioRequest request) {
        CreatePortfolioCommand command = requestMapper.toCommand(request, userId);
        PortfolioView portfolioView = portfolioApplicationService.createPortfolio(command);
        return ResponseEntity.ok(portfolioDtoMapper.toPortfolioResponse(portfolioView));
    }

    @PutMapping("/{portfolioId}")
    public ResponseEntity<PortfolioHttpResponse> updatePortfolio(@Nonnull @PathVariable String portfolioId, @AuthenticatedUser UUID userId, @Valid @RequestBody CreatePortfolioRequest request) {
        UpdatePortfolioCommand command = requestMapper.toCommand(portfolioId, userId, request);
        PortfolioView portfolioView = portfolioApplicationService.updatePortfolio(command);
        return ResponseEntity.ok(portfolioDtoMapper.toPortfolioResponse(portfolioView));
    }
    
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable String portfolioId, @AuthenticatedUser UUID userId, @RequestBody DeletePortfolioRequest request) {
        DeletePortfolioCommand command = requestMapper.toCommand(portfolioId, userId, request);
        portfolioApplicationService.deletePortfolio(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{portfolioId}/accounts/{accountId}")
    public ResponseEntity<AccountHttpResponse> getAccount(@PathVariable String portfolioId,  @AuthenticatedUser UUID userId, @PathVariable String accountId) {
        // Fetch the account view from the service
        GetAccountRequest request = new GetAccountRequest(accountId);
        GetAccountSummaryQuery query = requestMapper.toAccountQuery(portfolioId, userId, request);
        AccountView account = portfolioQueryService.getAccountSummary(query);
        return ResponseEntity.ok(portfolioDtoMapper.toAccountResponse(portfolioId, account));
    }

    @GetMapping("/{portfolioId}/accounts")
    public ResponseEntity<List<AccountHttpResponse>> listAccounts(@PathVariable String portfolioId, @AuthenticatedUser UUID userId) {
        // Fetch the list of account views
        List<AccountView> accounts = portfolioQueryService.listAccountSummaries(portfolioId, userId);
    
        List<AccountHttpResponse> response = accounts.stream()
            .map(account -> portfolioDtoMapper.toAccountResponse(portfolioId, account))
            .toList();

    return ResponseEntity.ok(response);
    }

    @PostMapping("/{portfolioId}/accounts")
    public ResponseEntity<AccountHttpResponse> addAccount(@PathVariable String portfolioId, @AuthenticatedUser UUID userId, @Valid @RequestBody CreateAccountRequest request) {
        AddAccountCommand command = requestMapper.toCommand(portfolioId, userId, request);
        AccountView account = portfolioApplicationService.addAccount(command);
        return ResponseEntity.ok(portfolioDtoMapper.toAccountResponse(portfolioId, account));
    }

    @DeleteMapping("/{portfolioId}/accounts/{accountId}")
    public ResponseEntity<Void> removeAccount(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @AuthenticatedUser UUID userId) {

        RemoveAccountCommand command = requestMapper.toCommand(portfolioId, userId, accountId);

        portfolioApplicationService.removeAccount(command);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{portfolioId}/accounts/{accountId}/assets/{assetId}")
    public ResponseEntity<AssetHoldingHttpResponse> getAsset(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @PathVariable String assetId,
            @AuthenticatedUser UUID userId) {

        GetAssetQueryView query = requestMapper.toAssetQuery(portfolioId, userId, accountId, assetId);
        AssetView asset = portfolioQueryService.getAssetSummary(query);

        return ResponseEntity.ok(portfolioDtoMapper.toAssetResponse(asset)
        );
    }
}
