package com.laderrco.fortunelink.portfoliomanagement.domain.events;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;

public class DividendReinvestedEvent implements DomainEvent {
    private final PortfolioId portfolioId;
    private final AssetHoldingId assetHoldingId;
    private final Money dividendAmount;
    private final BigDecimal sharesReceived;
    private final Money pricePerShare;
    private final Instant reinvestmentDate;

    public DividendReinvestedEvent(
        PortfolioId portfolioId, 
        AssetHoldingId assetHoldingId, 
        Money dividendAmount,
        BigDecimal sharesReceived, 
        Money pricePerShare, 
        Instant reinvestmentDate
    ) {
        
        this.portfolioId = portfolioId;
        this.assetHoldingId = assetHoldingId;
        this.dividendAmount = dividendAmount;
        this.sharesReceived = sharesReceived;
        this.pricePerShare = pricePerShare;
        this.reinvestmentDate = reinvestmentDate;
    }
    public PortfolioId getPortfolioId() { return portfolioId; }
    public AssetHoldingId getAssetId() { return assetHoldingId; }
    public Money getDividendAmount() { return dividendAmount; }
    public BigDecimal getSharesReceived() { return sharesReceived; }
    public Money getPricePerShare() { return pricePerShare; }
    public Instant getReinvestmentDate() { return reinvestmentDate; }

    @Override
    public Instant occuredAt() {
        return this.reinvestmentDate;
    }

}
