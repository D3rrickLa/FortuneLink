package com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.PortfolioCurrency;

// infra layer
public interface IHistoricalAssetPriceService {
    // Fetches the historical market price for a given asset identifier on a
    // specific date.
    Optional<Money> getHistoricalPrice(AssetIdentifier assetIdentifier, LocalDate date,
            PortfolioCurrency targetCurrency);

    // Fetches historical prices for multiple assets on a specific date.
    Map<AssetIdentifier, Money> getHistoricalPrices(Set<AssetIdentifier> assetIdentifiers, LocalDate date,
            PortfolioCurrency targetCurrency);

    // Fetches a series of historical prices for an asset over a date range.
    Map<LocalDate, Money> getPriceSeries(AssetIdentifier assetIdentifier, LocalDate startDate, LocalDate endDate,
            PortfolioCurrency targetCurrency);

}
