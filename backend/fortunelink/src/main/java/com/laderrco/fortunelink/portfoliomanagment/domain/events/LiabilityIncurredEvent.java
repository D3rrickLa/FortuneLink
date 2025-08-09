package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;

public record LiabilityIncurredEvent(
    LiabilityId liabilityId,
    Percentage interestRate,
    Money amount
) {

}
