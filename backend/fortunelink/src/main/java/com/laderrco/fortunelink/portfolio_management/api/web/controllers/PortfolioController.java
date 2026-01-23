package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortfolioDtoMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortoflioHttpMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreateAccountRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreatePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.DeletePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetUsersPortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AccountHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PortfolioHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioApplicationService;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioQueryService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolios")
public class PortfolioController {
    private final PortfolioApplicationService portfolioApplicationService;
    private final PortfolioQueryService portfolioQueryService;
    private final PortfolioDtoMapper portfolioDtoMapper;
    private final PortoflioHttpMapper requestMapper;

    @PostMapping
    public ResponseEntity<PortfolioHttpResponse> createPortfolio(@Valid @RequestBody CreatePortfolioRequest request) {
        CreatePortfolioCommand command = requestMapper.toCommand(request);
        PortfolioView portfolio = portfolioApplicationService.createPortfolio(command);
        PortfolioHttpResponse response = portfolioDtoMapper.toPortfolioResponse(portfolio);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioHttpResponse> getPortfolio(@PathVariable String id) {
        GetPortfolioByIdQuery query = requestMapper.toCommand(id);
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

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioHttpResponse> updatePortfolio(@PathVariable String id, @Valid @RequestBody CreatePortfolioRequest request) {
        UpdatePortfolioCommand command = requestMapper.toCommand(id, request);
        PortfolioView portfolio = portfolioApplicationService.updatePortfolio(command);
        PortfolioHttpResponse response = portfolioDtoMapper.toPortfolioResponse(portfolio);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable DeletePortfolioRequest request) {
        DeletePortfolioCommand command = requestMapper.toCommand(request);
        portfolioApplicationService.deletePortfolio(command);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/accounts")
    public ResponseEntity<AccountHttpResponse> addAccount(@PathVariable String id, @Valid @RequestBody CreateAccountRequest request) {
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

    // DO NOT KNOW IF IMMA KEEP THESE
    //   /**
    //  * Add an asset to an account.
    //  * 
    //  * POST /api/portfolios/{portfolioId}/accounts/{accountId}/assets
    //  * Body: {
    //  *   "symbol": "AAPL",
    //  *   "assetType": "STOCK",
    //  *   "quantity": 10,
    //  *   "costBasis": 1500.00,
    //  *   "acquiredDate": "2024-01-15T10:00:00"
    //  * }
    //  */
    // @PostMapping("/{portfolioId}/accounts/{accountId}/assets")
    // public ResponseEntity<PortfolioResponse> addAsset(
    //         @PathVariable String portfolioId,
    //         @PathVariable String accountId,
    //         @Valid @RequestBody AddAssetRequest request) {
        
    //     Portfolio portfolio = portfolioService.addAsset(
    //         portfolioId,
    //         accountId,
    //         request.symbol(),
    //         request.assetType(),
    //         request.quantity(),
    //         request.costBasis(),
    //         request.acquiredDate()
    //     );
        
    //     return ResponseEntity
    //         .status(HttpStatus.CREATED)
    //         .body(mapper.toResponse(portfolio));
    // }
    
    // /**
    //  * Remove an asset from an account.
    //  * 
    //  * DELETE /api/portfolios/{portfolioId}/accounts/{accountId}/assets/{assetId}
    //  */
    // @DeleteMapping("/{portfolioId}/accounts/{accountId}/assets/{assetId}")
    // public ResponseEntity<PortfolioResponse> removeAsset(
    //         @PathVariable String portfolioId,
    //         @PathVariable String accountId,
    //         @PathVariable String assetId) {
        
    //     Portfolio portfolio = portfolioService.removeAsset(portfolioId, accountId, assetId);
    //     return ResponseEntity.ok(mapper.toResponse(portfolio));
    // }
}
