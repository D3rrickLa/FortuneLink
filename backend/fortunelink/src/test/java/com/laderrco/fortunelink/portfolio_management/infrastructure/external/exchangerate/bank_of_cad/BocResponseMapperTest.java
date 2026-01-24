package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad.dtos.BocExchangeRateResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ProviderExchangeRate;

public class BocResponseMapperTest {
    private BocResponseMapper mapper;

    @BeforeEach
    void setup() {
        mapper = new BocResponseMapper();
    }

    @Test
    void toXYZ_nullObservations_returnsEmpty() {
        BocExchangeRateResponse response = new BocExchangeRateResponse();

        List<ProviderExchangeRate> result = mapper.toXYZ(response);

        assertThat(result).isEmpty();
    }

    @Test
    void toXYZ_nullRates_skipped() {
        BocExchangeRateResponse.Observation obs = new BocExchangeRateResponse.Observation();
        obs.setDate("2024-01-01");

        BocExchangeRateResponse response = new BocExchangeRateResponse();
        response.setObservations(List.of(obs));

        assertThat(mapper.toXYZ(response)).isEmpty();
    }

    @Test
    void toXYZ_invalidKey_skipped() {
        BocExchangeRateResponse response = responseWith("2024-01-01",
                Map.of("INVALID", BigDecimal.ONE));

        assertThat(mapper.toXYZ(response)).isEmpty();
    }

    @Test
    void toXYZ_nonCadPair_skipped() {
        BocExchangeRateResponse response = responseWith("2024-01-01",
                Map.of("FXEURUSD", BigDecimal.ONE));

        assertThat(mapper.toXYZ(response)).isEmpty();
    }

    @Test
    void toXYZ_nullRate_skipped() {
        BocExchangeRateResponse response = responseWith("2024-01-01",
                Map.of("FXUSDCAD", null));

        assertThat(mapper.toXYZ(response)).isEmpty();
    }

    @Test
    void toXYZ_validCadPair_mapped() {
        BocExchangeRateResponse response = responseWith("2024-01-01",
                Map.of("FXUSDCAD", BigDecimal.valueOf(1.25)));

        List<ProviderExchangeRate> result = mapper.toXYZ(response);

        assertThat(result).hasSize(1);
        ProviderExchangeRate rate = result.get(0);

        assertThat(rate.fromCurrency()).isEqualTo("USD");
        assertThat(rate.toCurrency()).isEqualTo("CAD");
        assertThat(rate.rate()).isEqualByComparingTo("1.25");
        assertThat(rate.date()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(rate.source()).isEqualTo("Bank of Canada");
    }

    @Test
    void toXYZ_sameCurrency_throws() {
        BocExchangeRateResponse response = new BocExchangeRateResponse();

        assertThatThrownBy(() -> mapper.toXYZ(response, "USD", "USD"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be the same");
    }

    @Test
    void toXYZ_directCadPair() {
        BocExchangeRateResponse response = responseWith("2024-01-01",
                Map.of("FXUSDCAD", BigDecimal.valueOf(1.30)));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "USD", "CAD");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rate())
                .isEqualByComparingTo("1.30");
    }

    @Test
    void toXYZ_crossCurrencyViaCad() {
        BocExchangeRateResponse response = responseWith("2024-01-01",
                Map.of(
                        "FXEURCAD", BigDecimal.valueOf(1.50),
                        "FXCADUSD", BigDecimal.valueOf(0.75)));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rate())
                .isEqualByComparingTo("1.125"); // 1.5 * 0.75
    }

    @Test
    void toXYZ_missingLeg_skipped() {
        BocExchangeRateResponse response = responseWith("2024-01-01",
                Map.of("FXEURCAD", BigDecimal.valueOf(1.50)));

        assertThat(mapper.toXYZ(response, "EUR", "USD"))
                .isEmpty();
    }

    private BocExchangeRateResponse responseWith(
            String date,
            Map<String, BigDecimal> rates) {

        BocExchangeRateResponse.Observation obs = new BocExchangeRateResponse.Observation();
        obs.setDate(date);

        if (rates != null) {
            Map<String, BocExchangeRateResponse.Observation.Rate> mapped = rates.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> {
                                BocExchangeRateResponse.Observation.Rate r = new BocExchangeRateResponse.Observation.Rate();
                                r.setValue(e.getValue());
                                return r;
                            }));
            obs.setRates(mapped);
        }

        BocExchangeRateResponse response = new BocExchangeRateResponse();
        response.setObservations(List.of(obs));
        return response;
    }

}
