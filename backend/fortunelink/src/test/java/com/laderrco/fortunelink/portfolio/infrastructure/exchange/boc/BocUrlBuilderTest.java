package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BocUrlBuilderTest {

  private static final String BASE_URL = "https://www.bankofcanada.ca/valet";
  private BocUrlBuilder builder;

  @BeforeEach
  void setUp() {
    builder = new BocUrlBuilder(BASE_URL);
  }

  @Test
  void shouldBuildBasicUrlWithObservations() {
    String result = builder.observations("FXUSDCAD").build();

    assertThat(result).isEqualTo(BASE_URL + "/observations/FXUSDCAD");
  }

  @Test
  void shouldBuildUrlWithMultipleSeries() {
    String result = builder.observations("FXUSDCAD", "FXEURCAD").build();

    assertThat(result).isEqualTo(BASE_URL + "/observations/FXUSDCAD,FXEURCAD");
  }

  @Test
  void shouldAppendFormatCorrectly() {
    String result = builder.observations("FXUSDCAD").format("json").build();

    assertThat(result).isEqualTo(BASE_URL + "/observations/FXUSDCAD/json");
  }

  @Test
  void shouldAddRecentQueryParameter() {
    String result = builder.observations("FXUSDCAD").recent(5).build();

    assertThat(result).isEqualTo(BASE_URL + "/observations/FXUSDCAD?recent=5");
  }

  @Test
  void shouldConvertDatesToIsoFormat() {
    Instant start = Instant.parse("2023-01-01T10:00:00Z");
    Instant end = Instant.parse("2023-01-31T10:00:00Z");

    String result = builder.observations("FXUSDCAD").startDate(start).endDate(end).build();

    assertThat(result).contains("start_date=2023-01-01");
    assertThat(result).contains("end_date=2023-01-31");
  }

  @Test
  void shouldEncodeQueryParameters() {
    // Space becomes + or %20 depending on URLEncoder version
    String result = new BocUrlBuilder(BASE_URL).observations("FXUSDCAD")
        .startDate(Instant.parse("2023-01-01T00:00:00Z")).build();

    // Testing that the equals sign and keys are handled correctly
    assertThat(result).isEqualTo(BASE_URL + "/observations/FXUSDCAD?start_date=2023-01-01");
  }

  @Test
  void shouldHandleComplexFluentCall() {
    Instant date = Instant.parse("2024-12-25T00:00:00Z");

    String result = builder.observations("FXUSDCAD", "FXGBPCAD").format("csv").startDate(date)
        .recent(1).build();

    String expected =
        BASE_URL + "/observations/FXUSDCAD,FXGBPCAD/csv?start_date=2024-12-25&recent=1";
    assertThat(result).isEqualTo(expected);
  }
}