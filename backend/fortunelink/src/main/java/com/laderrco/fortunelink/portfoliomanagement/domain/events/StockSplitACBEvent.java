package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;

public class StockSplitACBEvent implements DomainEvent {
    private final PortfolioId portfolioId;
    private final AssetHoldingId assetHoldingId;
    private final BigDecimal splitRatio;
    private final Instant effectiveDate;

    public StockSplitACBEvent(
        PortfolioId portfolioId, 
        AssetHoldingId assetHoldingId, 
        BigDecimal splitRatio,
        Instant effectiveDate
    ) {
        this.portfolioId = portfolioId;
        this.assetHoldingId = assetHoldingId;
        this.splitRatio = splitRatio;
        this.effectiveDate = effectiveDate;
    }
    
    public PortfolioId getPortfolioId() { return portfolioId; }
    public AssetHoldingId getAssetId() { return assetHoldingId; }
    public BigDecimal getSplitRatio() { return splitRatio; }
    public Instant getEffectiveDate() { return effectiveDate; }

    @Override
    public Instant occuredAt() {
        return this.effectiveDate;
    }

}
