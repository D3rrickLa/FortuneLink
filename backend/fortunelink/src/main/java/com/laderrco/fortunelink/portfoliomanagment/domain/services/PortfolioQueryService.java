package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Transaction;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;

// dedicated read-side service, methods are all about fetching data and calculating metrics for reports 
public interface PortfolioQueryService {
    public Money getNetWorth(PortfolioId portfolioId);
    public Money getTotalAssetValue(PortfolioId portfolioId);
    public Money getTotalLiabilityValue(PortfolioId portfolioId);
    public Map<AssetType, BigDecimal> getAssetAllocation(PortfolioId portfolioId); // type of financial instrument - stock, etf, etc.
    public List<Transaction> getTransactionHistory(PortfolioId portfolioId);

}
