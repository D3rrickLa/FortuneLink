package com.laderrco.fortunelink.portfolio.infrastructure.persistence.converters;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Converts {@code Map<String,String>} to/from a JSON string for JSONB columns.
 * <p>
 * Uses Jackson directly rather than a Hibernate-Types library dependency,
 * keeping the dependency footprint minimal. Jackson is already on the
 * classpath via Spring Boot.
 */
@Converter
public class StringMapConverter implements AttributeConverter<Map<String, String>, String> {
  private final ObjectMapper mapper = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(Map<String, String> attribute) {
    if (attribute == null)
      return null;
    try {
      return mapper.writeValueAsString(attribute);
    } catch (JacksonException e) {
      throw new RuntimeException("JSON encoding error", e);
    }
  }

  @Override
  public Map<String, String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty())
      return new HashMap<>();
    try {
      return mapper.readValue(dbData, new TypeReference<>() {
      });
    } catch (JacksonException e) {
      throw new RuntimeException("JSON decoding error", e);
    }
  }
}