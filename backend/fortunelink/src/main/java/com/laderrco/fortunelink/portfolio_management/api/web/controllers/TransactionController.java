package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PagedTransactionHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.TransactionHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionHistoryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioApplicationService;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.security.AuthenticatedUser;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/portfolios/{portfolioId}/accounts/{accountId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final PortfolioApplicationService applicationService;
    private final PortfolioQueryService queryService;
    private final PortfolioHttpMapper portfolioHttpMapper;
    private final TransactionDtoMapper transactionDtoMapper;
    private final TransactionCommandAssembler transactionCommandAssembler;

    // ------------------- GET TRANSACTION -------------------

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionHttpResponse> getTransaction(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @PathVariable String transactionId,
            @AuthenticatedUser UUID userId) {

        GetTransactionByIdQuery query =
                transactionCommandAssembler.toTransactionQuery(
                        portfolioId,
                        accountId,
                        transactionId,
                        userId
                );

        TransactionView transaction =
                queryService.getTransactionDetails(query);

        return ResponseEntity.ok(
                transactionDtoMapper.toResponse(accountId, transaction)
        );
    }

    // ------------------- GET TRANSACTION HISTORY -------------------

    @GetMapping
    public ResponseEntity<PagedTransactionHttpResponse> getTransactionHistory(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticatedUser UUID userId) {

        GetTransactionHistoryQuery query =
                transactionCommandAssembler.toHistoryQuery(
                        portfolioId,
                        accountId,
                        userId,
                        type,
                        startDate,
                        endDate,
                        page,
                        size
                );

        TransactionHistoryView history =
                queryService.getTransactionHistory(query);

        return ResponseEntity.ok(
                transactionDtoMapper.toPagedResponse(accountId, history)
        );
    }

    // ------------------- CREATE TRANSACTIONS -------------------

    @PostMapping("/buy")
    public ResponseEntity<TransactionHttpResponse> buy(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @Valid @RequestBody RecordTransactionRequest request,
            @AuthenticatedUser UUID userId) {

        request.validateFields();

        AccountView accountView = getAccountView(portfolioId, accountId, userId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();

        RecordPurchaseCommand command =
                transactionCommandAssembler.toPurchaseCommand(
                        portfolioId,
                        accountId,
                        userId,
                        accountCurrency,
                        request
                );

        TransactionView view =
                applicationService.recordAssetPurchase(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionDtoMapper.toResponse(accountId, view));
    }

    @PostMapping("/sell")
    public ResponseEntity<TransactionHttpResponse> sell(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @Valid @RequestBody RecordTransactionRequest request,
            @AuthenticatedUser UUID userId) {

        request.validateFields();

        AccountView accountView = getAccountView(portfolioId, accountId, userId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();

        RecordSaleCommand command =
                transactionCommandAssembler.toSaleCommand(
                        portfolioId,
                        accountId,
                        userId,
                        accountCurrency,
                        request
                );

        TransactionView view =
                applicationService.recordAssetSale(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionDtoMapper.toResponse(accountId, view));
    }

    @PostMapping("/dividends")
    public ResponseEntity<TransactionHttpResponse> dividend(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @Valid @RequestBody RecordTransactionRequest request,
            @AuthenticatedUser UUID userId) {

        request.validateFields();

        AccountView accountView = getAccountView(portfolioId, accountId, userId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();

        RecordIncomeCommand command =
                transactionCommandAssembler.toDividendCommand(
                        portfolioId,
                        accountId,
                        userId,
                        accountCurrency,
                        request
                );

        TransactionView view =
                applicationService.recordDividendIncome(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionDtoMapper.toResponse(accountId, view));
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionHttpResponse> deposit(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @Valid @RequestBody RecordTransactionRequest request,
            @AuthenticatedUser UUID userId) {

        request.validateFields();

        AccountView accountView = getAccountView(portfolioId, accountId, userId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();

        RecordDepositCommand command =
                transactionCommandAssembler.toDepositCommand(
                        portfolioId,
                        accountId,
                        userId,
                        accountCurrency,
                        request
                );

        TransactionView view =
                applicationService.recordDeposit(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionDtoMapper.toResponse(accountId, view));
    }

    @PostMapping("/withdrawal")
    public ResponseEntity<TransactionHttpResponse> withdrawal(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @Valid @RequestBody RecordTransactionRequest request,
            @AuthenticatedUser UUID userId) {

        request.validateFields();

        AccountView accountView = getAccountView(portfolioId, accountId, userId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();

        RecordWithdrawalCommand command =
                transactionCommandAssembler.toWithdrawalCommand(
                        portfolioId,
                        accountId,
                        userId,
                        accountCurrency,
                        request
                );

        TransactionView view =
                applicationService.recordWithdrawal(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionDtoMapper.toResponse(accountId, view));
    }

    // ------------------- UPDATE TRANSACTION -------------------

    @PutMapping("{transactionId}")
    public ResponseEntity<TransactionHttpResponse> update(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @PathVariable String transactionId,
            @Valid @RequestBody RecordTransactionRequest request,
            @AuthenticatedUser UUID userId) {

        request.validateFields();

        AccountView accountView = getAccountView(portfolioId, accountId, userId);
        ValidatedCurrency accountCurrency = accountView.baseCurrency();

        UpdateTransactionCommand command =
                transactionCommandAssembler.toUpdateCommand(
                        portfolioId,
                        accountId,
                        transactionId,
                        userId,
                        accountCurrency,
                        request
                );

        TransactionView view =
                applicationService.updateTransaction(command);

        return ResponseEntity.ok(
                transactionDtoMapper.toResponse(accountId, view)
        );
    }

    // ------------------- DELETE TRANSACTION -------------------

    @DeleteMapping("{transactionId}")
    public ResponseEntity<Void> delete(
            @PathVariable String portfolioId,
            @PathVariable String accountId,
            @PathVariable String transactionId,
            @RequestParam(defaultValue = "true") boolean softDelete,
            @Valid @RequestBody(required = false) DeleteTransactionRequest request,
            @AuthenticatedUser UUID userId) {

        String notes = request != null ? request.getNotes() : null;

        DeleteTransactionCommand command =
                transactionCommandAssembler.toDeleteCommand(
                        portfolioId,
                        accountId,
                        transactionId,
                        userId,
                        softDelete,
                        notes
                );

        applicationService.deleteTransaction(command);

        return ResponseEntity.noContent().build();
    }

    // ------------------- HELPER -------------------

    private AccountView getAccountView(
            String portfolioId,
            String accountId,
            UUID userId) {

        GetAccountSummaryQuery summaryQuery =
                portfolioHttpMapper.toAccountQuery(
                        portfolioId,
                        userId,
                        new GetAccountRequest(accountId)
                );

        return queryService.getAccountSummary(summaryQuery);
    }
}
