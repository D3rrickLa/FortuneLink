package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "user_preferences")
public class UserPreferencesEntity {
  @Id
  private UUID userId;

  private String baseCurrency;

  private boolean emailNotifications;
  private boolean priceAlerts;

  private String dateFormat;
}