package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;

public class EligibleDividendReceivedEvent implements DomainEvent {
    private final PortfolioId portfolioId;
    private final AssetHoldingId assetHoldingId;
    private final Money dividendAmount;
    private final Money grossUpAmount;
    private final Instant receivedAt;

    public EligibleDividendReceivedEvent(
        PortfolioId portfolioId, 
        AssetHoldingId assetHoldingId, 
        Money dividendAmount,
        Money grossUpAmount, 
        Instant receivedAt) 
    {
        this.portfolioId = portfolioId;
        this.assetHoldingId = assetHoldingId;
        this.dividendAmount = dividendAmount;
        this.grossUpAmount = grossUpAmount;
        this.receivedAt = receivedAt;
    }

    public PortfolioId getPortfolioId() { return portfolioId; }
    public AssetHoldingId getAssetId() { return assetHoldingId; }
    public Money getDividendAmount() { return dividendAmount; }
    public Money getGrossUpAmount() { return grossUpAmount; }
    public Instant getReceivedAt() { return receivedAt; }

    @Override
    public Instant occuredAt() {
        return this.receivedAt;
    }

}
