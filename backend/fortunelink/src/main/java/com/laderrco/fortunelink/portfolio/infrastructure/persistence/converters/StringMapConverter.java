package com.laderrco.fortunelink.portfolio.infrastructure.persistence.converters;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
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
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final TypeReference<Map<String, String>> TYPE_REF = new TypeReference<>() {
  };

  @Override
  public String convertToDatabaseColumn(Map<String, String> attribute) {
    if (attribute == null || attribute.isEmpty())
      return "{}";
    try {
      return MAPPER.writeValueAsString(attribute);
    } catch (Exception e) {
      throw new IllegalStateException("Could not serialize metadata map to JSON", e);
    }
  }

  @Override
  public Map<String, String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank() || dbData.equals("{}"))
      return new HashMap<>();
    try {
      return MAPPER.readValue(dbData, TYPE_REF);
    } catch (Exception e) {
      throw new IllegalStateException("Could not deserialize metadata JSON: " + dbData, e);
    }
  }
}