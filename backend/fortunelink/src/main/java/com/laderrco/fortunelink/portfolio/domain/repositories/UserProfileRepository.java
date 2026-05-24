package com.laderrco.fortunelink.portfolio.domain.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserProfile;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.util.Optional;

public interface UserProfileRepository {
  Optional<UserProfile> findById(UserId userId);

  UserProfile save(UserProfile profile);
}