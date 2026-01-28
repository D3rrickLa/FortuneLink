package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

public class FmpConfigurationPropertiesTest {
    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowExceptionWhenApiKeyIsMissing(String s) {
        FmpConfigurationProperties props = new FmpConfigurationProperties();
        props.setApiKey(s); // Empty key

        IllegalStateException exception = assertThrows(IllegalStateException.class, props::validate);
        assertTrue(exception.getMessage().contains("FMP API key is required"));
    }

    @Test
    void shouldNotThrowExceptionWhenApiKeyIsPresent() {
        FmpConfigurationProperties props = new FmpConfigurationProperties();
        props.setApiKey("test-key-123");

        assertDoesNotThrow(props::validate);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowExceptionWhenbaseUrlIsMissing(String s) {
        FmpConfigurationProperties props = new FmpConfigurationProperties();
        props.setApiKey("test-key-123");
        props.setBaseUrl(s);

        IllegalStateException exception = assertThrows(IllegalStateException.class, props::validate);
        assertTrue(exception.getMessage().contains("FMP base URL cannot be blank"));
    }
}
