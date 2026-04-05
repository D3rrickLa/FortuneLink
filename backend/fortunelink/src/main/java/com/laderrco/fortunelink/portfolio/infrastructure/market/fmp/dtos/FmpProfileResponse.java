package com.laderrco.fortunelink.portfolio.infrastructure.market.fmp.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Data;

/**
 * FMP API response for company profile endpoint.
 * <p>
 * Endpoint: GET /profile/{symbol} Example:
 * https://financialmodelingprep.com/api/v3/profile/AAPL?apikey=xxx
 * <p>
 * Sample Response: [ { "symbol": "AAPL", "price": 150.25, "beta": 1.2, "volAvg": 75000000,
 * "mktCap": 2500000000000, "lastDiv": 0.92, "range": "120.00-180.00", "changes": 2.25,
 * "companyName": "Apple Inc.", "currency": "USD", "cik": "0000320193", "isin": "US0378331005",
 * "cusip": "037833100", "exchange": "NASDAQ Global Select", "exchangeShortName": "NASDAQ",
 * "industry": "Consumer Electronics", "website": "https://www.apple.com", "description": "Apple
 * Inc. designs, manufactures, and markets smartphones...", "ceo": "Timothy Cook", "sector":
 * "Technology", "country": "US", "fullTimeEmployees": "164000", "phone": "14089961010", "address":
 * "One Apple Park Way", "city": "Cupertino", "state": "CA", "zip": "95014", "dcfDiff": 5.25, "dcf":
 * 145.00, "image": "https://financialmodelingprep.com/image-stock/AAPL.png", "ipoDate":
 * "1980-12-12", "defaultImage": false, "isEtf": false, "isActivelyTrading": true, "isAdr": false,
 * "isFund": false } ]
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FmpProfileResponse {
  @JsonProperty("symbol")
  private String symbol;

  @JsonProperty("price")
  private BigDecimal price;

  @JsonProperty("marketCap")
  private Long marketCap;

  @JsonProperty("beta")
  private BigDecimal beta;

  @JsonProperty("lastDividend")
  private BigDecimal lastDiv;

  @JsonProperty("range")
  private String range;

  @JsonProperty("change")
  private BigDecimal change;

  @JsonProperty("cnagePercentage")
  private BigDecimal changePercentage;

  @JsonProperty("volume")
  private Long volume;

  @JsonProperty("averageVolume")
  private Long avgVol;

  @JsonProperty("companyName")
  private String companyName;

  @JsonProperty("currency")
  private String currency;

  @JsonProperty("cik")
  private String cik;

  @JsonProperty("isin")
  private String isin;

  @JsonProperty("cusip")
  private String cusip;

  @JsonProperty("exchangeFullName")
  private String exchangeFullName;

  @JsonProperty("exchange")
  private String exchange;

  @JsonProperty("industry")
  private String industry;

  @JsonProperty("website")
  private String website;

  @JsonProperty("description")
  private String description;

  @JsonProperty("ceo")
  private String ceo;

  @JsonProperty("sector")
  private String sector;

  @JsonProperty("country")
  private String country;

  @JsonProperty("fullTimeEmployees")
  private String fullTimeEmployees;

  @JsonProperty("phone")
  private String phone;

  @JsonProperty("address")
  private String address;

  @JsonProperty("city")
  private String city;

  @JsonProperty("state")
  private String state;

  @JsonProperty("zip")
  private String zip;

  @JsonProperty("image")
  private String image;

  @JsonProperty("ipoDate")
  private String ipoDate;

  @JsonProperty("defaultImage")
  private Boolean defaultImage;

  @JsonProperty("isEtf")
  private Boolean isEtf;

  @JsonProperty("isActivelyTrading")
  private Boolean isActivelyTrading;

  @JsonProperty("isAdr")
  private Boolean isAdr;

  @JsonProperty("isFund")
  private Boolean isFund;
}