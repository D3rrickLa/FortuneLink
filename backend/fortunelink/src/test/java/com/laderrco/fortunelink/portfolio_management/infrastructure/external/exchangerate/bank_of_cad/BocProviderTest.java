package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.ExchangeRateUnavailableException;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad.dtos.BocExchangeRateResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ProviderExchangeRate;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

@ExtendWith(MockitoExtension.class)
public class BocProviderTest {

    @Mock
    private BocApiClient bocApiClient;

    @Mock
    private BocResponseMapper mapper;

    private BocProvider provider;

    @BeforeEach
    void setup() {
        provider = new BocProvider(bocApiClient, mapper);
    }

    @Test
    void getExchangeRate_sameCurrency_returnsOne() throws Exception {
        ProviderExchangeRate rate = provider.getExchangeRate(
                ValidatedCurrency.USD,
                ValidatedCurrency.USD,
                null);

        assertThat(rate.rate()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(rate.source()).isEqualTo("INTERNAL CALL");

        verifyNoInteractions(bocApiClient, mapper);
    }

    @Test
    void getExchangeRate_latestRate() throws Exception {
        BocExchangeRateResponse response = new BocExchangeRateResponse();

        when(bocApiClient.getLatestExchangeRate("CAD", "USD"))
                .thenReturn(response);

        ProviderExchangeRate mapped = new ProviderExchangeRate("USD", "CAD",
                BigDecimal.valueOf(1.25),
                LocalDate.now(),
                "BOC");

        when(mapper.toXYZ(response, "USD", "CAD"))
                .thenReturn(List.of(mapped));

        ProviderExchangeRate result = provider.getExchangeRate(
                ValidatedCurrency.USD,
                ValidatedCurrency.CAD,
                null);

        assertThat(result.rate()).isEqualByComparingTo("1.25");
        assertThat(result.source()).isEqualTo("BOC");

        verify(bocApiClient).getLatestExchangeRate("CAD", "USD");
    }

    @Test
    void getExchangeRate_historicalRate() throws Exception {
        Instant instant = Instant.parse("2024-01-10T12:00:00Z");

        BocExchangeRateResponse response = new BocExchangeRateResponse();

        when(bocApiClient.getHistoricalExchangeRate(
                eq("CAD"),
                eq("USD"),
                any(),
                any()))
                .thenReturn(response);

        ProviderExchangeRate dayRate = new ProviderExchangeRate("USD", "CAD",
                BigDecimal.valueOf(1.30),
                LocalDate.of(2024, 1, 10),
                "BOC");

        when(mapper.toXYZ(response, "USD", "CAD"))
                .thenReturn(List.of(dayRate));

        ProviderExchangeRate result = provider.getExchangeRate(
                ValidatedCurrency.USD,
                ValidatedCurrency.CAD,
                instant);

        assertThat(result.rate()).isEqualByComparingTo("1.30");

        verify(bocApiClient).getHistoricalExchangeRate(
                eq("CAD"),
                eq("USD"),
                any(),
                any());
    }

    @Test
    void getExchangeRate_noRatesReturned_throwsException() throws Exception {
        BocExchangeRateResponse response = new BocExchangeRateResponse();

        when(bocApiClient.getLatestExchangeRate("CAD", "USD"))
                .thenReturn(response);

        when(mapper.toXYZ(response, "USD", "CAD"))
                .thenReturn(List.of());

        assertThrows(
                ExchangeRateUnavailableException.class,
                () -> provider.getExchangeRate(
                        ValidatedCurrency.USD,
                        ValidatedCurrency.CAD,
                        null));
    }

    @Test
    void getExchangeRate_returnsLastRate() throws Exception {
        BocExchangeRateResponse response = new BocExchangeRateResponse();

        ProviderExchangeRate r1 = new ProviderExchangeRate("USD", "CAD",
                BigDecimal.valueOf(1.20),
                LocalDate.now(),
                "BOC");

        ProviderExchangeRate r2 = new ProviderExchangeRate("USD", "CAD",
                BigDecimal.valueOf(1.22),
                LocalDate.now(),
                "BOC");

        when(bocApiClient.getLatestExchangeRate("CAD", "USD"))
                .thenReturn(response);

        when(mapper.toXYZ(response, "USD", "CAD"))
                .thenReturn(List.of(r1, r2));

        ProviderExchangeRate result = provider.getExchangeRate(
                ValidatedCurrency.USD,
                ValidatedCurrency.CAD,
                null);

        assertThat(result.rate()).isEqualByComparingTo("1.22");
    }

}
