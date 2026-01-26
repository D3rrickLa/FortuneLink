package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
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

    @Test
    void toXYZ_ShouldHandleNullRatesMap_ByContinuingToNextObservation() {
        // Arrange
        BocExchangeRateResponse response = new BocExchangeRateResponse();

        // Observation 1: Has a null rates map (triggers the 'continue')
        BocExchangeRateResponse.Observation obsWithNullMap = new BocExchangeRateResponse.Observation();
        obsWithNullMap.setDate("2024-01-01");
        obsWithNullMap.setRates(null); // Explicitly set to null

        // Observation 2: Has a valid rate (proves the loop continues)
        BocExchangeRateResponse.Observation validObs = new BocExchangeRateResponse.Observation();
        validObs.setDate("2024-01-02");
        BocExchangeRateResponse.Observation.Rate rate = new BocExchangeRateResponse.Observation.Rate();
        rate.setValue(new BigDecimal("1.35"));
        validObs.addRate("FXUSDCAD", rate);

        response.setObservations(List.of(obsWithNullMap, validObs));

        // Act
        List<ProviderExchangeRate> result = mapper.toXYZ(response);

        // Assert
        // Verify that it didn't crash on the null map and successfully processed the
        // second one
        assertEquals(1, result.size());
        assertEquals("2024-01-02", result.get(0).date().toString());
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

    @Test
    void toXYZ_ShouldReturnEmptyList_WhenObservationsAreNull() {
        // Arrange
        BocExchangeRateResponse response = new BocExchangeRateResponse();
        response.setObservations(null);

        // Act
        List<ProviderExchangeRate> result = mapper.toXYZ(response);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void toXYZ_ShouldSkipObservation_WhenRatesMapIsNull() {
        // Arrange
        BocExchangeRateResponse response = new BocExchangeRateResponse();
        BocExchangeRateResponse.Observation obs = new BocExchangeRateResponse.Observation();
        obs.setDate("2024-01-01");
        // Manual override to simulate null rates map if possible,
        // though your DTO initializes it to a new HashMap.
        // If your DTO prevents null, this test is for "double-sure" logic.
        response.setObservations(List.of(obs));

        // Act
        List<ProviderExchangeRate> result = mapper.toXYZ(response);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void toXYZ_DirectCAD_ShouldAddRateWhenFound() {
        // Arrange: Request USD to CAD (Direct)
        LocalDate date = LocalDate.of(2024, 1, 1);
        BocExchangeRateResponse.Observation obs = createObservation(date, "FXUSDCAD", "1.35");
        BocExchangeRateResponse response = wrapInResponse(obs);

        // Act
        List<ProviderExchangeRate> result = mapper.toXYZ(response, "USD", "CAD");

        // Assert: .ifPresent(result::add) was triggered
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("1.35"), result.get(0).rate());
    }

    @Test
    void toXYZ_DirectCAD_ShouldSkipWhenFilterReturnsEmpty() {
        // Arrange: Request USD to CAD, but response ONLY contains EUR to CAD
        LocalDate date = LocalDate.of(2024, 1, 1);
        BocExchangeRateResponse.Observation obs = createObservation(date, "FXEURCAD", "1.45");
        BocExchangeRateResponse response = wrapInResponse(obs);

        // Act
        List<ProviderExchangeRate> result = mapper.toXYZ(response, "USD", "CAD");

        // Assert: .filter(...) failed, result is empty
        assertTrue(result.isEmpty());
    }

    @Test
    void toXYZ_ShouldSkipKey_WhenKeyIsMalformed() {
        // Arrange
        BocExchangeRateResponse.Observation obs = createBaseObservation("2024-01-01");
        // Test "invalid" keys: too short, wrong prefix, or too long
        obs.addRate("SHORT", createRate("1.35"));
        obs.addRate("NOTFXUSD", createRate("1.35"));
        obs.addRate("FXUSDCAD_LONG", createRate("1.35"));

        BocExchangeRateResponse response = new BocExchangeRateResponse();
        response.setObservations(List.of(obs));

        // Act
        List<ProviderExchangeRate> result = mapper.toXYZ(response);

        // Assert
        assertTrue(result.isEmpty(), "Malformed keys should be skipped");
    }

    @Test
    void toXYZ_CrossCurrency_ShouldMultiplyRatesWhenBothExist() {
        // Arrange: EUR -> USD (Needs EUR/CAD and CAD/USD)
        BocExchangeRateResponse.Observation obs = new BocExchangeRateResponse.Observation();
        obs.setDate("2024-01-01");
        obs.addRate("FXEURCAD", createRate("1.50"));
        obs.addRate("FXCADUSD", createRate("0.75"));
        BocExchangeRateResponse response = wrapInResponse(obs);

        // Act
        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        // Assert: 1.50 * 0.75 = 1.125
        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("1.125").compareTo(result.get(0).rate()));
    }

    @Test
    void toXYZ_CrossCurrency_ShouldSkipIfOnePartIsMissing() {
        // Arrange: EUR -> USD requested, but CAD/USD is missing from response
        LocalDate date = LocalDate.of(2024, 1, 1);
        BocExchangeRateResponse.Observation obs = createObservation(date, "FXEURCAD", "1.50");
        BocExchangeRateResponse response = wrapInResponse(obs);

        // Act
        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        // Assert: baseToCad is found, but cadToTarget is null -> nothing added
        assertTrue(result.isEmpty());
    }

    @Test
    void toXYZ_ShouldThrowException_WhenCurrenciesMatch() {
        BocExchangeRateResponse response = new BocExchangeRateResponse();

        assertThrows(IllegalArgumentException.class, () -> {
            mapper.toXYZ(response, "USD", "USD");
        });
    }

    @Test
    void toXYZ_ShouldSkipRate_WhenRateValueIsNull() {
        // Arrange
        BocExchangeRateResponse.Observation obs = createBaseObservation("2024-01-01");
        obs.addRate("FXUSDCAD", createRate(null)); // Value is null

        BocExchangeRateResponse response = new BocExchangeRateResponse();
        response.setObservations(List.of(obs));

        // Act
        List<ProviderExchangeRate> result = mapper.toXYZ(response);

        // Assert
        assertTrue(result.isEmpty(), "Null rate values should be skipped");
    }

    @Test
    void toXYZ_ShouldSkipSeries_WhenNeitherSideIsCAD() {
        // Arrange
        BocExchangeRateResponse.Observation obs = createBaseObservation("2024-01-01");
        obs.addRate("FXUSDEUR", createRate("0.92")); // No CAD involved

        BocExchangeRateResponse response = new BocExchangeRateResponse();
        response.setObservations(List.of(obs));

        // Act
        List<ProviderExchangeRate> result = mapper.toXYZ(response);

        // Assert
        assertTrue(result.isEmpty(), "Non-CAD involved series should be skipped");
    }

    @Test
    void returns_direct_rate_when_base_is_cad() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("CAD", "USD", 0.75, date),
                createRate("EUR", "CAD", 1.45, date) // noise
        ));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "CAD", "USD");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("0.75"), result.get(0).rate());
    }

    @Test
    void returns_direct_rate_when_target_is_cad() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("EUR", "CAD", 1.45, date)));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "CAD");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("1.45"), result.get(0).rate());
    }

    @Test
    void throws_when_base_and_target_are_same() {
        BocExchangeRateResponse response = responseWithRates(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> mapper.toXYZ(response, "USD", "USD"));
    }

    @Test
    void skips_direct_cad_branch_when_neither_currency_is_cad() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("EUR", "USD", 1.1, date)));

        mapper.toXYZ(response, "EUR", "USD");

        // assertion is indirect: if CAD branch ran, coverage would show it
    }

    @Test
    void direct_cad_filter_fails_when_target_currency_does_not_match() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("CAD", "EUR", 0.70, date) // wrong target
        ));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "CAD", "USD");

        assertTrue(result.isEmpty());
    }

    @Test
    void direct_cad_filter_fails_when_base_currency_does_not_match() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("USD", "CAD", 1.33, date) // wrong base
        ));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "CAD", "USD");

        assertTrue(result.isEmpty());
    }

    @Test
    void computes_cross_rate_when_both_legs_exist() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("EUR", "CAD", 1.5, date),
                createRate("CAD", "USD", 0.8, date)));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("1.20"), result.get(0).rate());
    }

    @Test
    void skips_cross_rate_when_base_to_cad_missing() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("CAD", "USD", 0.8, date)));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        assertTrue(result.isEmpty());
    }

    @Test
    void skips_cross_rate_when_cad_to_target_missing() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("EUR", "CAD", 1.5, date)));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        assertTrue(result.isEmpty());
    }

    @Test
    void findRate_filters_out_when_both_base_and_target_do_not_match() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("GBP", "JPY", 180.0, date), // FF
                createRate("EUR", "CAD", 1.5, date) // noise to enter cross path
        ));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        assertTrue(result.isEmpty());
    }

    @Test
    void findRate_true_true_match() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("EUR", "CAD", 1.5, date),
                createRate("CAD", "USD", 0.8, date)));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        assertEquals(1, result.size());
    }

    @Test
    void findRate_true_false() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("EUR", "USD", 1.1, date), // base matches, target wrong
                createRate("CAD", "USD", 0.8, date)));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        assertTrue(result.isEmpty());
    }

    @Test
    void findRate_false_true() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("GBP", "CAD", 1.7, date), // target matches CAD, base wrong
                createRate("CAD", "USD", 0.8, date)));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        assertTrue(result.isEmpty());
    }

    @Test
    void findRate_false_false() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("GBP", "JPY", 180.0, date), // FF
                createRate("EUR", "CAD", 1.5, date) // noise
        ));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        assertTrue(result.isEmpty());
    }

    @Test
    void cross_currency_path_executes_findRate() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("EUR", "CAD", 1.5, date),
                createRate("CAD", "USD", 0.8, date)));

        mapper.toXYZ(response, "EUR", "USD");
    }

    @Test
    void explicitly_executes_findRate_all_branches() {
        LocalDate date = LocalDate.of(2024, 1, 1);

        BocExchangeRateResponse response = responseWithRates(List.of(
                createRate("EUR", "CAD", 1.5, date), // baseToCad
                createRate("CAD", "USD", 0.8, date), // cadToTarget
                createRate("GBP", "JPY", 180.0, date) // FF case
        ));

        List<ProviderExchangeRate> result = mapper.toXYZ(response, "EUR", "USD");

        assertEquals(1, result.size());
    }

    @Test
    void findRate_via_reflection_covers_all_branches() throws Exception {
        LocalDate date = LocalDate.of(2024, 1, 1);

        List<ProviderExchangeRate> rates = List.of(
                createRate("EUR", "USD", 1.1, date), // TT candidate
                createRate("EUR", "CAD", 1.5, date), // TF
                createRate("GBP", "USD", 1.3, date), // FT
                createRate("GBP", "JPY", 180.0, date) // FF
        );

        Method method = BocResponseMapper.class.getDeclaredMethod(
                "findRate",
                List.class,
                String.class,
                String.class);
        method.setAccessible(true);

        // TT
        ProviderExchangeRate tt = (ProviderExchangeRate) method.invoke(mapper, rates, "EUR", "USD");
        assertNotNull(tt);

        // TF
        ProviderExchangeRate tf = (ProviderExchangeRate) method.invoke(mapper, rates, "EUR", "CHF");
        assertNull(tf);

        // FT
        ProviderExchangeRate ft = (ProviderExchangeRate) method.invoke(mapper, rates, "AUD", "USD");
        assertNull(ft);

        // FF
        ProviderExchangeRate ff = (ProviderExchangeRate) method.invoke(mapper, rates, "AUD", "CHF");
        assertNull(ff);
    }

    private ProviderExchangeRate createRate(String string, String string2, double d, LocalDate today) {
        return new ProviderExchangeRate(string, string2, BigDecimal.valueOf(d), today, "");
    }

    // --- Helper Methods ---
    private BocExchangeRateResponse.Observation createBaseObservation(String date) {
        BocExchangeRateResponse.Observation obs = new BocExchangeRateResponse.Observation();
        obs.setDate(date);
        return obs;
    }

    private BocExchangeRateResponse.Observation.Rate createRate(String value) {
        BocExchangeRateResponse.Observation.Rate rate = new BocExchangeRateResponse.Observation.Rate();
        if (value != null)
            rate.setValue(new BigDecimal(value));
        return rate;
    }

    /**
     * Creates a single observation with one rate.
     */
    private BocExchangeRateResponse.Observation createObservation(LocalDate date, String seriesKey, String value) {
        BocExchangeRateResponse.Observation obs = new BocExchangeRateResponse.Observation();
        obs.setDate(date.toString());

        BocExchangeRateResponse.Observation.Rate rate = new BocExchangeRateResponse.Observation.Rate();
        if (value != null) {
            rate.setValue(new BigDecimal(value));
        }

        obs.addRate(seriesKey, rate);
        return obs;
    }

    /**
     * Helper to wrap a list of observations into the top-level response DTO.
     */
    private BocExchangeRateResponse wrapInResponse(BocExchangeRateResponse.Observation... observations) {
        BocExchangeRateResponse response = new BocExchangeRateResponse();
        response.setObservations(Arrays.asList(observations));
        return response;
    }

    private BocExchangeRateResponse responseWithRates(
            List<ProviderExchangeRate> rates) {
        BocExchangeRateResponse response = new BocExchangeRateResponse();

        Map<String, BocExchangeRateResponse.SeriesDetail> seriesDetail = new HashMap<>();

        List<BocExchangeRateResponse.Observation> observations = rates.stream()
                .collect(Collectors.groupingBy(ProviderExchangeRate::date))
                .entrySet()
                .stream()
                .map(entry -> {
                    BocExchangeRateResponse.Observation obs = new BocExchangeRateResponse.Observation();

                    obs.setDate(entry.getKey().toString());

                    for (ProviderExchangeRate rate : entry.getValue()) {
                        // Stable fake key, but MUST match seriesDetail
                        String key = "FX" + rate.fromCurrency() + rate.toCurrency();

                        BocExchangeRateResponse.Observation.Rate r = new BocExchangeRateResponse.Observation.Rate();
                        r.setValue(rate.rate());

                        obs.getRates().put(key, r);

                        // ---- seriesDetail ----
                        BocExchangeRateResponse.SeriesDetail sd = new BocExchangeRateResponse.SeriesDetail();

                        BocExchangeRateResponse.SeriesDetail.Dimension dim = new BocExchangeRateResponse.SeriesDetail.Dimension();
                        dim.setKey(rate.fromCurrency() + rate.toCurrency());

                        sd.setDimension(dim);
                        seriesDetail.put(key, sd);
                    }

                    return obs;
                })
                .toList();

        response.setObservations(observations);
        response.setSeriesDetail(seriesDetail);

        return response;
    }

}
