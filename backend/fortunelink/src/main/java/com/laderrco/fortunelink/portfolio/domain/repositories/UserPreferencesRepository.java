package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.util.Optional;

public interface UserPreferencesRepository {
  Optional<UserPreferences> findById(UserId userId);

  boolean existsById(UserId userId);

  UserPreferences save(UserPreferences userPreferences);
}
