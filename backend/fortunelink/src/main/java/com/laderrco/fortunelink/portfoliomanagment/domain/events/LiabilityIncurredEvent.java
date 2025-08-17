package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;

public record LiabilityIncurredEvent(
    LiabilityId liabilityId,
    Percentage interestRate,
    Money amount,
    Instant date
) {
    public LiabilityIncurredEvent {
        Objects.requireNonNull(liabilityId, "Liability id cannot be null.");
        Objects.requireNonNull(interestRate, "Interest rate cannot be null.");
        Objects.requireNonNull(amount, "Amount cannot be null.");
        Objects.requireNonNull(date, "Date of liability incurrence cannot be null.");
    }
}
