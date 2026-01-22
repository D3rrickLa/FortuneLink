package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortfolioDtoMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortoflioHttpMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreatePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PortfolioHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
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
        PortfolioHttpResponse response = portfolioDtoMapper.toResponse(portfolio);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioHttpResponse> getPortfolio(@PathVariable UUID id) {
        return null;
    }
}
