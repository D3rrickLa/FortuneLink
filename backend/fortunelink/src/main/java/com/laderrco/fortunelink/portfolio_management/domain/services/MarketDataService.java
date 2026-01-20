package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.ErrorType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetQuote;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public interface MarketDataService {


    /**
     * Retrieves the most recent market price for a specific asset.
     * 
     * @param assetIdentifier
     * @return An {@link Optional} containing the {@link MarketAssetQuote} if found, otherwise, empty
     */
    Optional<MarketAssetQuote> getCurrentQuote(AssetIdentifier assetIdentifier);
    
    /**
     * Convience default method
     * 
     * @param assetIdentifier
     * @return
     */
    default Money getCurrentPrice(AssetIdentifier assetIdentifier) {
        return getCurrentQuote(assetIdentifier).map(MarketAssetQuote::currentPrice)
            .orElseThrow(() -> new MarketDataException("Price unavailable: " + assetIdentifier.toString(), ErrorType.DATA_UNAVAILABLE));
    }

    /**
     * Retrieves the price of an asset at a specific point in time.
     *
     * @param assetIdentifierId The unique identifier for the asset.
     * @param date              The specific date and time for the historical lookup.
     * @return The price of the asset at the requested time.
     */
    Optional<MarketAssetQuote> getHistoricalQuote(AssetIdentifier assetIdentifier, LocalDateTime date);
    
    /**
     * Efficiently fetches the current prices for a list of assets in a single batch.
     * Use this method instead of {@link #getCurrentPrice(AssetIdentifier)} when
     * updating portfolios to reduce network overhead.
     *
     * @param assetIdentifiers A list of asset identifiers to query.
     * @return A map where keys are the requested identifiers and values are their
     *         current prices.
     */
    Map<AssetIdentifier, MarketAssetQuote> getBatchQuotes(List<? extends AssetIdentifier> identifiers);
    
    
    /**
     * Fetches descriptive information for an asset based on its ticker symbol.
     *
     * @param symbol The unique identifier for the asset (e.g., Ticker + Exchange).
     * @return An {@link Optional} containing the {@link MarketAssetInfo} if found, otherwise an empty Optional.
     *         
     */
    public Optional<MarketAssetInfo> getAssetInfo(AssetIdentifier identifier);    

    /**
     * Batch fetch for multiple asset metadata objects.
     *
     * @param identifiers A list of tickers.
     * @return A map mapping the symbol string to its corresponding asset metadata.
     */
    public Map<AssetIdentifier, MarketAssetInfo> getBatchAssetInfo(List<? extends AssetIdentifier> identifiers);
    
    /**
     * Identifies the primary currency in which the asset is traded on its home
     * exchange.
     *
     * @param assetIdentifier The unique identifier for the asset.
     * @return The {@link ValidatedCurrency} used for transactions involving this
     *         asset.
     */
    public ValidatedCurrency getTradingCurrency(AssetIdentifier assetIdentifier);

    /**
     * Verifies if the underlying data provider supports and tracks the specified
     * symbol.
     *
     * @param symbol The identifier to check.
     * @return true if market data is available for this symbol; false otherwise.
     */
    boolean isSymbolSupported(AssetIdentifier symbol);

}
