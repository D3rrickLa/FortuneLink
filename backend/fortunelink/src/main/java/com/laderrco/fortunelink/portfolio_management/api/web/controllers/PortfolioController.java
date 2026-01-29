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
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetUsersPortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AccountHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AssetHoldingHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PortfolioHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdatePortfolioCommand;
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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolios")
public class PortfolioController {
    private final PortfolioApplicationService portfolioApplicationService;
    private final PortfolioQueryService portfolioQueryService;
    private final PortfolioDtoMapper portfolioDtoMapper;
    private final PortfolioHttpMapper requestMapper;

    @PostMapping
    public ResponseEntity<PortfolioHttpResponse> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request,
            @AuthenticatedUser UUID userId) {
        CreatePortfolioCommand command = requestMapper.toCommand(request, userId);
        PortfolioView portfolio = portfolioApplicationService.createPortfolio(command);
        PortfolioHttpResponse response = portfolioDtoMapper.toPortfolioResponse(portfolio);
        return ResponseEntity.ok(response);
    }

    // If we want only the owner can access, inject @AuthenticatedUser UUID userId and pass it to the query service
    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioHttpResponse> getPortfolio(@PathVariable String portfolioId) {
        GetPortfolioByIdQuery query = requestMapper.toCommand(portfolioId);
        PortfolioView portfolio = portfolioQueryService.getPortfolioById(query);
        return ResponseEntity.ok(portfolioDtoMapper.toPortfolioResponse(portfolio));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PortfolioHttpResponse>> getUserPortfolios(
            @PathVariable GetUsersPortfolioRequest userId) {
        GetPortfoliosByUserIdQuery query = requestMapper.toCommand(userId);
        List<PortfolioSummaryView> portfolios = portfolioQueryService.getUserPortfolioSummaries(query);

        List<PortfolioHttpResponse> response = portfolios.stream()
                .map(portfolioDtoMapper::toPortfolioResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{portfolioId}")
    public ResponseEntity<PortfolioHttpResponse> updatePortfolio(@PathVariable String portfolioId,
            @Valid @RequestBody CreatePortfolioRequest request) {
        UpdatePortfolioCommand command = requestMapper.toCommand(portfolioId, request);
        PortfolioView portfolio = portfolioApplicationService.updatePortfolio(command);
        PortfolioHttpResponse response = portfolioDtoMapper.toPortfolioResponse(portfolio);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable String id, @RequestBody DeletePortfolioRequest request) {
        DeletePortfolioCommand command = requestMapper.toCommand(request);
        portfolioApplicationService.deletePortfolio(command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/accounts")
    public ResponseEntity<AccountHttpResponse> addAccount(@PathVariable String id,
            @Valid @RequestBody CreateAccountRequest request) {
        AddAccountCommand command = requestMapper.toCommand(id, request);
        AccountView account = portfolioApplicationService.addAccount(command);
        return ResponseEntity.ok(portfolioDtoMapper.toAccountResponse(id, account));
    }

    /**
     * Remove an account from a portfolio.
     * 
     * DELETE /api/portfolios/{id}/accounts/{accountId}
     */
    @DeleteMapping("/{id}/accounts/{accountId}")
    public ResponseEntity<Void> removeAccount(@PathVariable String id, @PathVariable String accountId) {
        RemoveAccountCommand command = requestMapper.toCommand(id, accountId);
        portfolioApplicationService.removeAccount(command);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{portfolioId}/accounts/{accountId}/assets/{assetId}")
    public ResponseEntity<AssetHoldingHttpResponse> getAsset(@PathVariable String portfolioId,
            @PathVariable String accountId, @PathVariable String assetId) {
        // We create a Query object just like your other methods
        GetAssetQueryView query = requestMapper.toAssetQuery(portfolioId, accountId, assetId);
        AssetView asset = portfolioQueryService.getAssetSummary(query);

        return ResponseEntity.ok(portfolioDtoMapper.toAssetResponse(asset));
    }
}
