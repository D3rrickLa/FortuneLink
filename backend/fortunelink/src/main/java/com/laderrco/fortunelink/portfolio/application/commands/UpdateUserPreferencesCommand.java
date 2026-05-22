package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;

public record UpdateUserPreferencesCommand(
    Currency baseCurrency,
    boolean emailNotifications,
    boolean priceAlerts,
    String dateFormat) {
}