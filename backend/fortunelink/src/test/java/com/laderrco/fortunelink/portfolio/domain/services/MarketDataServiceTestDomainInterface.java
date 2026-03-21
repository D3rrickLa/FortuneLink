package com.laderrco.fortunelink.portfolio.domain.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketDataServiceTestDomainInterface {
  private final MarketDataService marketDataService = new MarketDataService() {

    @Override
    public Map<AssetSymbol, MarketAssetQuote> getBatchQuotes(Set<AssetSymbol> symbols) {
      AssetSymbol aapl = new AssetSymbol("AAPL");

      if (symbols.contains(new AssetSymbol("AAPL"))) {
        MarketAssetQuote quote = new MarketAssetQuote(aapl, new Price(Money.of(123, "USD")), null,
            null, null, null, null, null, null, null, null, null);

        return Map.of(aapl, quote);

      }

      return Map.of();
    }

    @Override
    public Optional<MarketAssetQuote> getHistoricalQuote(AssetSymbol symbol, Instant date) {
      return Optional.empty();
    }

    @Override
    public Optional<MarketAssetInfo> getAssetInfo(AssetSymbol symbol) {
      AssetSymbol aapl = new AssetSymbol("AAPL");

      if (aapl.equals(symbol)) {
        MarketAssetInfo marketAssetInfo = new MarketAssetInfo(aapl, "APPLE", AssetType.STOCK,
            "NASDAQ", Currency.USD, "Technology", "Apple is an apple company");

        return Optional.of(marketAssetInfo);

      }

      return Optional.empty();
    }

    @Override
    public Map<AssetSymbol, MarketAssetInfo> getBatchAssetInfo(Set<AssetSymbol> symbols) {
      return Map.of();
    }

    @Override
    public Currency getTradingCurrency(AssetSymbol symbol) {
      return null;
    }

    @Override
    public boolean isSymbolSupported(AssetSymbol symbol) {
      return false;
    }
  };

  @Test
  void testGetCurrentPrice_success_getsMockPrice() {
    Map<AssetSymbol, MarketAssetQuote> currentquotes = marketDataService.getBatchQuotes(
        Set.of(new AssetSymbol("AAPL")));

    Price currentPrice = currentquotes.get(new AssetSymbol("AAPL")).currentPrice();
    assertEquals(new Price(Money.of(123, "USD")), currentPrice);
  }

  @Test
  void testGetCurrentPrice_failure_returnsEmpty() {
    Map<AssetSymbol, MarketAssetQuote> currentquotes = marketDataService.getBatchQuotes(
        Set.of(new AssetSymbol("MSFT")));

    assertThat(currentquotes.get(new AssetSymbol("MSFT"))).isEqualTo(null);
  }

  @Test
  void testGetAssetInfo_success_informationIsPresent() {
    Optional<MarketAssetInfo> info = marketDataService.getAssetInfo(new AssetSymbol("AAPL"));
    assertTrue(info.isPresent());
  }
}