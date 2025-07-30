package com.laderrco.fortunelink.portfoliomanagment.infrastructure.services;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.PriceService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

@Service
public class StockApiService implements PriceService {

    @Override
    @Deprecated
    public Money getCurrentAssetPrice(AssetIdentifier assetIdentifier, Instant time) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCurrentAssetPrice'");
    }

    @Override
    public Map<AssetIdentifier, MarketPrice> getCurrentPrices(Set<AssetIdentifier> assetIdentifiers) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCurrentPrices'");
    }

    @Override
    public Map<AssetIdentifier, MarketPrice> getHistoricalPrices(Set<AssetIdentifier> assetIdentifiers, Instant time) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHistoricalPrices'");
    }

    @Override
    public MarketPrice getCurrentPrice(AssetIdentifier assetIdentifier) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCurrentPrice'");
    }
    
}
