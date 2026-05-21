package com.laderrco.fortunelink.portfolio.domain.model.valueobjects;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPreferences {
  private final UserId userId;

  private Currency baseCurrency;
  private boolean emailNotifications;
  private boolean priceAlerts;
  private String dateFormat;

  public UserPreferences(UserId userId, Currency baseCurrency, boolean emailNotifications, boolean priceAlerts,
      String dateFormat) {
    this.userId = userId;
    this.baseCurrency = baseCurrency;
    this.emailNotifications = emailNotifications;
    this.priceAlerts = priceAlerts;
    this.dateFormat = dateFormat;
  }
}