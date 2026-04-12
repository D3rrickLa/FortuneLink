package com.laderrco.fortunelink.portfolio.infrastructure.config.serialization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;

class CaseInsensitiveEnumConverterFactoryTest {

  private final CaseInsensitiveEnumConverterFactory factory = new CaseInsensitiveEnumConverterFactory();
  enum TestStatus {
    ACTIVE,
    IN_PROGRESS,
    COMPLETED
  }

  @Test
  void shouldConvertLowerCaseToEnum() {
    Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);

    assertThat(converter.convert("active")).isEqualTo(TestStatus.ACTIVE);
    assertThat(converter.convert("in_progress")).isEqualTo(TestStatus.IN_PROGRESS);
  }

  @Test
  void shouldConvertMixedCaseToEnum() {
    Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);

    assertThat(converter.convert("CoMpLeTeD")).isEqualTo(TestStatus.COMPLETED);
  }

  @Test
  void shouldHandleWhitespace() {
    Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);

    assertThat(converter.convert("  active  ")).isEqualTo(TestStatus.ACTIVE);
  }

  @Test
  void shouldReturnNullForBlankSource() {
    Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);

    assertThat(converter.convert("")).isNull();
    assertThat(converter.convert("   ")).isNull();
  }

  @Test
  void shouldReturnNullForInvalidEnumValue() {
    Converter<String, TestStatus> converter = factory.getConverter(TestStatus.class);

    // Should catch IllegalArgumentException and return null
    assertThat(converter.convert("NOT_A_STATUS")).isNull();
  }

  @Test
  void shouldCreateConverterForDifferentEnumTypes() {
    // Just verifying the factory can handle multiple types
    enum AnotherEnum {
      YES, NO
    }

    Converter<String, AnotherEnum> converter = factory.getConverter(AnotherEnum.class);
    assertThat(converter.convert("yes")).isEqualTo(AnotherEnum.YES);
  }
}