package com.laderrco.fortunelink.portfolio.domain.model.valueobjects;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserPreferences {
  private final UserId userId;

  private Currency baseCurrency;
  private boolean emailNotifications;
  private boolean priceAlerts;
  private String dateFormat;
  private String timeZone; // e.g. "America/Toronto"

  public UserPreferences(UserId userId, Currency baseCurrency, boolean emailNotifications,
      boolean priceAlerts, String dateFormat, String timeZone) {
    this.userId = userId;
    this.baseCurrency = baseCurrency;
    this.emailNotifications = emailNotifications;
    this.priceAlerts = priceAlerts;
    this.dateFormat = dateFormat;
    this.timeZone = timeZone;
  }
}