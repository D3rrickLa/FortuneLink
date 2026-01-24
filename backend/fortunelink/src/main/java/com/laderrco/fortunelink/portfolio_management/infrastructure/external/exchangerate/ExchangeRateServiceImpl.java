package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate;

import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateProvider;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ExchangeRateMapper;
import com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.common.ProviderExchangeRate;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyAreTheSameException;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements ExchangeRateService {
    private static final Logger log = LoggerFactory.getLogger(ExchangeRateServiceImpl.class);
    private final ExchangeRateMapper mapper;
    private final ExchangeRateProvider provider;

    @Override
    public Optional<ExchangeRate> getExchangeRate(ValidatedCurrency from, ValidatedCurrency to) throws CurrencyAreTheSameException, JsonMappingException, JsonProcessingException {
        if (from.equals(to)) {
            throw new CurrencyAreTheSameException(from.getCode());
        }

        // latest rate
        ProviderExchangeRate exchangeRate = provider.getExchangeRate(from, to, null);
        return Optional.of(mapper.toExchangeRate(exchangeRate));
    }

    @Override
    public Money convert(Money amount, ValidatedCurrency targetCurrency, Instant asOfDate) throws JsonMappingException, JsonProcessingException {
        log.debug("Fetching conversion for amount: {} to {}", amount, targetCurrency.getCode());
        ValidatedCurrency sourceCurrency = amount.currency();
        // Same currency → no conversion
        if (sourceCurrency.equals(targetCurrency)) {
            return amount;
        }

        ProviderExchangeRate rate = provider.getExchangeRate(sourceCurrency, targetCurrency, asOfDate);
        
        return mapper.toMoney(amount, rate);
    }

    @Override
    public Money convert(Money amount, ValidatedCurrency targetCurrency)  throws JsonMappingException, JsonProcessingException {
        return convert(amount, targetCurrency, null);
    }

}
