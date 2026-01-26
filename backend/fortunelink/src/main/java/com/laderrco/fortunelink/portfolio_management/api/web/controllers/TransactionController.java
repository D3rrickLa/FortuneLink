package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

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
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AssetHoldingHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAssetQueryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioApplicationService;
import com.laderrco.fortunelink.portfolio_management.application.services.PortfolioQueryService;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
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

    // we beed a get method
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        // 1. Map the request path to a Query object
        GetTransactionByIdQuery query = requestMapper.toTransactionQuery(transactionId);

        // 2. Fetch the view from the Query Service
        TransactionView transaction = portfolioQueryService.getTransactionDetails(query);

        // 3. Map to the HTTP Response DTO
        return ResponseEntity.ok(portfolioDtoMapper.toTransactionResponse(transaction));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PagedTransactionResponse> getTransactionHistory(
            @RequestParam String portfolioId,
            @RequestParam(required = false) String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) TransactionType type) {

        // 1. Mapper converts params to your GetTransactionHistoryQuery record
        GetTransactionHistoryQuery query = requestMapper.toHistoryQuery(
                portfolioId, accountId, type, page, size);

        // 2. Query Service returns a Page/List of Views
        // Note: This likely calls a different service method than the "Single ID" one
        Page<TransactionView> history = portfolioQueryService.getTransactionHistory(query);

        // 3. Map the Page to a Paged Response DTO
        return ResponseEntity.ok(portfolioDtoMapper.toPagedResponse(history));
    }

    // ------------------- HELPER -------------------

    private AccountView getAccountView(String portfolioId, String accountId) {
        GetAccountSummaryQuery summaryQuery = portfolioHttpMapper.toCommand(portfolioId,
                new GetAccountRequest(accountId));

        return queryService.getAccountSummary(summaryQuery);
    }
}
