package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects;

import java.util.Map;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.AssetHolding;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Liability;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;

public record Snapshot(
    Money portfolioCashBalance,
    Map<LiabilityId, Liability> liabilities,
    Map<AssetHoldingId, AssetHolding> holdings,
    String reason
) {
    public Snapshot {
        portfolioCashBalance = Objects.requireNonNull(portfolioCashBalance, "Portfolio cash balance cannot be null");
        liabilities = Objects.requireNonNull(liabilities, "Liabilities map cannot be null");
        holdings = Objects.requireNonNull(holdings, "Holdings map cannot be null");
        reason = Objects.requireNonNull(reason, "Reason cannot be null");
    }
}
