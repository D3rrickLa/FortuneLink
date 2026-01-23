package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.exceptions.CurrencyAreTheSameException;
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
    public ResponseEntity<BigDecimal> getRate(
            @RequestParam String from,
            @RequestParam String to) {
        try {
            BigDecimal rate = exchangeRateService.getExchangeRate(ValidatedCurrency.of(from), ValidatedCurrency.of(to));
            return ResponseEntity.ok(rate);
        } catch (CurrencyAreTheSameException e) {
            return ResponseEntity.badRequest().body(BigDecimal.ONE);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
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
     */
    @PostMapping("/convert")
    public ResponseEntity<Money> convert(@RequestBody ConvertRequest request) {
        Money amount = new Money(request.getAmount(), ValidatedCurrency.of(request.getCurrency()));
        Money converted = exchangeRateService.convert(amount, ValidatedCurrency.of(request.getTargetCurrency()));
        return ResponseEntity.ok(converted);
    }

    // Request body class
    public static class ConvertRequest {
        private BigDecimal amount;
        private String currency;
        private String targetCurrency;

        // getters & setters
        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getTargetCurrency() {
            return targetCurrency;
        }

        public void setTargetCurrency(String targetCurrency) {
            this.targetCurrency = targetCurrency;
        }
    }
}
