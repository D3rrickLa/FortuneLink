package com.laderrco.fortunelink.portfolio_management.api.web.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.ConvertRequest;
import com.laderrco.fortunelink.portfolio_management.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.DevSecurityConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.RateLimitConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.SecurityConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.GlobalExceptionHandler;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

@Import({ SecurityConfig.class, RateLimitConfig.class }) // Explicitly pulls in your permitAll() logic
@WebMvcTest({ ExchangeRateController.class, GlobalExceptionHandler.class, AuthenticationUserService.class })
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    @Autowired
    private ObjectMapper objectMapper;

    private ValidatedCurrency usd;
    private ValidatedCurrency cad;
    private UUID mockUserId;

    @BeforeEach
    void setUp() {
        usd = ValidatedCurrency.of("USD");
        cad = ValidatedCurrency.of("CAD");
        mockUserId = UUID.randomUUID();
    }

    @SuppressWarnings("null")
    @Test
    void getRate_ShouldReturnRate_WhenCurrenciesAreValid() throws Exception {
        // Arrange
        ExchangeRate mockRate = new ExchangeRate(usd, cad, new BigDecimal("1.35"), Instant.now(), "BOC");
        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(Optional.of(mockRate));

        // Act & Assert
        mockMvc.perform(get("/api/exchange/rate")
                .with(jwt().jwt(j -> j.subject(mockUserId.toString())))
                .param("from", "USD")
                .param("to", "CAD"))
                .andExpect(status().isOk())
                .andExpect(content().string("1.350000"));
    }

    @SuppressWarnings("null")
    @Test
    void getRate_ShouldReturnOne_WhenCurrenciesAreSame() throws Exception {
        // Arrange
        ValidatedCurrency usd = ValidatedCurrency.of("USD");
        // We must tell the MOCK service to return the 1:1 rate
        ExchangeRate identityRate = new ExchangeRate(usd, usd, BigDecimal.ONE, Instant.now(), "INTERNAL");

        when(exchangeRateService.getExchangeRate(any(), any()))
                .thenReturn(Optional.of(identityRate));

        // Act & Assert
        mockMvc.perform(get("/api/exchange/rate")
                .with(jwt().jwt(j -> j.subject(mockUserId.toString())))
                .param("from", "USD")
                .param("to", "USD"))
                .andExpect(status().isOk())
                .andExpect(content().string("1.000000")); // Should pass now!
    }

    @SuppressWarnings("null")
    @Test
    void convert_ShouldReturnConvertedMoney() throws Exception {
        // Arrange
        ConvertRequest request = new ConvertRequest(new BigDecimal("100"), "USD", "CAD");
        Money convertedMoney = new Money(new BigDecimal("135.00"), cad);

        when(exchangeRateService.convert(any(Money.class), any(ValidatedCurrency.class)))
                .thenReturn(convertedMoney);

        // Act & Assert
        mockMvc.perform(post("/api/exchange/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt().jwt(j -> j.subject(mockUserId.toString())))
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(135.00))
                .andExpect(jsonPath("$.currency.code").value("CAD"));
    }

}