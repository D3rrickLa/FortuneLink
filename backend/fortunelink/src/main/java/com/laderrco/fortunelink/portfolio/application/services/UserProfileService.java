package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserProfile;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserProfileRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileService {
  private final UserProfileRepository repository;

  public String getFullName(UserId userId) {
    return repository.findById(userId).map(UserProfile::getFullName).orElse("");
  }

  public void updateFullName(UserId userId, String fullName) {
    UserProfile profile = repository.findById(userId).orElse(new UserProfile(userId, fullName));

    profile.updateFullName(fullName);

    repository.save(profile);
  }
}