package com.laderrco.fortunelink.portfolio.infrastructure.exchange;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {
  private static final Logger log = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);
  private final ExchangeRateProvider provider;

  @Override
  public Optional<ExchangeRate> getRate(Currency from, Currency to) {
    return getRate(from, to, Instant.now());
  }

  @Override
  public Optional<ExchangeRate> getRate(Currency from, Currency to, Instant date) {
    try {
      // If the API is up, we get the real rate
      return Optional.of(provider.getExchangeRate(from, to, date));
    } catch (Exception ex) {
      // If the API (BOC) is down, we log and return Empty
      log.warn("Exchange rate provider failed for {}/{}. Cause: {}", from.getCode(), to.getCode(),
          ex.getMessage());
      return Optional.empty();
    }
  }

  @Override
  public Money convert(Money amount, Currency targetCurrency) {
    if (amount.currency().equals(targetCurrency)) {
      return amount;
    }
    return getRate(amount.currency(), targetCurrency).map(rate -> rate.convert(amount)).orElseThrow(
        () -> new ExchangeRateUnavailableException(amount.currency().getCode(),
            targetCurrency.getCode(), Instant.now()));
  }

  @Override
  public Money convert(Money amount, Currency targetCurrency, Instant asOfDate) {
    if (amount.currency().equals(targetCurrency)) {
      return amount;
    }

    try {
      ExchangeRate rate = provider.getExchangeRate(amount.currency(), targetCurrency, asOfDate);
      return rate.convert(amount);
    } catch (Exception ex) {
      throw ex;
    }
  }
}
