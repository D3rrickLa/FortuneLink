package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.util.Objects;
import java.util.UUID;

// voidingTransactionId = the new Id that would void the transaction assigned to this 'transaction prev'
public record VoidInfo(UUID voidingTransactionId) {
    public VoidInfo {
        Objects.requireNonNull(voidingTransactionId);
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
        return Objects.equals(this.voidingTransactionId, that.voidingTransactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.voidingTransactionId);
    }
}
