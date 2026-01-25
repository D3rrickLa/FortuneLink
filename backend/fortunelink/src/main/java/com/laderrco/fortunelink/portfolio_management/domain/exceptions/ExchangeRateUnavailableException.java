package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

import java.time.Instant;

public class ExchangeRateUnavailableException extends RuntimeException {
    public ExchangeRateUnavailableException(String code, String code2, Instant asOf) {
        super(String.format("%s to %s on %s does not exist", code, code2, asOf));
    }

    public ExchangeRateUnavailableException(String string) {
        super(string);
    }
}
