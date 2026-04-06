package com.laderrco.fortunelink.portfolio.infrastructure.config.serialization;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

public class CaseInsensitiveEnumConverterFactory implements ConverterFactory<String, Enum<?>> {

  @Override
  public <T extends Enum<?>> Converter<String, T> getConverter(Class<T> targetType) {
    return new CaseInsensitiveEnumConverter<>(targetType);
  }

  private static class CaseInsensitiveEnumConverter<T extends Enum<?>> implements Converter<String, T> {
    private final Class<T> enumType;

    public CaseInsensitiveEnumConverter(Class<T> enumType) {
      this.enumType = enumType;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public T convert(String source) {
      if (source.isBlank()) {
        return null;
      }
      try {
        // This does the heavy lifting: forced uppercase match
        return (T) Enum.valueOf((Class) enumType, source.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        return null; // Spring will throw a 400 Bad Request
      }
    }
  }
}