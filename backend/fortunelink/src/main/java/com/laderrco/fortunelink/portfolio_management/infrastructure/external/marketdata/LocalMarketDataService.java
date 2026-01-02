package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

@Service
public class LocalMarketDataService implements MarketDataService{

    @Override
    public Money getCurrentPrice(AssetIdentifier assetIdentifierId) {
        return Money.of(808, "USD");
    }

    @Override
    public Money getHistoricalPrice(AssetIdentifier assetIdentifierId, LocalDateTime time) {
        return Money.of(808, "USD");
    }

    @Override
    public Map<AssetIdentifier, Money> getBatchPrices(List<AssetIdentifier> assetIdentifierIds) {
        return Map.of();
    }

    @Override
    public Optional<MarketAssetInfo> getAssetInfo(String symbol) {
        return Optional.ofNullable(new MarketAssetInfo(symbol, symbol, AssetType.OTHER, symbol, ValidatedCurrency.JPY, symbol, Money.of(1001, "JPY")));
    }

    @Override
    public Map<String, MarketAssetInfo> getAssetInfoBatch(List<String> symbols) {
        return Map.of();
    }

    @Override
    public ValidatedCurrency getTradingCurrency(AssetIdentifier assetIdentifier) {
        return ValidatedCurrency.JPY;
    }
    
}
