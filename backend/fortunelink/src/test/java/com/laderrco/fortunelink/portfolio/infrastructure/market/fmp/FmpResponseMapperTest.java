package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio.api.web.dto.SymbolSearchResult;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpProfileResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpQuoteResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos.FmpSearchResponse;

@DisplayName("FMP Response Mapper Tests")
class FmpResponseMapperTest {

  private final FmpResponseMapper mapper = new FmpResponseMapper();
  private final Currency usd = Currency.of("USD");

  @Nested
  @DisplayName("Quote Mapping (toQuote)")
  class QuoteMappingTests {

    @Test
    @DisplayName("should map full FMP quote response correctly")
    void shouldMapFullQuote() {
      
      FmpQuoteResponse fmp = new FmpQuoteResponse();
      fmp.setSymbol("AAPL");
      fmp.setPrice(new BigDecimal("150.00"));
      fmp.setChangePercentage(new BigDecimal("2.5")); 
      fmp.setTimestamp(1704067200L); 

      
      MarketAssetQuote result = mapper.toQuote(fmp, usd);

      
      assertThat(result.symbol().symbol()).isEqualTo("AAPL");
      assertThat(result.currentPrice().amount()).isEqualByComparingTo("150.00");
      assertThat(result.currentPrice().currency()).isEqualTo(usd);
      
      assertThat(result.changePercent()).isEqualByComparingTo(new PercentageChange(BigDecimal.valueOf(0.025)));
      assertThat(result.timestamp()).isEqualTo(Instant.ofEpochSecond(1704067200L));
    }
  }

  @Nested
  @DisplayName("Asset Type Resolution (mapToAssetType)")
  class AssetTypeTests {

    @Test
    @DisplayName("should prioritize ETF and Fund flags")
    void shouldMapFlags() {
      FmpProfileResponse etf = new FmpProfileResponse();
      etf.setSymbol("VTI"); 
      etf.setCurrency("USD"); 
      etf.setIsEtf(true);

      assertThat(mapper.toAssetInfo(etf).type()).isEqualTo(AssetType.ETF);

      FmpProfileResponse fund = new FmpProfileResponse();
      fund.setSymbol("VFIAX"); 
      fund.setCurrency("USD"); 
      fund.setIsFund(true);

      assertThat(mapper.toAssetInfo(fund).type()).isEqualTo(AssetType.OTHER);
    }

    @Test
    @DisplayName("should map via Exchange name when flags are false")
    void shouldMapViaExchange() {
      FmpProfileResponse crypto = new FmpProfileResponse();
      crypto.setSymbol("BTCUSD"); 
      crypto.setCurrency("USD"); 
      crypto.setExchange("Crypto");

      assertThat(mapper.toAssetInfo(crypto).type()).isEqualTo(AssetType.CRYPTO);

      FmpProfileResponse forex = new FmpProfileResponse();
      forex.setSymbol("EURUSD"); 
      forex.setCurrency("USD"); 
      forex.setExchange("FOREX");
      assertThat(mapper.toAssetInfo(forex).type()).isEqualTo(AssetType.FOREX_PAIR);
      forex.setExchange("CURRENCY");
      assertThat(mapper.toAssetInfo(forex).type()).isEqualTo(AssetType.FOREX_PAIR);

    }

    @Test
    @DisplayName("should default to STOCK")
    void shouldDefaultToStock() {
      FmpProfileResponse stock = new FmpProfileResponse();
      stock.setSymbol("MSFT"); 
      stock.setCurrency("USD"); 
      

      assertThat(mapper.toAssetInfo(stock).type()).isEqualTo(AssetType.STOCK);
    }

    @Test
    @DisplayName("should return null when method null")
    void returnNull() {
      assertThat(mapper.toAssetInfo(null)).isNull();
    }
  }

  @Test
  @DisplayName("should handle null response and null numeric fields")
  void shouldHandleNulls() {
    assertThat(mapper.toQuote(null, usd)).isNull();

    FmpQuoteResponse fmp = new FmpQuoteResponse();
    fmp.setSymbol("NULL-TEST"); 

    MarketAssetQuote result = mapper.toQuote(fmp, usd);

    assertThat(result.currentPrice().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.changeAmount()).isEqualTo(BigDecimal.ZERO);
  }

  @Nested
  @DisplayName("Search Mapping (toSearchResult)")
  class SearchMappingTests {

    @Test
    @DisplayName("should map search response correctly")
    void shouldMapSearch() {
      FmpSearchResponse fmp = new FmpSearchResponse();
      fmp.setSymbol("MSFT");
      fmp.setCurrency("USD");
      fmp.setName("Microsoft");

      SymbolSearchResult result = mapper.toSearchResult(fmp);

      assertThat(result.symbol().symbol()).isEqualTo("MSFT");
      assertThat(result.tradingCurrency()).isEqualTo(usd);
      assertThat(mapper.toSearchResult(null)).isNull();
    }
  }
}