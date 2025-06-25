// this class is meant for to be flexxible in terms of storing a specific transaction type (i.e. liability vs. asset transaction)

package com.laderrco.fortunelink.PortfolioManagement.domain.factories;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Transaction;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Fee;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionMetadata;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.DecimalPrecision;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
// Precision used in Money class and Quantities variables

public class TransactionFactory {
    // include everything for transaction + things related to the
    // entityTransactionDetails

    public static Transaction createBuyAssetTransaction(UUID transactionId, UUID portfolioId,
            AssetIdentifier assetIdentifier, Instant transactionDate, BigDecimal quantity, Money pricePerUnit,
            TransactionDetails transactionDetails, TransactionMetadata transactionMetadata, List<Fee> associatedFees) {

        // problem, we need to know what we have bought, right now we only know if it's
        // a stock/etf or crypto through AssetIdentifer
        // the IF statement right now seems to fix that issue, but say if the asset we bought were bonds, like acutal T-Bills
        // and not ETFs of bonds, that will pose a problem + any other asset in the future we could think of.
        // for now though I think we are sticking to use STOCKS - on the exchange and crypto
        RoundingMode roundMode = RoundingMode.HALF_UP;
        int cashDecimalPlaces;
        
        if (assetIdentifier.isStockOrETF()) {
            cashDecimalPlaces = DecimalPrecision.STOCK.getDecimalPlaces();
        } else {
            cashDecimalPlaces = DecimalPrecision.CRYPTO.getDecimalPlaces();
        }

        Money costOfAssets = pricePerUnit.mulitply(quantity);
        Money totalFees = associatedFees != null
                ? associatedFees.stream().map(Fee::amount).reduce(Money.ZERO(pricePerUnit.currency()), Money::add)
                : Money.ZERO(pricePerUnit.currency());
        Money totalTransactionAmount = costOfAssets.add(totalFees);

        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(assetIdentifier, quantity,
                pricePerUnit);

        return new Transaction.Builder()
                .transactionId(transactionId)
                .portfolioId(portfolioId)
                .transactionType(TransactionType.BUY)
                .totalTransactionAmount(totalTransactionAmount)
                .transactionDate(transactionDate)
                .transactionDetails(assetTransactionDetails)
                .transactionMetadata(transactionMetadata)
                .fees(associatedFees)
                .build();
    }

}