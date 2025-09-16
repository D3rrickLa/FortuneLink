package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfoliomanagement.domain.events.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.AssetHoldingId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.UserId;

// we are not storing the transaction here, only maintaining portfolio state
public class Portfolio {
    private final PortfolioId portfolioId;
    private final UserId userId;
    private String name;
    private String description;
    private Money cashBalance;
    private Map<AssetHoldingId, AssetHolding> holdings;
    private int version;
    private final Instant createdAt;
    private Instant lastModifiedAt;

    private final List<DomainEvent> domainEvents;

    


    public Portfolio(PortfolioId portfolioId, UserId userId, String name, String description, Money cashBalance,
            Map<AssetHoldingId, AssetHolding> holdings, int version, Instant createdAt, Instant lastModifiedAt,
            List<DomainEvent> domainEvents) {
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.cashBalance = cashBalance;
        this.holdings = holdings;
        this.version = version;
        this.createdAt = createdAt;
        this.lastModifiedAt = lastModifiedAt;
        this.domainEvents = domainEvents;
    }

    public TransactionId buySecurity() {
        return null;
    }

    public TransactionId sellSecurity() {
        return null;
    }

    public TransactionId depositCash() {
        return null;
    }

    public TransactionId withdrawCash() {
        return null;
    }

    public TransactionId recordDividend() {
        return null;
    }

    public void updateDetails() {
        
    }

    public PortfolioId getPortfolioId() {
        return portfolioId;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Money getCashBalance() {
        return cashBalance;
    }

    public Map<AssetHoldingId, AssetHolding> getHoldings() {
        return holdings;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public List<DomainEvent> getDomainEvents() {
        return domainEvents;
    }

    


}
