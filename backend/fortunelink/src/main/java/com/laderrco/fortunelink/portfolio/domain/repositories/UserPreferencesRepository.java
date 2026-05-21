package com.laderrco.fortunelink.portfolio.domain.repositories;

import java.util.Optional;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public interface UserPreferencesRepository {
  Optional<UserPreferences> findById(UserId userId);
  boolean existsById(UserId userId);
  UserPreferences save(UserPreferences userPreferences);
}
