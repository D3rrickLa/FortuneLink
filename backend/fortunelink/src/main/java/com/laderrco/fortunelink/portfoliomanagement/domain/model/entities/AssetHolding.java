package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.Quantity;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.shared.domain.valueobjects.Money;

public class AssetHolding {
    private final AssetHoldingId assetId;
    private final AssetType assetType;
    private final AssetIdentifier assetIdentifier;
    private Quantity quantity;
    private Money costBasis;

    private Instant updatedAt;
    private int version;

    public void increasePosition(){}
    public void decreasePosition(){}
    public void recordDividend(){}
    public void processDividendReinvestment(){}
    public void processReturnOfCaptial(){}
    public void processStockSplitACB(){}
    public void recordEligibleDividend(){}

    // query methods
    public void getCurrentMarketValue(){}
    public void getUnrealizedGainLoss(){}
    public void getUnrealizedGainLossPercentage(){}
    public void calculateCaptialGainLoss(){}
}
