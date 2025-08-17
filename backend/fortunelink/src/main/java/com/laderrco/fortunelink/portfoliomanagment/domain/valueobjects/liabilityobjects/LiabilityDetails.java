package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.LiabilityType;

public record LiabilityDetails(
    String name,
    String description,
    LiabilityType liabilityType,
    Percentage annualInterestRate,
    Instant maturityDate
) {
    public LiabilityDetails {
        Objects.requireNonNull(name, "Name cannot be null.");
    }
}
