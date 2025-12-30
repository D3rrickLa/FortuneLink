package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.util.List;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;

public class TransactionMapper {
    private TransactionMapper() {}

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

        // Using clear local variables or passing directly to constructor
        return new TransactionResponse(
            transaction.getTransactionId(),
            transaction.getTransactionType(),
            transaction.getAssetIdentifier().getPrimaryId(),
            transaction.getQuantity(),
            transaction.getPricePerUnit(),
            transaction.getFees() != null ? transaction.getFees() : List.of(),
            transaction.calculateTotalCost(),
            transaction.getTransactionDate(),
            transaction.getNotes()
            // In the future, you'd add assetInfo fields here as constructor arguments
        );
    }

    public static List<TransactionResponse> toResponseList(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }

        return transactions.stream()
            .map(t -> toResponse(t, null))
            .toList();
    }
}
