package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.ExchangeRateProvider;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.dtos.BocExchangeResponse;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
    if (from.equals(to)) {
      return ExchangeRate.identity(to, asOf);
    }

    BocExchangeResponse response;

    if (asOf == null || isToday(asOf)) {
      response = bocApiClient.getLatestExchangeRate(to.getCode(), from.getCode());
    } else {
      response = getHistoricalWithFallback(to.getCode(), from.getCode(), asOf);
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

  private BocExchangeResponse getHistoricalWithFallback(String from, String to, Instant asOf) {
    // Try up to 4 days back (covers long weekends)
    for (int daysBack = 0; daysBack <= 4; daysBack++) {
      Instant adjusted = asOf.minus(daysBack, ChronoUnit.DAYS);
      BocExchangeResponse response = bocApiClient.getHistoricalExchangeRate(to, from, adjusted,
          adjusted);
      if (!response.getObservations().isEmpty()) {
        return response;
      }
    }
    throw new ExchangeRateUnavailableException(from, to, asOf);
  }

  private boolean isToday(Instant instant) {
    return LocalDate.ofInstant(instant, ZoneId.systemDefault()).equals(LocalDate.now());
  }
}