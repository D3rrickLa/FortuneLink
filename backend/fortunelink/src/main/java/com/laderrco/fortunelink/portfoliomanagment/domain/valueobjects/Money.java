package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.math.BigDecimal;
import java.util.Currency;

public record Money(
    BigDecimal amount, 
    Currency currency
) {

    private void isSameCurrency(Currency other, String methodName) {
        if (!this.currency.equals(other)) {
            throw new IllegalArgumentException(String.format("CAnnot %s money with different currencies. Please convert them to be the same.", methodName));
        }
    }

    public Money add(Money other) {
        return null;
    }

    public Money subtract(Money other) {
        return null;
    }

    public Money mulitply (BigDecimal multiplier) {
        return null;
    }

    public Money divide(BigDecimal divisor){
        return null;
    }

    public Money negate() {
        return null;
    }

    public Money abs() {
        return null;
    }

    public boolean isZero() {
        return false;
    }

    public boolean isPositive() {
        return false;
    }

    public boolean isNegative(){ 
        return false;
    }

    public int compareTo(Money other) {
        return -1;
    }

    public Money convertTo(Currency targetCurrency, BigDecimal exchangeRate) {
        return null;
    }

    public static Money ZERO(Currency currency) {
        return null;
    }
}
