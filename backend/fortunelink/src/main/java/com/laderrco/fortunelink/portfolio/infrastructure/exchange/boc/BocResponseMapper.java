package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.dtos.BocExchangeResponse;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class BocResponseMapper {
  private static final Currency CAD = Currency.CAD;

  public List<ExchangeRate> toExchangeRates(BocExchangeResponse response) {
    List<ExchangeRate> result = new ArrayList<>();

    if (response == null || response.getObservations() == null) {
      return result;
    }

    for (BocExchangeResponse.Observation obs : response.getObservations()) {
      // BOC dates are "YYYY-MM-DD", Instant.parse needs a time.
      // We append T00:00:00Z or use LocalDate.
      Instant date = LocalDate.parse(obs.getDate()).atStartOfDay(ZoneOffset.UTC).toInstant();

      if (obs.getRates() == null) {
        continue;
      }

      for (Map.Entry<String, BocExchangeResponse.Observation.Rate> entry : obs.getRates()
          .entrySet()) {
        String key = entry.getKey(); // e.g., "FXUSDCAD"

        if (!key.startsWith("FX") || key.length() != 8) {
          continue;
        }

        Currency base = Currency.of(key.substring(2, 5));
        Currency target = Currency.of(key.substring(5, 8));
        BigDecimal rateValue = entry.getValue().getValue();

        if (rateValue == null) {
          continue;
        }

        result.add(new ExchangeRate(base, target, rateValue, date));
      }
    }
    return result;
  }

  public List<ExchangeRate> toExchangeRates(BocExchangeResponse response, String base,
      String target) {
    Currency baseCurrency = Currency.of(base.toUpperCase());
    Currency targetCurrency = Currency.of(target.toUpperCase());

    if (baseCurrency.equals(targetCurrency)) {
      return List.of();
    }

    List<ExchangeRate> allRates = toExchangeRates(response);
    Map<Instant, List<ExchangeRate>> byDate = allRates.stream()
        .collect(Collectors.groupingBy(ExchangeRate::quotedAt));

    List<ExchangeRate> result = new ArrayList<>();

    for (Map.Entry<Instant, List<ExchangeRate>> entry : byDate.entrySet()) {
      Instant date = entry.getKey();
      List<ExchangeRate> rates = entry.getValue();

      // Use the smart finder that can invert CAD rates
      ExchangeRate baseToTarget = findRateSmart(rates, baseCurrency, targetCurrency);

      if (baseToTarget != null) {
        result.add(baseToTarget);
      } else {
        // Cross-currency logic: e.g., EUR -> USD via CAD
        ExchangeRate baseToCad = findRateSmart(rates, baseCurrency, CAD);
        ExchangeRate cadToTarget = findRateSmart(rates, CAD, targetCurrency);

        if (baseToCad != null && cadToTarget != null) {
          BigDecimal crossRate = baseToCad.rate().multiply(cadToTarget.rate());
          result.add(new ExchangeRate(baseCurrency, targetCurrency, crossRate, date));
        }
      }
    }
    return result;
  }

  /**
   * Smart finder: If looking for CAD -> USD but only USD -> CAD exists, it returns the inverted
   * rate (1/rate).
   */
  private ExchangeRate findRateSmart(List<ExchangeRate> rates, Currency from, Currency to) {
    // 1. Try direct match
    Optional<ExchangeRate> direct = rates.stream()
        .filter(r -> r.from().equals(from) && r.to().equals(to)).findFirst();

    if (direct.isPresent()) {
      return direct.get();
    }

    // 2. Try inverted match (BOC specific logic)
    Optional<ExchangeRate> inverted = rates.stream()
        .filter(r -> r.from().equals(to) && r.to().equals(from)).findFirst();

    if (inverted.isPresent()) {
      ExchangeRate rev = inverted.get();
      // rate = 1 / originalRate
      BigDecimal invertedRate = BigDecimal.ONE.divide(rev.rate(),
          Precision.FOREX.getDecimalPlaces(), Rounding.FOREX.getMode());
      return new ExchangeRate(from, to, invertedRate, rev.quotedAt());
    }

    return null;
  }
}