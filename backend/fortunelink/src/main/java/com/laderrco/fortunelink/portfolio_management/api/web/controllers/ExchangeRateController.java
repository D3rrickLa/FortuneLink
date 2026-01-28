package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.ConvertRequest;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /**
     * GET latest exchange rate
     * Example: GET /api/exchange/rate?from=USD&to=EUR
     */
    @GetMapping("/rate")
    public ResponseEntity<BigDecimal> getRate(@RequestParam String from, @RequestParam String to) {
        return exchangeRateService.getExchangeRate(ValidatedCurrency.of(from), ValidatedCurrency.of(to))
                .map(rate -> ResponseEntity.ok(rate.rate()))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST convert money
     * Example: POST /api/exchange/convert
     * Body:
     * {
     * "amount": 100.50,
     * "currency": "USD",
     * "targetCurrency": "EUR"
     * }
     * 
     * @throws JsonProcessingException
     * @throws JsonMappingException
     */
    @PostMapping("/convert")
    public ResponseEntity<Money> convert(@RequestBody ConvertRequest request)
            throws JsonMappingException, JsonProcessingException {
        Money amount = new Money(request.getAmount(), ValidatedCurrency.of(request.getCurrency()));
        Money converted = exchangeRateService.convert(amount, ValidatedCurrency.of(request.getTargetCurrency()));
        return ResponseEntity.ok(converted);
    }
}
