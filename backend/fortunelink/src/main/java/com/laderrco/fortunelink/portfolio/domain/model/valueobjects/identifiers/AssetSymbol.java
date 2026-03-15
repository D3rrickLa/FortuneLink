package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

// BEFORE WE HAD A @Embeddable, don't use that here, instead we have a JPA converter in the infra layer, looks something like this:
/*
Converter(autoApply = true)
public class AssetSymbolConverter implements AttributeConverter<AssetSymbol, String> {
    @Override
    public String convertToDatabaseColumn(AssetSymbol attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public AssetSymbol convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AssetSymbol.of(dbData);
    }
}
*/
public record AssetSymbol(String symbol) {
  public AssetSymbol {
    notNull(symbol, "Symbol");
    symbol = normalizeAndValidate(symbol);
  }

  private static String normalizeAndValidate(String raw) {
    String trimmed = raw.trim().toUpperCase();

    if (!trimmed.matches("^[A-Z0-9.-]+$")) {
      throw new IllegalArgumentException(
          "Symbol must contain only letters, numbers, dots, and hyphens: " + raw);
    }

    if (trimmed.length() > 20) {
      throw new IllegalArgumentException("Symbol too long (max 20 characters): " + raw);
    }

    return trimmed;
  }

  public String value() {
    return symbol;
  }
}