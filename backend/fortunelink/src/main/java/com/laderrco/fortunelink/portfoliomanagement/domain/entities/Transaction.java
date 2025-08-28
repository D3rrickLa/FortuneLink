package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.CashTransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TradeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.CorrelationId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.AccountTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.TradeTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.TransactionDetails;

public class Transaction {
    private final TransactionId transactionId;
    private final TransactionId parentTransactionId; // dirved 
    private TransactionId reversalTransactionId; 
    private final CorrelationId correlationId; // multi-tx events
    private final PortfolioId portfolioId;

    private final TransactionType type;
    private TransactionStatus status;
    private final TransactionDetails details;
    private final Instant transactionDate;

    private final Money transactionNetImpact; // in portfolio's currency

    private boolean hidden;
    private int version;
    private final Instant createdAt;
    private Instant updatedAt;

    private final Map<String,String> metadata;

    private static final Set<TransactionType> REVERSAL_TYPES = Set.of(
        CashTransactionType.REVERSAL
    );

    


    public Transaction(TransactionId transactionId, TransactionId parentTransactionId,
            TransactionId reversalTransactionId, CorrelationId correlationId, PortfolioId portfolioId,
            TransactionType type, TransactionStatus status, TransactionDetails details, Instant transactionDate,
            Money transactionNetImpact, boolean hidden, int version, Instant createdAt, Instant updatedAt,
            Map<String, String> metadata) {
        this.transactionId = transactionId;
        this.parentTransactionId = parentTransactionId;
        this.reversalTransactionId = reversalTransactionId;
        this.correlationId = correlationId;
        this.portfolioId = portfolioId;
        this.type = type;
        this.status = status;
        this.details = details;
        this.transactionDate = transactionDate;
        this.transactionNetImpact = transactionNetImpact;
        this.hidden = hidden;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.metadata = metadata;
    }

    // delegates net cost calc to details
    // TODO check if this is handling fees or not
    public Money getNetCostInPortfolioCurrency() {
        if (details instanceof TradeTransactionDetails tradeDetails) {
            return tradeDetails.calculateCashImpact((TradeType) type);
        }
        else if (details instanceof AccountTransactionDetails acctDetails) {
            return acctDetails.getNetWorthImpact().getConversionAmount();
        }
        return transactionNetImpact;
    }

    public Optional<Money> getRealizedGainLoss() {
        if (details instanceof TradeTransactionDetails tradeDetails) {
            return Optional.ofNullable(tradeDetails.getRealizedGainLoss())
                .map(MonetaryAmount::getConversionAmount);
        }
        return Optional.empty();
    }

    public boolean isReversed() {
        return REVERSAL_TYPES.contains(this.type);
    }

    public boolean canBeUpdated() {
        return this.status != TransactionStatus.FINALIZED || this.status != TransactionStatus.CANCELLED;
    }

    public void updateStatus(TransactionStatus newStatus, Instant updatedAt) {
        newStatus = Objects.requireNonNull(newStatus, "New status cannot be null.");
        updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null.");

        if (updatedAt.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("UpdatedAt cannot go backwards.");
        }

        this.status = newStatus;
        this.updatedAt = updatedAt;        
        updateVersion();
    }

    public void hide(Instant updatedAt) {
        updateVisibility(true, updatedAt);
    }

    public void unhide(Instant updatedAt) {
        updateVisibility(false, updatedAt);
    }

    public void reverse(TransactionId reversalTransactionId, Instant reversedAt) {
        if (!canBeUpdated()) {
            throw new IllegalStateException("Cannot reverse a finalized or cancelled transaction.");
        }
        if (isReversed()) {
            throw new IllegalArgumentException("Transaction already reversed.");
        }
        reversalTransactionId = Objects.requireNonNull(reversalTransactionId, "Reversal transaction id cannot be null.");
        reversedAt = Objects.requireNonNull(reversedAt, "Reversed at cannot be null.");

        this.status = TransactionStatus.REVERSED;
        this.hidden = true;
        this.updatedAt = reversedAt;
        updateVersion();
    }

    

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public TransactionId getParentTransactionId() {
        return parentTransactionId;
    }

    public CorrelationId getCorrelationId() {
        return correlationId;
    }

    public PortfolioId getPortfolioId() {
        return portfolioId;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public TransactionDetails getDetails() {
        return details;
    }

    public Instant getTransactionDate() {
        return transactionDate;
    }

    public Money getTransactionNetImpact() {
        return transactionNetImpact;
    }

    public boolean isHidden() {
        return hidden;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public static Set<TransactionType> getReversalTypes() {
        return REVERSAL_TYPES;
    }

    private void updateVisibility(boolean hidden, Instant updatedAt) {
        updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null.");
        if (updatedAt.isBefore(this.updatedAt)) { // CANIDATE for refactoring
            throw new IllegalArgumentException("UpdatedAt cannot go backwards.");            
        }

        this.hidden = hidden;
        this.updatedAt = updatedAt; // NOTE: in realizty, everything we update something, we should be updating version, so could argue that 1 method to do this
        updateVersion();
        
    }


    private void updateVersion() {
        this.version++;
    }

}
