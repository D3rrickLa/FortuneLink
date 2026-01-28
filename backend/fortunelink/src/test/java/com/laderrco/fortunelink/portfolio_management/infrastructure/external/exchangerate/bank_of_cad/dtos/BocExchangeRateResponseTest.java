package com.laderrco.fortunelink.portfolio_management.infrastructure.external.exchangerate.bank_of_cad.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BocExchangeRateResponseTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldMapBocJsonCorrectlyWithDynamicRates() throws JsonProcessingException {
        // Arrange: Sample BOC JSON structure
        String json = """
                {
                  "seriesDetail": {
                    "FXUSDCAD": {
                      "label": "USD to CAD",
                      "description": "US dollar to Canadian dollar daily exchange rate"
                    }
                  },
                  "observations": [
                    {
                      "d": "2024-01-25",
                      "FXUSDCAD": { "v": "1.3450" }
                    }
                  ]
                }
                """;

        // Act
        BocExchangeRateResponse response = objectMapper.readValue(json, BocExchangeRateResponse.class);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getObservations().size());

        BocExchangeRateResponse.Observation obs = response.getObservations().get(0);
        assertEquals("2024-01-25", obs.getDate());

        // Verify @JsonAnySetter logic
        // The "d" key should NOT be in the rates map
        assertFalse(obs.getRates().containsKey("d"), "The 'd' key should be excluded from rates map");

        // The dynamic series key should be present
        assertTrue(obs.getRates().containsKey("FXUSDCAD"));
        assertEquals(new BigDecimal("1.3450"), obs.getRates().get("FXUSDCAD").getValue());
    }

    @Test
    void addRate_ShouldOnlyAddNonDateKeysToRatesMap() {
        // Arrange
        BocExchangeRateResponse.Observation observation = new BocExchangeRateResponse.Observation();
        BocExchangeRateResponse.Observation.Rate mockRate = new BocExchangeRateResponse.Observation.Rate();
        mockRate.setValue(new BigDecimal("1.2345"));

        // Act
        observation.addRate("FXUSDCAD", mockRate); // Valid rate key
        observation.addRate("d", null); // The date key (should be ignored)

        // Assert
        // 1. Verify the valid rate was added
        assertTrue(observation.getRates().containsKey("FXUSDCAD"), "Currency series should be in the map");
        assertEquals(mockRate, observation.getRates().get("FXUSDCAD"));

        // 2. Verify the "d" key was NOT added
        assertFalse(observation.getRates().containsKey("d"), "The 'd' key should never be in the rates map");
        assertEquals(1, observation.getRates().size(), "Map should only contain the valid currency rate");
    }

    @Test
    void jackson_ShouldProperlyRouteDateAndRates() throws JsonProcessingException {
        String json = """
                {
                    "d": "2024-05-20",
                    "FXUSDCAD": { "v": "1.36" }
                }
                """;

        ObjectMapper mapper = new ObjectMapper();
        BocExchangeRateResponse.Observation obs = mapper.readValue(json, BocExchangeRateResponse.Observation.class);

        // Verify 'd' went to the String field
        assertEquals("2024-05-20", obs.getDate());

        // Verify 'd' did NOT go into the map
        assertFalse(obs.getRates().containsKey("d"));

        // Verify FXUSDCAD DID go into the map
        assertTrue(obs.getRates().containsKey("FXUSDCAD"));
    }
}
