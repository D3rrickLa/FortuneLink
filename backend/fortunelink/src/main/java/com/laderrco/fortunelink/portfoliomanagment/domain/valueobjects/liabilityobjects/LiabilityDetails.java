package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;

public record LiabilityDetails(
    String name,
    String description,
    Percentage annualInterestRate,
    Instant maturityDate
) {
    
}
