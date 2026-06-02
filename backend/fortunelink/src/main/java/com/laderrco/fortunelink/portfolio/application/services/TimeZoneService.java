package com.laderrco.fortunelink.portfolio.application.services;

import java.time.ZoneId;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

@Service
public class TimeZoneService {
  public ZoneId resolve(UserId userId, UserPreferences prefs) {
    if (prefs != null && prefs.getTimeZone() != null) {
      return ZoneId.of(prefs.getTimeZone());
    }

    return ZoneId.of("America/Toronto"); // fallback
  }
}