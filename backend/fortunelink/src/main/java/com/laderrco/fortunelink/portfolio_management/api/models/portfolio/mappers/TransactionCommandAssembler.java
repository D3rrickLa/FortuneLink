package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.FeeRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.RecordTransactionRequest;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.SymbolIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionCommandAssembler {

    private final ExchangeRateService exchangeRateService;

    public RecordPurchaseCommand toPurchaseCommand(String portfolioId, String accountId,
            ValidatedCurrency accountBaseCurrency, RecordTransactionRequest request) {
        Money price = toMoney(request.getPrice(), request.getPriceCurrency());

        List<Fee> fees = toFees(request.getFees(), accountBaseCurrency, request.getTransactionDate());

        return new RecordPurchaseCommand(
                toPortfolioId(portfolioId),
                toAccountId(accountId),
                request.getSymbol(),
                request.getQuantity(),
                price,
                fees,
                request.getTransactionDate().toInstant(ZoneOffset.UTC),
                request.getNotes());
    }

    public RecordSaleCommand toSaleCommand(String portfolioId, String accountId, ValidatedCurrency accountBaseCurrency,
            RecordTransactionRequest request) {
        Money price = toMoney(request.getPrice(), request.getPriceCurrency());

        List<Fee> fees = toFees(request.getFees(), accountBaseCurrency, request.getTransactionDate());

        return new RecordSaleCommand(
                toPortfolioId(portfolioId),
                toAccountId(accountId),
                request.getSymbol(),
                request.getQuantity(),
                price,
                fees,
                request.getTransactionDate().toInstant(ZoneOffset.UTC),
                request.getNotes());
    }

    public RecordIncomeCommand toDividendCommand(String portfolioId, String accountId,
            ValidatedCurrency accountBaseCurrency, RecordTransactionRequest request) {
        Money price = toMoney(request.getPrice(), request.getPriceCurrency());
        return new RecordIncomeCommand(
                toPortfolioId(portfolioId),
                toAccountId(accountId),
                request.getSymbol(),
                price,
                TransactionType.valueOf(request.getTransactionType()),
                request.getIsDrip(),
                request.getSharesReceived(),
                request.getTransactionDate().toInstant(ZoneOffset.UTC),
                request.getNotes());
    }

    public RecordDepositCommand toDepositCommand(String portfolioId, String accountId,
            ValidatedCurrency accountBaseCurrency, RecordTransactionRequest request) {
        Money price = toMoney(request.getPrice(), request.getPriceCurrency());
        List<Fee> fees = toFees(request.getFees(), accountBaseCurrency, request.getTransactionDate());
        return new RecordDepositCommand(
                toPortfolioId(portfolioId),
                toAccountId(accountId),
                price,
                fees,
                request.getTransactionDate().toInstant(ZoneOffset.UTC),
                request.getNotes());
    }

    public RecordWithdrawalCommand toWithdrawalCommand(String portfolioId, String accountId,
            ValidatedCurrency accountBaseCurrency, RecordTransactionRequest request) {
        Money price = toMoney(request.getPrice(), request.getPriceCurrency());
        List<Fee> fees = toFees(request.getFees(), accountBaseCurrency, request.getTransactionDate());
        return new RecordWithdrawalCommand(
                toPortfolioId(portfolioId),
                toAccountId(accountId),
                price,
                fees,
                request.getTransactionDate().toInstant(ZoneOffset.UTC),
                request.getNotes());
    }

    public UpdateTransactionCommand toUpdateCommand(String portfolioId, String accountId,
            String transactionId, ValidatedCurrency accountBaseCurrency, RecordTransactionRequest request) {
        Money price = toMoney(request.getPrice(), request.getPriceCurrency());
        List<Fee> fees = toFees(request.getFees(), accountBaseCurrency, request.getTransactionDate());
        AssetIdentifier identifier = null;
        if (request.isAssetTransaction()) {
            // Convert symbol to AssetIdentifier
            identifier = new SymbolIdentifier(request.getSymbol());
        }
        return new UpdateTransactionCommand(
                toPortfolioId(portfolioId),
                toAccountId(accountId),
                toTransactionId(transactionId),
                TransactionType.valueOf(request.getTransactionType()),
                identifier,
                request.getQuantity(),
                price,
                fees,
                request.getTransactionDate().toInstant(ZoneOffset.UTC),
                request.getNotes());
    }

    public DeleteTransactionCommand toDeleteCommand(String portfolioId, String accountId,
            String transactionId, boolean softDelete, @Nullable String notes) {
        return new DeleteTransactionCommand(toPortfolioId(portfolioId), toAccountId(accountId),
                toTransactionId(transactionId), softDelete, notes);
    }

    public

    private List<Fee> toFees(
            List<FeeRequest> feeRequests,
            ValidatedCurrency accountBaseCurrency,
            LocalDateTime fallbackDate) {
        if (feeRequests == null || feeRequests.isEmpty()) {
            return List.of();
        }

        return feeRequests.stream()
                .map(req -> toFee(req, accountBaseCurrency, fallbackDate))
                .toList();
    }

    private Fee toFee(FeeRequest request, ValidatedCurrency accountBaseCurrency, LocalDateTime fallbackDate) {
        Money amount = Money.of(request.amount(), ValidatedCurrency.of(request.currency()));

        ExchangeRate exchangeRate = exchangeRateService
                .getExchangeRate(amount.currency(), accountBaseCurrency)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cannot resolve exchange rate from " + amount.currency() + " to " + accountBaseCurrency));

        Instant feeInstant = (request.feeDate() != null ? request.feeDate() : fallbackDate)
                .toInstant(ZoneOffset.UTC);

        Map<String, String> metadata = request.metadata() != null ? request.metadata() : Map.of();

        return new Fee(request.type(), amount, exchangeRate, metadata, feeInstant);
    }

    private Money toMoney(BigDecimal amount, String currency) {
        return Money.of(amount, currency);
    }

    private PortfolioId toPortfolioId(String id) {
        return new PortfolioId(UUID.fromString(id));
    }

    private AccountId toAccountId(String id) {
        return new AccountId(UUID.fromString(id));
    }

    private TransactionId toTransactionId(String id) {
        return new TransactionId(UUID.fromString(id));
    }
}
