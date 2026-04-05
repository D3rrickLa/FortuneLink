package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.ExchangeRateProvider;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.dtos.BocExchangeResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class BocProvider implements ExchangeRateProvider {
  private final BocApiClient bocApiClient;
  private final BocResponseMapper mapper;

  @Override
  public ExchangeRate getExchangeRate(Currency from, Currency to, Instant asOf) {
    // 1. Identity check (Must return!)
    if (from.equals(to)) {
      return ExchangeRate.identity(to, asOf);
    }

    BocExchangeResponse response;

    // 2. Determine if we need Latest or Historical
    if (asOf == null || isToday(asOf)) {
      response = bocApiClient.getLatestExchangeRate(to.getCode(), from.getCode());
    } else {
      // BOC expects a range. Usually, for a specific point in time,
      // we request from that date to that date.
      response = bocApiClient.getHistoricalExchangeRate(to.getCode(), from.getCode(), asOf, asOf);
    }

    // 3. Map the complex BOC JSON structure to your Domain objects
    List<ExchangeRate> rates = mapper.toExchangeRates(response, from.getCode(), to.getCode());

    if (rates.isEmpty()) {
      log.warn("No exchange rate found for {}/{} at {}", from.getCode(), to.getCode(), asOf);
      throw new ExchangeRateUnavailableException(from.getCode(), to.getCode(), asOf);
    }

    // 4. Return the most relevant rate (BOC usually returns list sorted by date)
    return rates.get(rates.size() - 1);
  }

  private boolean isToday(Instant instant) {
    return LocalDate.ofInstant(instant, ZoneId.systemDefault()).equals(LocalDate.now());
  }
}