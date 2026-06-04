package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserProfile;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserProfileRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.UserProfileEntity;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserProfileRepositoryImpl implements UserProfileRepository {
  private final JpaUserProfileRepository jpa;

  @Override
  public Optional<UserProfile> findById(UserId userId) {
    return jpa.findById(userId.id())
        .map(e -> new UserProfile(new UserId(e.getUserId()), e.getFullName()));
  }

  @Override
  public UserProfile save(UserProfile profile) {
    UserProfileEntity e = new UserProfileEntity();
    e.setUserId(profile.getUserId().id());
    e.setFullName(profile.getFullName());

    var saved = jpa.save(e);

    return new UserProfile(new UserId(saved.getUserId()), saved.getFullName());
  }
}