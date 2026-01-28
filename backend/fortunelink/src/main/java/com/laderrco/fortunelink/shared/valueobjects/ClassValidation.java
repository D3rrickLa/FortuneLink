package com.laderrco.fortunelink.shared.valueobjects;

import java.util.Objects;

public interface ClassValidation {
    static <T> T validateParameter(T value) {
        return Objects.requireNonNull(value, "Parameter cannot be null");
    }

    static <T> T validateParameter(T value, String name) {
        return Objects.requireNonNull(value, String.format("%s cannot be null", name));
    }
}