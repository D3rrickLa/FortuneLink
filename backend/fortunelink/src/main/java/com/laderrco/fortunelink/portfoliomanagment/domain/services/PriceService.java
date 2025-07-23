package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.shared.valueobjects.Money;

// for fetching current market price of a stock/other asset
public interface PriceService {

    public Money getCurrentAssetPrice(AssetIdentifier assetIdentifier, Instant time);
    
}