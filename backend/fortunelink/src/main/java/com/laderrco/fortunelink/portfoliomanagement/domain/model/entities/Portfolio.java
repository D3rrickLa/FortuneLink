package com.laderrco.fortunelink.portfoliomanagement.domain.model.entities;

import java.time.LocalDateTime;
import java.util.List;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.valueobjects.ids.UserId;

public class Portfolio {
    private final PortfolioId portfolioId;
    private final UserId userId;
    private List<Account> accoutns;
    
    private final LocalDateTime systemCreatedDate;
    private LocalDateTime lastUpdated;
    public Portfolio(PortfolioId portfolioId, UserId userId, List<Account> accoutns, LocalDateTime systemCreatedDate,
            LocalDateTime lastUpdated) {
        this.portfolioId = portfolioId;
        this.userId = userId;
        this.accoutns = accoutns;
        this.systemCreatedDate = systemCreatedDate;
        this.lastUpdated = lastUpdated;
    } 

    
}
