package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.interfaces.TransactionDetails;

public final class ReversalTransactionDetails extends TransactionDetails {
    private final UUID originalTransactionId;
    private final TransactionType transactionType;
    private final String reasonForReversal;
    
    public ReversalTransactionDetails(UUID originalTransactionId, String reasonForReversal, TransactionType transactionType) {
        Objects.requireNonNull(originalTransactionId, "Original Transaction ID for reversal cannot be null.");
        Objects.requireNonNull(transactionType, "Transaction Type for reversal cannot be null.");
        Objects.requireNonNull(reasonForReversal, "Reason For Reversal cannot be null.");
        if (reasonForReversal.isBlank()) {
            throw new IllegalArgumentException("Reason for reversal cannot be blank.");
        }
        
        this.originalTransactionId = originalTransactionId;
        this.transactionType = transactionType;
        this.reasonForReversal = reasonForReversal;
    }
    
    public UUID getOriginalTransactionId() {
        return originalTransactionId;
    }
    public String getReasonForReversal() {
        return reasonForReversal;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        ReversalTransactionDetails that = (ReversalTransactionDetails) o;
        return Objects.equals(this.originalTransactionId, that.originalTransactionId) 
            && Objects.equals(this.transactionType, that.transactionType)
            && Objects.equals(this.reasonForReversal, that.reasonForReversal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.originalTransactionId, this.transactionType, this.reasonForReversal);
    }
}