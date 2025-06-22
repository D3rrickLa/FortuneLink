package com.laderrco.fortunelink.PortfolioManagement.domain.Services;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.PortfolioCurrency;

// impelement this the infra layer, to call actual 3rd party APIS to get said info
public interface IAssetPriceService {
    // Fetches the current market price for a given asset identifier in a specific
    // currency.
    // Returns Optional.empty() if price cannot be found.
    Optional<Money> getCurrentPrice(AssetIdentifier assetIdentifier, PortfolioCurrency targetCurrency);

    // Fetches current prices for multiple assets.
    Map<AssetIdentifier, Money> getCurrentPrices(Set<AssetIdentifier> assetIdentifiers,PortfolioCurrency targetCurrency);
}
