package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;

public record DeletePortfolioRequest(PortfolioId portfolioId, UserId userId, boolean confirmed, boolean softDelete) {
    
}
