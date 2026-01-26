package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.PortfolioHttpMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.TransactionCommandAssembler;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers.TransactionDtoMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.DeleteTransactionRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetAccountRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.RecordTransactionRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
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

    // ------------------- CREATE TRANSACTIONS -------------------

    @PostMapping("/buy")
    public ResponseEntity<TransactionResponse> buy(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @Valid @RequestBody RecordTransactionRequest request) {

        request.validateFields();

        AccountView accountView = getAccountView(portfolioId, accountId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();

        RecordPurchaseCommand command = transactionCommandAssembler.toPurchaseCommand(
                portfolioId, accountId, accountCurrency, request);

        TransactionView view = applicationService.recordAssetPurchase(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionDtoMapper.toResponse(accountId, view));
    }

    @PostMapping("/sell")
    public ResponseEntity<TransactionResponse> sell(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @Valid @RequestBody RecordTransactionRequest request) {

        request.validateFields();

        AccountView accountView = getAccountView(portfolioId, accountId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();

        RecordSaleCommand command = transactionCommandAssembler.toSaleCommand(
                portfolioId, accountId, accountCurrency, request);

        TransactionView view = applicationService.recordAssetSale(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionDtoMapper.toResponse(accountId, view));
    }

    @PostMapping("/dividends")
    public ResponseEntity<TransactionResponse> dividend(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @Valid @RequestBody RecordTransactionRequest request) {

        request.validateFields();

        AccountView accountView = getAccountView(portfolioId, accountId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();

        RecordIncomeCommand command = transactionCommandAssembler.toDividendCommand(
                portfolioId, accountId, accountCurrency, request);

        TransactionView view = applicationService.recordDividendIncome(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionDtoMapper.toResponse(accountId, view));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @Valid @RequestBody RecordTransactionRequest request) {

        request.validateFields();
        AccountView accountView = getAccountView(portfolioId, accountId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();
        RecordDepositCommand command = transactionCommandAssembler.toDepositCommand(
                portfolioId, accountId, accountCurrency, request);

        TransactionView view = applicationService.recordDeposit(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionDtoMapper.toResponse(accountId, view));
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<TransactionResponse> withdrawal(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @Valid @RequestBody RecordTransactionRequest request) {
        AccountView accountView = getAccountView(portfolioId, accountId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();
        request.validateFields();

        RecordWithdrawalCommand command = transactionCommandAssembler.toWithdrawalCommand(
                portfolioId, accountId, accountCurrency, request);

        TransactionView view = applicationService.recordWithdrawal(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionDtoMapper.toResponse(accountId, view));
    }

    // ------------------- UPDATE TRANSACTIONS -------------------

    @PutMapping("/update/{transactionId}")
    public ResponseEntity<TransactionResponse> update(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @PathVariable String transactionId,
            @Valid @RequestBody RecordTransactionRequest request) {

        request.validateFields(); // validate required fields per transaction type
        AccountView accountView = getAccountView(portfolioId, accountId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();
        UpdateTransactionCommand command = transactionCommandAssembler.toUpdateCommand(portfolioId, accountId,
                transactionId, accountCurrency, request);

        TransactionView view = applicationService.updateTransation(command);

        return ResponseEntity.ok(transactionDtoMapper.toResponse(accountId, view));
    }

    // ------------------- DELETE TRANSACTIONS -------------------

    @DeleteMapping("/delete/{transactionId}")
    public ResponseEntity<Void> delete(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @PathVariable String transactionId,
            @RequestParam(defaultValue = "true") boolean softDelete,
            @Valid @RequestBody(required = false) DeleteTransactionRequest request) {

        String notes = request != null ? request.getNotes() : null;

        DeleteTransactionCommand command = transactionCommandAssembler.toDeleteCommand(
                portfolioId, accountId, transactionId, softDelete, notes);

        applicationService.deleteTransaction(command);

        return ResponseEntity.noContent().build();
    }

    // ------------------- HELPER -------------------

    private AccountView getAccountView(String portfolioId, String accountId) {
        GetAccountSummaryQuery summaryQuery = portfolioHttpMapper.toCommand(portfolioId,
                new GetAccountRequest(accountId));

        return queryService.getAccountSummary(summaryQuery);
    }
}
