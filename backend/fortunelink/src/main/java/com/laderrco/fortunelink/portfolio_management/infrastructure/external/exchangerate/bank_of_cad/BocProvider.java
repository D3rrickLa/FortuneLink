package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateProvider;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.ExchangeRateUnavailableException;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad.dtos.BocExchangeRateResponse;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ProviderExchangeRate;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class BocProvider implements ExchangeRateProvider {
    private final BocApiClient bocApiClient;
    private final BocResponseMapper mapper;

    @Override
    public ProviderExchangeRate getExchangeRate(ValidatedCurrency from, ValidatedCurrency to, Instant asOf)
            throws JsonMappingException, JsonProcessingException {
        if (from.equals(to)) {
            return new ProviderExchangeRate(from.getCode(), to.getCode(), BigDecimal.ONE, LocalDate.now(),
                    "INTERNAL CALL");
        }

        BocExchangeRateResponse response;

        // ---- Latest rate ----
        if (asOf == null) {
            response = bocApiClient.getLatestExchangeRate(
                    to.getCode(),
                    from.getCode());
        }
        // ---- Historical rate ----
        else {
            LocalDate date = asOf.atZone(ZoneOffset.UTC).toLocalDate();

            // single-day clamp (BOC may return empty on weekends/holidays)
            response = bocApiClient.getHistoricalExchangeRate(
                    to.getCode(),
                    from.getCode(),
                    date.atStartOfDay(),
                    date.atTime(LocalTime.MAX));
        }

        List<ProviderExchangeRate> rates = mapper.toXYZ(response, from.getCode(), to.getCode());

        if (rates.isEmpty()) {
            throw new ExchangeRateUnavailableException(from.getCode(), to.getCode(), asOf);
        }

        // BOC guarantees one rate per business day
        return new ProviderExchangeRate(from.getCode(), to.getCode(), rates.get(rates.size() - 1).rate(),
                LocalDate.now(), "BOC");
    }

}
