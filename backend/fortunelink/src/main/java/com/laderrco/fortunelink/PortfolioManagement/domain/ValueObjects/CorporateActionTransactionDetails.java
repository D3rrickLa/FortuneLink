package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.math.BigDecimal;
import java.util.Objects;

public class CorporateActionTransactionDetails extends TransactionDetails {
    private final AssetIdentifier assetIdentifier;
    private final BigDecimal splitRatio;

    public CorporateActionTransactionDetails(AssetIdentifier assetIdentifier, BigDecimal splitRatio) {
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null");

        this.assetIdentifier = assetIdentifier;
        this.splitRatio = splitRatio;
    }

    public AssetIdentifier getAssetIdentifier() {
        return assetIdentifier;
    }

    public BigDecimal getSplitRatio() {
        return splitRatio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CorporateActionTransactionDetails that = (CorporateActionTransactionDetails) o;
        return Objects.equals(this.assetIdentifier, that.assetIdentifier)
                && Objects.equals(this.splitRatio, that.splitRatio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.assetIdentifier, this.splitRatio);
    }

}

// NOTE: the current implementation is kind of basic, only handles splits and
// 'other' as a catch all
// in the future when we want to do htings like mergers, spinoffs, etc.
// we would wnat ot make this an abstract class and have concrete classes
// implement said class (representing different corporate actions mentioned
// above)
// said impelmentation would look something like this

/*
 * public abstract class CorporateActionDetails extends TransactionDetails { ...
 * }
 * 
 * public class StockSplitDetails extends CorporateActionDetails { ... }
 * public class MergerDetails extends CorporateActionDetails { ... }
 * 
 * 
 * inside the factory
 * public Transaction createCorporateActionTransaction(
 * UUID transactionId,
 * UUID portfolioId,
 * CorporateActionDetails details, // <--- This is the key change
 * Instant interactionDate,
 * TransactionMetadata transactionMetadata
 * ) {
 * TransactionType type;
 * if (details instanceof StockSplitDetails) {
 * type = TransactionType.STOCK_SPLIT;
 * } else if (details instanceof MergerDetails) {
 * type = TransactionType.CORPORATE_ACTION; // Or create new MERGER type
 * } else {
 * type = TransactionType.CORPORATE_ACTION; // Default or throw error
 * }
 * }
 */