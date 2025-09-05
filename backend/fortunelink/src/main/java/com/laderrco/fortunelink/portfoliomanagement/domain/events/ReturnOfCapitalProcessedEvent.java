package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;

public class ReturnOfCapitalProcessedEvent implements DomainEvent {
    private final PortfolioId portfolioId;
    private final AssetHoldingId assetHoldingId;
    private final Money rocAmount;
    private final Money excessROC; // Amount that became capital gain
    private final Instant effectiveDate;

    public ReturnOfCapitalProcessedEvent(
        PortfolioId portfolioId, 
        AssetHoldingId assetHoldingId, 
        Money rocAmount,
        Money excessROC, 
        Instant effectiveDate
    ) {
        this.portfolioId = portfolioId;
        this.assetHoldingId = assetHoldingId;
        this.rocAmount = rocAmount;
        this.excessROC = excessROC;
        this.effectiveDate = effectiveDate;
    }
    
    public PortfolioId getPortfolioId() { return portfolioId; }
    public AssetHoldingId getAssetId() { return assetHoldingId; }
    public Money getRocAmount() { return rocAmount; }
    public Money getExcessROC() { return excessROC; }
    public Instant getEffectiveDate() { return effectiveDate; }

    @Override
    public Instant occuredAt() {
        return this.effectiveDate;
    }

}
