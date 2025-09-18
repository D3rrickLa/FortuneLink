package com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(BigDecimal amount, Currency currency) {

    /**
     * REQUIREMENTS:
     *  add two amounts with same currency [X]
     *  reject two currencies  
     *  handle zero amounts -> does nothing [X]
     *  handle negative amounts  -> allows 
     *  validate null
     * 
     * 
     * @param other
     * @return new Money object with summed amount from this and other.amount
     */
    public Money add(Money other) {
        
        return new Money(this.amount.add(other.amount()), this.currency);
    }

    public Money subtract(Money other) {
        return null;
    }

    public Money multiply(BigDecimal multiplier) {
        return null;
    }

    public Money divide(BigDecimal divisor) {
        return null;
    }


}