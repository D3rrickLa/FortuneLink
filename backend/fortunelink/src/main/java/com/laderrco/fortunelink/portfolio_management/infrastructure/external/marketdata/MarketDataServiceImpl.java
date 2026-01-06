package com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;


public class MarketDataServiceImpl implements MarketDataService{

    @Override
    public Money getCurrentPrice(AssetIdentifier assetIdentifier) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCurrentPrice'");
    }

    @Override
    public Money getHistoricalPrice(AssetIdentifier assetIdentifierId, LocalDateTime date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHistoricalPrice'");
    }

    @Override
    public Map<AssetIdentifier, Money> getBatchPrices(List<AssetIdentifier> assetIdentifiers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBatchPrices'");
    }

    @Override
    public Optional<MarketAssetInfo> getAssetInfo(String symbol) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAssetInfo'");
    }

    @Override
    public Map<String, MarketAssetInfo> getAssetInfoBatch(List<String> symbols) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAssetInfoBatch'");
    }

    @Override
    public boolean isSymbolSupported(AssetIdentifier symbol) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isSymbolSupported'");
    }

    @Override
    public ValidatedCurrency getTradingCurrency(AssetIdentifier assetIdentifier) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getTradingCurrency'");
    }
    
}
