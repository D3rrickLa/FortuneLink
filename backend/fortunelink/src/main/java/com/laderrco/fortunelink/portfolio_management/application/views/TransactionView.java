package com.laderrco.fortunelink.portfolio_management.application.views;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;

// NOTE: issue with this we need switching logic to see if execution is in transaction.java or not
// if not, we use the currency as the 'symbol'
public record TransactionView(
        TransactionId transactionId,
        TransactionType type,
        String symbol,
        Quantity quantity,
        Price price,
        List<Fee> fees,
        Money totalCost,
        Map<String, String> metadata,
        Instant date,
        String notes) {
    public static TransactionView create(Transaction transaction) {
        if (transaction.execution() == null) {
            // this is confirmed a cash event
        }
        return null;
    }
}
