package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;
import java.util.UUID;

public class VoidTransactionDetails extends TransactionDetails  {
    private final UUID originalTransactionId;
    private final String reason;
    public VoidTransactionDetails(UUID originalTransactionId, String reason) {
        Objects.requireNonNull(originalTransactionId, "Original Transaction ID cannot be null.");
        Objects.requireNonNull(originalTransactionId, "Reason for voiding cannot be null.");
        this.originalTransactionId = originalTransactionId;
        this.reason = reason;
    }
    
    public UUID getOriginalTransactionId() {
        return originalTransactionId;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((originalTransactionId == null) ? 0 : originalTransactionId.hashCode());
        result = prime * result + ((reason == null) ? 0 : reason.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VoidTransactionDetails other = (VoidTransactionDetails) obj;
        if (originalTransactionId == null) {
            if (other.originalTransactionId != null)
                return false;
        } else if (!originalTransactionId.equals(other.originalTransactionId))
            return false;
        if (reason == null) {
            if (other.reason != null)
                return false;
        } else if (!reason.equals(other.reason))
            return false;
        return true;
    }

    
}
