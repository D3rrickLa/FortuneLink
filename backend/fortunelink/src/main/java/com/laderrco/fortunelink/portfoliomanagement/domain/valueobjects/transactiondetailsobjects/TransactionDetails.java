package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.CurrencyMismatchException;
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
     * Sum all fees in a given taget cuurrency (portfolio currency)
     * @param targetCurrency is the currency we want to use, most likely our portoflio currency 
     * @return Money.java where the value equals to the summed of all fees in portfolio currency
     */
    public Money getTotalFeesInCurrency(Currency targetCurrency) {
        return this.fees.stream()
            .map(fee -> {
                Money converted = fee.amount().getConversionAmount();
                if (!converted.currency().equals(targetCurrency)) {
                    throw new CurrencyMismatchException(String.format("Fee conversion currency %s does not match taget currency %s", converted.currency(), targetCurrency));
                }
                return converted;
            })
            .reduce(Money.ZERO(targetCurrency), Money::add); // handles empty fees automatically
    }

    public TransactionSource getSource() {return source;}
    public String getDescription() {return description;}
    public List<Fee> getFees() {return fees;}
}
