package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;

public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(String s) {
        super(s);
    }
    public CurrencyMismatchException(Currency a, Currency b) {
        super("Currency mismatch: " + a + " vs " + b);
    }
}
