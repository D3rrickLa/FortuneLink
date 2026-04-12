package com.laderrco.fortunelink.portfolio.infrastructure.exchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc.exceptions.ExchangeRateUnavailableException;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceImplTest {

  @Mock
  private ExchangeRateProvider provider;

  private ExchangeRateServiceImpl exchangeRateService;

  private final Currency usd = Currency.of("USD");
  private final Currency cad = Currency.of("CAD");
  private final Instant now = Instant.now();

  @BeforeEach
  void setUp() {
    exchangeRateService = new ExchangeRateServiceImpl(provider);
  }

  @Test
  void getRateShouldReturnRateOnSuccess() {

    ExchangeRate mockRate = mock(ExchangeRate.class);
    when(provider.getExchangeRate(eq(usd), eq(cad), any(Instant.class)))
        .thenReturn(mockRate);

    Optional<ExchangeRate> result = exchangeRateService.getRate(usd, cad);

    assertThat(result).isPresent().contains(mockRate);
  }

  @Test
  void getRateShouldReturnEmptyOnProviderFailure() {

    when(provider.getExchangeRate(any(), any(), any()))
        .thenThrow(new RuntimeException("API Down"));

    Optional<ExchangeRate> result = exchangeRateService.getRate(usd, cad);

    assertThat(result).isEmpty();

  }

  @Test
  void convertShouldReturnSameAmountIfCurrenciesMatch() {

    Money amount = new Money(new BigDecimal("100.00"), usd);

    Money result = exchangeRateService.convert(amount, usd);

    assertThat(result).isEqualTo(amount);
    verifyNoInteractions(provider);
  }

  @Test
  void convertShouldPerformConversionOnSuccess() {

    Money amount = new Money(new BigDecimal("100.00"), usd);
    Money convertedAmount = new Money(new BigDecimal("135.00"), cad);
    ExchangeRate mockRate = mock(ExchangeRate.class);

    when(provider.getExchangeRate(eq(usd), eq(cad), any())).thenReturn(mockRate);
    when(mockRate.convert(amount)).thenReturn(convertedAmount);

    Money result = exchangeRateService.convert(amount, cad);

    assertThat(result).isEqualTo(convertedAmount);
  }

  @Test
  void convertShouldThrowCustomExceptionWhenRateUnavailable() {

    Money amount = new Money(new BigDecimal("100.00"), usd);
    when(provider.getExchangeRate(any(), any(), any()))
        .thenThrow(new RuntimeException("Connection Timeout"));

    assertThatThrownBy(() -> exchangeRateService.convert(amount, cad))
        .isInstanceOf(ExchangeRateUnavailableException.class)
        .hasMessageContaining("USD")
        .hasMessageContaining("CAD");
  }

  @Test
  void convertWithDateshouldPropagateOriginalExceptionOnFailure() {

    Money amount = new Money(new BigDecimal("100.00"), usd);
    RuntimeException originalException = new RuntimeException("Specific API Error");
    when(provider.getExchangeRate(any(), any(), any())).thenThrow(originalException);

    assertThatThrownBy(() -> exchangeRateService.convert(amount, cad, now))
        .isSameAs(originalException);
  }

  @Test
  void convertWithDate_shouldReturnSameAmountIfCurrenciesMatch() {

    Money amount = new Money(new BigDecimal("100.00"), usd);
    Instant specificDate = Instant.parse("2023-01-01T10:00:00Z");

    Money result = exchangeRateService.convert(amount, usd, specificDate);

    assertThat(result).isEqualTo(amount);

    verifyNoInteractions(provider);
  }

  @Test
  void convertWithDate_shouldCallProviderAndConvert() {

    Money amount = new Money(new BigDecimal("100.00"), usd);
    Money expectedMoney = new Money(new BigDecimal("135.00"), cad);
    Instant specificDate = Instant.parse("2023-01-01T10:00:00Z");

    ExchangeRate mockRate = mock(ExchangeRate.class);

    when(provider.getExchangeRate(usd, cad, specificDate)).thenReturn(mockRate);
    when(mockRate.convert(amount)).thenReturn(expectedMoney);

    Money result = exchangeRateService.convert(amount, cad, specificDate);

    assertThat(result).isEqualTo(expectedMoney);
    verify(provider).getExchangeRate(usd, cad, specificDate);
  }

  @Test
  void convertWithDate_shouldPropagateExceptionWhenProviderFails() {

    Money amount = new Money(new BigDecimal("100.00"), usd);
    Instant specificDate = Instant.now();
    RuntimeException providerError = new RuntimeException("BOC API Timeout");

    when(provider.getExchangeRate(eq(usd), eq(cad), eq(specificDate)))
        .thenThrow(providerError);

    assertThatThrownBy(() -> exchangeRateService.convert(amount, cad, specificDate))
        .isSameAs(providerError);
  }
}