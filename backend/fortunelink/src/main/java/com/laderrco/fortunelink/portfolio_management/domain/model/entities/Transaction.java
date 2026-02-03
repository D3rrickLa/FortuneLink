package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

import io.micrometer.common.lang.Nullable;
import lombok.Builder;

/*



*/
@Builder
public record Transaction(
        TransactionId transactionId,
        AccountId accountId,
        TransactionType transactionType,
        Money cashDelta, // cash impact, what left/came into the bank account
        @Nullable TradeExecution execution, // only trade types have this
        List<Fee> fees,
        Instant occurredAt,
        String notes,
        @Nullable TransactionId relatedTransactionId,
        TransactionMetadata metadata) implements ClassValidation {

    public Transaction {

        fees = List.copyOf(fees);
        notes = notes.trim();

        boolean requiresExecution = switch (transactionType) {
            case BUY, SELL, DIVIDEND_REINVEST, SPLIT -> true;
            default -> false;
        };

        if (requiresExecution && execution == null) {
            throw new IllegalArgumentException(String.format("%s requires trade execution details", transactionType));
        }

        if (!requiresExecution && execution != null) {
            throw new IllegalArgumentException(
                    String.format("%s cannot have  trade execution details", transactionType));
        }

    }

    public record TradeExecution(AssetSymbol assetSymbol, Quantity quantity, Price pricePerUnit) {
        public Money grossValue() {
            return pricePerUnit.pricePerUnit().multiply(quantity.amount());
        }
    }

    public record TransactionMetadata(Map<String, String> values) {
        public TransactionMetadata {
            values = values == null ? Map.of() : Map.copyOf(values);
        }

        public String get(String key) {
            return values.get(key);
        }

        public TransactionMetadata with(String key, String value) {
            Map<String, String> copy = new HashMap<>(values);
            copy.put(key, value);
            return new TransactionMetadata(copy);
        }
    }

}