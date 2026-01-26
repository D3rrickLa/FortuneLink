package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortfolioHttpMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.TransactionCommandAssembler;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.TransactionDtoMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetAccountRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.RecordTransactionRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioApplicationService;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/accounts/{accountId}")
@RequiredArgsConstructor
public class TransactionController {

    private final PortfolioApplicationService applicationService;
    private final PortfolioQueryService queryService;
    private final PortfolioHttpMapper portfolioHttpMapper;
    private final TransactionDtoMapper transactionDtoMapper;
    private final TransactionCommandAssembler transactionCommandAssembler;

    @PostMapping("/buy")
    public ResponseEntity<TransactionResponse> recordTransaction(@PathVariable String portfolioId, @PathVariable String accountId, @Valid @RequestBody RecordTransactionRequest request) {
        GetAccountSummaryQuery summaryQuery = portfolioHttpMapper.toCommand(portfolioId, new GetAccountRequest(accountId));
        AccountView accountView = queryService.getAccountSummary(summaryQuery);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();

        RecordPurchaseCommand command = transactionCommandAssembler.toPurchaseCommand(portfolioId, accountId, accountCurrency, request);
        TransactionView view = applicationService.recordAssetPurchase(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionDtoMapper.toResponse(accountId, view));
    }

    @PostMapping("/sell")
    public ResponseEntity<TransactionResponse> sell(@PathVariable String portfolioId, @PathVariable String accountId, @Valid @RequestBody RecordTransactionRequest request) {
        return null;
    }

    @PostMapping("/dividends")
    public ResponseEntity<TransactionResponse> dividend(@PathVariable String portfolioId, @PathVariable String accountId, @Valid @RequestBody RecordTransactionRequest request) {
        return null;
    }
}