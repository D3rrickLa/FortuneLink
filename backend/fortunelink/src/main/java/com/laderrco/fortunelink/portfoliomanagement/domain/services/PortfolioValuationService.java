package com.laderrco.fortunelink.portfoliomanagement.domain.services;

import com.laderrco.fortunelink.portfoliomanagement.domain.model.entities.Account;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.entities.Asset;
import com.laderrco.fortunelink.portfoliomanagement.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class PortfolioValuationService {
    public Money calculateTotalValue(Portfolio portfolio, MarketDataService marketDataService) {return null;}
    public Money calculateAssetValue(Asset asset, MarketDataService marketDataService) {return null;}
    public Money calculateAccountValue(Account account, MarketDataService marketDataService) {return null;}
}
