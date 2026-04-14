package com.laderrco.fortunelink.portfolio.infrastructure.persistence.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@DisplayName("StringMapConverter Unit Tests")
class StringMapConverterTest {

  private StringMapConverter converter;

  @BeforeEach
  void setUp() {
    converter = new StringMapConverter();
  }

  @Test
  @DisplayName("Round-trip: Entity -> DB -> Entity should preserve data")
  void roundTripTest() {
    Map<String, String> original = Map.of("id", "123", "type", "test");

    String dbColumn = converter.convertToDatabaseColumn(original);
    Map<String, String> result = converter.convertToEntityAttribute(dbColumn);

    assertThat(result).isEqualTo(original);
  }

  @Nested
  @DisplayName("convertToDatabaseColumn (Serialization)")
  class Serialization {

    @Test
    @DisplayName("should return null when map is null")
    void shouldReturnNullWhenMapIsNull() {
      assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    @DisplayName("should convert a populated map to a JSON string")
    void shouldConvertMapToJson() {
      Map<String, String> data = Map.of("key1", "value1", "key2", "value2");
      String json = converter.convertToDatabaseColumn(data);

      assertThat(json).contains("\"key1\":\"value1\"");
      assertThat(json).contains("\"key2\":\"value2\"");
    }
  }

  @Nested
  @DisplayName("convertToEntityAttribute (Deserialization)")
  class Deserialization {

    @Test
    @DisplayName("should return an empty HashMap when DB data is null or empty")
    void shouldReturnEmptyMapWhenDbDataIsNullOrEmpty() {
      assertThat(converter.convertToEntityAttribute(null)).isEmpty();
      assertThat(converter.convertToEntityAttribute("")).isEmpty();
    }

    @Test
    @DisplayName("should convert a valid JSON string back to a Map")
    void shouldConvertJsonToMap() {
      String json = "{\"tag\":\"growth\",\"source\":\"manual\"}";
      Map<String, String> result = converter.convertToEntityAttribute(json);

      assertThat(result).hasSize(2).containsEntry("tag", "growth")
          .containsEntry("source", "manual");
    }

    @Test
    @DisplayName("should throw RuntimeException when JSON is invalid")
    void shouldThrowExceptionOnInvalidJson() {
      String invalidJson = "{not-json}";

      assertThatThrownBy(() -> converter.convertToEntityAttribute(invalidJson)).isInstanceOf(
          RuntimeException.class).hasMessageContaining("JSON decoding error");
    }
  }

  @DisplayName("StringMapConverter Error Handling")
  class StringMapConverterExceptionTest {

    @Test
    @DisplayName("convertToDatabaseColumn should wrap JacksonException in RuntimeException")
    void convertToDatabaseColumnShouldThrowRuntimeExceptionOnJacksonError()
        throws JacksonException {

      StringMapConverter converter = new StringMapConverter();

      ObjectMapper mockMapper = mock(ObjectMapper.class);

      ReflectionTestUtils.setField(converter, "mapper", mockMapper);

      JacksonException mockException = mock(JacksonException.class);
      when(mockMapper.writeValueAsString(any())).thenThrow(mockException);

      assertThatThrownBy(
          () -> converter.convertToDatabaseColumn(Map.of("key", "value"))).isInstanceOf(
          RuntimeException.class).hasMessage("JSON encoding error").hasCause(mockException);
    }

    @Test
    @DisplayName("convertToEntityAttribute should wrap JacksonException in RuntimeException")
    void convertToEntityAttributeShouldThrowRuntimeExceptionOnJacksonError()
        throws JacksonException {

      StringMapConverter converter = new StringMapConverter();
      ObjectMapper mockMapper = mock(ObjectMapper.class);
      ReflectionTestUtils.setField(converter, "mapper", mockMapper);

      JacksonException mockException = mock(JacksonException.class);
      when(mockMapper.readValue(anyString(), any(TypeReference.class))).thenThrow(mockException);

      assertThatThrownBy(
          () -> converter.convertToEntityAttribute("{\"key\":\"value\"}")).isInstanceOf(
          RuntimeException.class).hasMessage("JSON decoding error").hasCause(mockException);
    }
  }
}