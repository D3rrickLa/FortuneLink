package com.laderrco.fortunelink.portfolio.domain.exceptions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(Currency a, Currency b) {
        super("Currency mismatch: " + a + " vs " + b);
    }
    public CurrencyMismatchException(Currency a, Currency b, String s) {
        super("Currency mismatch: " + a + " vs " + b + " on the method: " + s);
    }
}
