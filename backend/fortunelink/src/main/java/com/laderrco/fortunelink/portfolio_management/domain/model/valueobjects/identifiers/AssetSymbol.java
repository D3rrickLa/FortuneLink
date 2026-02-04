package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers;

import com.laderrco.fortunelink.portfolio_management.shared.ClassValidation;

// BEFORE WE HAD A @Embeddable, don't use that here, instead we have a JPA converter in the infra layer, looks something liek this:
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
public record AssetSymbol(String symbol) implements ClassValidation {

    public AssetSymbol {
        ClassValidation.validateParameter(symbol, "Symbol cannot be null or blank");
        symbol = normalizeAndValidate(symbol);
    }

    public String value() {
        return symbol;
    }

    private static String normalizeAndValidate(String raw) {
        String trimmed = raw.trim().toUpperCase();

        if (!trimmed.matches("^[A-Z0-9.-]+$")) {
            throw new IllegalArgumentException(
                    "Symbol must contain only letters, numbers, dots, and hyphens: " + raw);
        }

        if (trimmed.length() > 20) {
            throw new IllegalArgumentException(
                    "Symbol too long (max 20 characters): " + raw);
        }

        return trimmed;
    }
}