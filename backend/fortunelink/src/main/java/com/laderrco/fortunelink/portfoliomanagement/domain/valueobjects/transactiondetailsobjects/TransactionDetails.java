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

    /**
     * Sum all fees in a givne taget ecurrency (portfolio currency)
     * @param targetCurrency is the currency we want to use 
     * @return Money.java where the value equals to the summed of all fees in portfolio currency
     */
    public Money getTotalFeesInCurrency(Currency targetCurrency) {
        return fees.stream()
            .map(fee -> fee.amount().getConversionAmount())
            .reduce(Money.ZERO(targetCurrency), Money::add);
    }

    public TransactionSource getSource() {return source;}
    public String getDescription() {return description;}
    public List<Fee> getFees() {return fees;}
}
