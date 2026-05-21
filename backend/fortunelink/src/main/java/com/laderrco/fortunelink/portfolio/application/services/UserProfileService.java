package com.laderrco.fortunelink.portfolio.application.services;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserProfile;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserProfileRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

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