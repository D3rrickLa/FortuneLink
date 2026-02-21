package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.MarketAssetInfo;

@Component
public class TransactionViewMapper {
    // DO we need this? is there some way to prevent this
    private TransactionViewMapper() {
    }

    /**
     * Converts a single Transaction entity to a TransactionResponse record.
     * we have assetinfo for future use where we can 'append more to the response'
     * if (transaction == null) {
     * 
     * @param transaction The domain Transaction entity
     * @param assetInfo   Market data information for the asset (can be null,
     *                    currently unused but kept for future extensibility)
     * @return TransactionResponse record containing transaction details
     */
    public static TransactionView toResponse(Transaction transaction, MarketAssetInfo assetInfo) {

        // Using clear local variables or passing directly to constructor
        // NOTE: THIS MAPPER IS WRONG AS IN THE TRANSACTION NOW DOESN'T HAVE THESE
        // WE NEED A WAY TO DISTINGUSH WHAT TYPE OF TRANSACTION IS WHAT
        // return new TransactionView(
        //         transaction.transactionId(),
        //         transaction.transactionType(),
        //         transaction.getAssetIdentifier().getPrimaryId(),
        //         transaction.getQuantity(),
        //         transaction.getPricePerUnit(),
        //         transaction.getFees() != null ? transaction.getFees() : List.of(),
        //         transaction.calculateTotalCost(),
        //         transaction.getTransactionDate(),
        //         transaction.getNotes()
        // // In the future, you'd add assetInfo fields here as constructor arguments
        // );

        return TransactionView.create(transaction);
    }

    public static List<TransactionView> toResponseList(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }

        return transactions.stream()
                .map(t -> toResponse(t, null))
                .toList();
    }
}
