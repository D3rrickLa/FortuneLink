package com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects;

import java.util.Objects;


import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionSource;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionStatus;

public record TransactionMetadata(TransactionStatus transactionStatus, TransactionSource transactionSource, String transactionDescription) {
    public TransactionMetadata {
        Objects.requireNonNull(transactionStatus, "Transaction Status cannot be null.");
        Objects.requireNonNull(transactionSource, "Transaction Source cannot be null.");
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TransactionMetadata that = (TransactionMetadata) o;
        return Objects.equals(this.transactionStatus, that.transactionStatus)
                && Objects.equals(this.transactionSource, that.transactionSource)
                && Objects.equals(this.transactionDescription, that.transactionDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.transactionStatus, this.transactionSource, this.transactionDescription);
    }
}