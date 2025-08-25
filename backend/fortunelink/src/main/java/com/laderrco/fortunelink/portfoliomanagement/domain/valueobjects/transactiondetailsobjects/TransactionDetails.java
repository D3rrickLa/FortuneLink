package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;

public abstract class TransactionDetails {
    private final TransactionSource source;
    private final String description;
    private final List<Fee> fees;

    protected TransactionDetails(
        TransactionSource source,
        String description,
        List<Fee> fees
    ) {
       this.source = Objects.requireNonNull(source, "Source cannot be null.");; 
       this.description = description.trim();
       this.fees = fees != null ? Collections.unmodifiableList(fees) : Collections.emptyList();
    }

    // Safe to keep here because *all* transactions may have fees
    public Money getTotalFeesInPortfolioCurrency(Currency portfolioCurrency) {
        return fees.stream()
            .map(fee -> fee.amount().getPortfolioAmount())
            .reduce(Money.ZERO(portfolioCurrency), Money::add);
    }

    public TransactionSource getSource() {return source;}
    public String getDescription() {return description;}
    public List<Fee> getFees() {return fees;}
}
