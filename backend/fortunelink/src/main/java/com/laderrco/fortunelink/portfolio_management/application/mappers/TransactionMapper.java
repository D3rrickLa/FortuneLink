package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class TransactionMapper {
    private TransactionMapper() {
        // Private con to prevent instantiation
    }

    /**
     * Converts a single Transaction entity to a TransactionResponse record.
     * 
     * @param transaction The domain Transaction entity
     * @param assetInfo Market data information for the asset (can be null, currently unused but kept for future extensibility)
     * @return TransactionResponse record containing transaction details
    */
    public static TransactionResponse toResponse(Transaction transaction, MarketAssetInfo assetInfo) { // we have assetinfo for future use where we can 'append more to the response'
        if (transaction == null) {
            return null;
        }

        TransactionId transactionId = transaction.getTransactionId();
        TransactionType type = transaction.getTransactionType();
        String symbol = transaction.getAssetIdentifier().getPrimaryId();
        BigDecimal quantity = transaction.getQuantity();
        Money price = transaction.getPricePerUnit();

        // Extract fees as a list
        List<Fee> fees = transaction.getFees() != null ? transaction.getFees() : List.of();

        Money totalCost = transaction.calculateTotalCost();

        Instant date = transaction.getTransactionDate();

        String notes = transaction.getNotes();

        if (assetInfo != null) {
            // this is what we would do
            // response.setAssetName(assetInfo.getName());
            // response.setAssetType(assetInfo.getAssetType());
            // response.setSector(assetInfo.getSector());
            // response.setExchange(assetInfo.getExchange());
        }
        
        return new TransactionResponse(transactionId, type, symbol, quantity, price, fees, totalCost, date, notes);
    }

    public List<TransactionResponse> toResponseList(List<Transaction> transactions) {
        if (transactions == null) {
            return null;
        }

        return transactions.stream()
            .map(t -> toResponse(t, null))
            .collect(Collectors.toList());
    }
}
