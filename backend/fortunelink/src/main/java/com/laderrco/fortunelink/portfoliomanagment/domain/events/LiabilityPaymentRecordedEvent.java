package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;

public record LiabilityPaymentRecordedEvent(
    LiabilityId liabilityId,
    Money principalPaid,
    Money interestPaid,
    Instant occurredOn
) {
    
}
