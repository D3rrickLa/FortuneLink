package com.laderrco.fortunelink.portfolio.domain.services;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import java.time.Instant;
import java.util.Optional;

/**
 * Service providing currency conversion and exchange rate lookups.
 *
 * <p>
 * <b>Common Parameters:</b>
 * <ul>
 * <li>{@code amount} / {@code price} - The monetary value to be converted.</li>
 * <li>{@code targetCurrency} - The currency to convert into.</li>
 * <li>{@code asOfDate} - The historical point in time for the exchange
 * rate.</li>
 * </ul>
 */
public interface ExchangeRateService {
  /**
   * Retrieves the current exchange rate between two currencies.
   *
   * @return An Optional containing the rate, or empty if no rate exists.
   */
  Optional<ExchangeRate> getRate(Currency from, Currency to, Instant date);

  Optional<ExchangeRate> getRate(Currency from, Currency to);

  /**
   * Converts an amount to a target currency using the current rate.
   */
  Money convert(Money amount, Currency targetCurrency);

  /**
   * Converts an amount to a target currency using a historical rate.
   */
  Money convert(Money amount, Currency targetCurrency, Instant asOfDate);

}