package com.laderrco.fortunelink.portfolio.application.utils;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountViewBuilder {
    private final PortfolioValuationService portfolioValuationService;
    private final PortfolioViewMapper portfolioViewMapper;

    public AccountView build(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        List<PositionView> positionViews = account.getPositionEntries().stream()
                .map(entry -> portfolioViewMapper.toPositionView(entry.getValue(), quoteCache.get(entry.getKey())))
                .toList();

        Money totalValue = portfolioValuationService.calculateAccountValue(account, quoteCache);
        Money cashBalance = account.getCashBalance();

        return portfolioViewMapper.toAccountView(account, positionViews, totalValue, cashBalance);
    }
}