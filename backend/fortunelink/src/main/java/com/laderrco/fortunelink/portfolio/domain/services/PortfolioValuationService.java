package com.laderrco.fortunelink.portfolio.domain.services;

import java.util.Map;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public interface PortfolioValuationService {
    Money calculateTotalValue(Portfolio portfolio, Currency targetCurrency,  Map<AssetSymbol, MarketAssetQuote> quoteCache);
    Money calculateAccountValue(Account account);
    Money calculatePositionsValue(Account account);
}