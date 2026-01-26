package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad.dtos.BocExchangeRateResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ProviderExchangeRate;

@Component
public class BocResponseMapper {

    private static final String CAD = "CAD";
    private static final String SOURCE = "Bank of Canada";

    /**
     * Generic mapper
     * Flattens *all* CAD-involved FX series returned by BOC
     */
    public List<ProviderExchangeRate> toXYZ(BocExchangeRateResponse response) {
        List<ProviderExchangeRate> result = new ArrayList<>();

        if (response.getObservations() == null) {
            return result;
        }

        for (BocExchangeRateResponse.Observation obs : response.getObservations()) {
            LocalDate date = LocalDate.parse(obs.getDate());

            if (obs.getRates() == null) {
                continue;
            }

            for (Map.Entry<String, BocExchangeRateResponse.Observation.Rate> entry : obs.getRates().entrySet()) {
                String key = entry.getKey(); // FXUSDCAD / FXCADUSD
                if (!key.startsWith("FX") || key.length() != 8) {
                    continue;
                }

                String base = key.substring(2, 5);
                String target = key.substring(5, 8);

                BigDecimal rate = entry.getValue().getValue();
                if (rate == null) {
                    continue;
                }

                // BOC guarantees one side is CAD, but we still guard
                if (!CAD.equals(base) && !CAD.equals(target)) {
                    continue;
                }

                result.add(new ProviderExchangeRate(
                        base,
                        target,
                        rate,
                        date,
                        SOURCE));
            }
        }

        return result;
    }

    /**
     * Intent-aware mapper
     * Returns only the requested pair (supports cross-currency via CAD)
     * Baiscally if we wanted to go form EUR to USD, we use this
     */
    public List<ProviderExchangeRate> toXYZ(BocExchangeRateResponse response, String base, String target) {
        String baseCurrency = base.toUpperCase();
        String targetCurrency = target.toUpperCase();

        if (baseCurrency.equals(targetCurrency)) {
            throw new IllegalArgumentException("Base and target currencies cannot be the same");
        }

        List<ProviderExchangeRate> allRates = toXYZ(response);
        Map<LocalDate, List<ProviderExchangeRate>> byDate = allRates.stream()
                .collect(Collectors.groupingBy(ProviderExchangeRate::date));

        List<ProviderExchangeRate> result = new ArrayList<>();

        for (Map.Entry<LocalDate, List<ProviderExchangeRate>> entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<ProviderExchangeRate> rates = entry.getValue();

            // ---- Direct CAD pair ----
            if (CAD.equals(baseCurrency) || CAD.equals(targetCurrency)) {
                rates.stream()
                        .filter(r -> r.fromCurrency().equals(baseCurrency) && r.toCurrency().equals(targetCurrency))
                        .findFirst()
                        .ifPresent(result::add);
                continue;
            }

            // ---- Cross currency via CAD ----
            ProviderExchangeRate baseToCad = findRate(rates, baseCurrency, CAD);
            ProviderExchangeRate cadToTarget = findRate(rates, CAD, targetCurrency);

            if (baseToCad != null && cadToTarget != null) {
                BigDecimal crossRate = baseToCad.rate().multiply(cadToTarget.rate());

                result.add(new ProviderExchangeRate(
                        baseCurrency,
                        targetCurrency,
                        crossRate,
                        date,
                        SOURCE));
            }
        }

        return result;
    }

    private ProviderExchangeRate findRate(List<ProviderExchangeRate> rates, String base, String target) {
        return rates.stream()
                .filter(r -> r.fromCurrency().equals(base) && r.toCurrency().equals(target))
                .findFirst()
                .orElse(null);
    }
}
