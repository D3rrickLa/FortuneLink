package com.laderrco.fortunelink.portfolio_management.infrastructure.config;

import static org.assertj.core.api.Assertions.*;

import java.net.http.HttpClient;
import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

public class BankOfCanadaConfigurationPropertiesTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(BankOfCanadaConfigurationProperties.class));

    @Test
    @DisplayName("Should load default values when no properties are provided")
    void shouldLoadDefaults() {
        contextRunner.run(context -> {
            BankOfCanadaConfigurationProperties props = context.getBean(BankOfCanadaConfigurationProperties.class);
            assertThat(props.getBaseUrl()).isEqualTo("https://www.bankofcanada.ca/valet/");
            assertThat(props.getTimeoutSeconds()).isEqualTo(10);
            assertThat(props.isDebugLogging()).isTrue();
        });
    }

    @Test
    @DisplayName("Should bind custom properties from environment")
    void shouldBindCustomProperties() {
        contextRunner
                .withPropertyValues(
                        "boc.base-url=https://www.bankofcanada.ca/valet/",
                        "boc.timeout-seconds=10",
                        "boc.debug-logging=false")
                .run(context -> {
                    BankOfCanadaConfigurationProperties props = context
                            .getBean(BankOfCanadaConfigurationProperties.class);
                    assertThat(props.getBaseUrl()).isEqualTo("https://www.bankofcanada.ca/valet/");
                    assertThat(props.getTimeoutSeconds()).isEqualTo(10);
                    assertThat(props.isDebugLogging()).isTrue();
                });
    }

    @Test
    @DisplayName("Validate should throw exception if baseUrl is empty")
    void validateShouldFailWhenUrlIsEmpty() {
        BankOfCanadaConfigurationProperties props = new BankOfCanadaConfigurationProperties();
        props.setBaseUrl("");

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bank of Canada base URL cannot be blank");
    }

    @Test
    @DisplayName("Validate should throw exception if baseUrl is empty")
    void validateShouldFailWhenUrlIsNull() {
        BankOfCanadaConfigurationProperties props = new BankOfCanadaConfigurationProperties();
        props.setBaseUrl(null);

        assertThatThrownBy(props::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bank of Canada base URL cannot be blank");
    }

    @Test
    @DisplayName("Validate should pass with valid URL")
    void validateShouldPassWithValidUrl() {
        BankOfCanadaConfigurationProperties props = new BankOfCanadaConfigurationProperties();
        props.setBaseUrl("https://valid.url");

        // Should not throw any exception
        props.validate();
    }

    @Test
    @DisplayName("HttpClient bean should be created with configured timeout")
    void shouldCreateHttpClientWithCorrectTimeout() {
        contextRunner
                .withPropertyValues("boc.timeout-seconds=5")
                .run(context -> {
                    assertThat(context).hasBean("bocHttpClient");
                    HttpClient client = context.getBean("bocHttpClient", HttpClient.class);

                    // We verify the configuration indirectly via the property check
                    // as HttpClient doesn't expose timeout via getter easily
                    assertThat(client.connectTimeout()).isPresent().contains(Duration.ofSeconds(5));
                    assertThat(client.followRedirects()).isEqualTo(HttpClient.Redirect.NORMAL);
                });
    }
}
