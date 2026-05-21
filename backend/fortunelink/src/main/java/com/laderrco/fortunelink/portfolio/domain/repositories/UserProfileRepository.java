package com.laderrco.fortunelink.portfolio.domain.repositories;

import java.util.Optional;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserProfile;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public interface UserProfileRepository {
  Optional<UserProfile> findById(UserId userId);

  UserProfile save(UserProfile profile);
}