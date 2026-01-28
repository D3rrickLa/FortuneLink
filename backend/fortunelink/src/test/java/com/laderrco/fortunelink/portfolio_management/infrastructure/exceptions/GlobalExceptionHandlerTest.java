package com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.laderrco.fortunelink.portfolio_management.api.models.market.MarketDataDtoMapper;
import com.laderrco.fortunelink.portfolio_management.api.web.controllers.MarketDataController;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.DevSecurityConfig;
import com.laderrco.fortunelink.portfolio_management.infrastructure.config.RateLimitConfig;

@WebMvcTest(MarketDataController.class)
@Import({ DevSecurityConfig.class, RateLimitConfig.class })
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketDataService marketDataService;

    @MockitoBean
    private MarketDataDtoMapper mapper;

    // ---------------------------------------------------
    // Generic Exception Handler Test
    // ---------------------------------------------------

    @Test
    @SuppressWarnings("null")
    void handleGenericException_returnsInternalServerError() throws Exception {
        // Arrange: Mock the service to throw a raw Exception or RuntimeException
        when(marketDataService.getCurrentQuote(any()))
                .thenThrow(new RuntimeException("Boom"));

        // Act & Assert
        mockMvc.perform(get("/api/market-data/price/AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("INTERNAL_SERVER_ERROR"))
                // Note: We check for your "friendly" message, not the "Boom" message
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.path").value("/api/market-data/price/AAPL"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}