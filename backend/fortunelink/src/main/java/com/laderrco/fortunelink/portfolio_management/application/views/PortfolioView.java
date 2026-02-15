package com.laderrco.fortunelink.portfolio_management.application.views;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.UserId;

import lombok.Builder;

@Builder
public record PortfolioView(
    PortfolioId portfolioId, 
    UserId userId, 
    String name, 
    String description,
    List<AccountView> accounts, 
    Money totalValue, 
    Instant createDate, 
    Instant lastUpdated) {}