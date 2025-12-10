package com.laderrco.fortunelink.portfolio_management.application.validators;

import java.util.List;

public record ValidationResult(boolean isValid, List<String> errors) {
    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(false, errors);
    } 
}
