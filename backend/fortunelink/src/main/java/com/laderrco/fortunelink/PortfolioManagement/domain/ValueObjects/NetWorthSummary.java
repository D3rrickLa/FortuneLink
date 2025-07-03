package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects;

import java.math.RoundingMode;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.enums.DecimalPrecision;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public record NetWorthSummary(Money totalAssetsValue, Money totalLiabilitiesValue) {
    public NetWorthSummary {
        Objects.requireNonNull(totalAssetsValue, "totalAssetsValue cannot be null.");
        Objects.requireNonNull(totalLiabilitiesValue, "totalLiabilitiesValue cannot be null.");
    }

    public static Money calculateNetWorth(Money rawTotalAssetValue, Money rawTotalLiabilitiesValue) {
        NetWorthSummary netWorthSummary = new NetWorthSummary(rawTotalAssetValue, rawTotalLiabilitiesValue);
        Money netWorth = netWorthSummary.totalAssetsValue().subtract(netWorthSummary.totalLiabilitiesValue());
        return new Money(netWorth.amount().setScale(DecimalPrecision.CASH.getDecimalPlaces(), RoundingMode.HALF_EVEN), netWorth.currency());
    }
}