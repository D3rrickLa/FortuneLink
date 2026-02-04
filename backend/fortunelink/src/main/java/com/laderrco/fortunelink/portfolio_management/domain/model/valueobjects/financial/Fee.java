package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial;

import java.math.RoundingMode;
import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Precision;
import com.laderrco.fortunelink.portfolio_management.shared.enums.Rounding;

public record Fee(
    FeeType feeType, 
    Money nativeAmount,
    Money accountAmount,
    ExchangeRate exchangeRate,
    Instant occurredAt,
    FeeMetadata metadata
) implements ClassValidation {
    private static int FEE_PRECISION = Precision.getMoneyPrecision();
    private static RoundingMode F_ROUNDING_MODE = Rounding.MONEY.getMode();

        public Fee {
        ClassValidation.validateParameter(feeType, "Fee type cannot be null");
        ClassValidation.validateParameter(nativeAmount, "Native amount cannot be null");
        ClassValidation.validateParameter(occurredAt, "Occurred at cannot be null");
        ClassValidation.validateParameter(metadata, "Metadata at cannot be null");

        if (nativeAmount.isNegative()) {
            throw new IllegalArgumentException("Fee amount cannot be negative");
        }

    }























    public record FeeMetadata() {
    }
}