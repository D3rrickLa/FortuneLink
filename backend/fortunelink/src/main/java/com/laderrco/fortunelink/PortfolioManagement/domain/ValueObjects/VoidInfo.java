package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;
import java.util.UUID;

public record VoidInfo(UUID voidingTransactionId, String voidReason) {
    public VoidInfo {
        Objects.requireNonNull(voidingTransactionId);
        Objects.requireNonNull(voidReason);

        if (voidReason.trim().isBlank()) {
            throw new IllegalArgumentException("A reason to void must be given.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }

        VoidInfo that = (VoidInfo) o;
        return Objects.equals(this.voidingTransactionId, that.voidingTransactionId) && Objects.equals(this.voidReason, that.voidReason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.voidingTransactionId, this.voidReason);
    }
}
