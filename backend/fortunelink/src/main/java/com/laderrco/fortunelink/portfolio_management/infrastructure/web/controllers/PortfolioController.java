package com.laderrco.fortunelink.portfolio_management.infrastructure.web.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioApplicationService;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.PortfolioDtoMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.RequestDtoMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.requests.CreatePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.responses.PortfolioResponse;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolios")
public class PortfolioController {
    private final PortfolioApplicationService portfolioApplicationService;
    private final PortfolioDtoMapper portfolioDtoMapper;
    private final RequestDtoMapper requestMapper;

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(@Valid @RequestBody CreatePortfolioRequest request) {
        CreatePortfolioCommand command = new CreatePortfolioCommand(new UserId(request.userId()), request.name(), ValidatedCurrency.of(request.currencyPreference()), request.description(), request.createAccount());
        Portfolio portfolio = portfolioApplicationService.createPortfolio(command);
    }
}
