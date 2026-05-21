package com.laderrco.fortunelink.portfolio.application.services;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.application.exceptions.UserNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.User;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserProfileService {
  private final UserRepository userRepository;

  public String getProfile(UserId userId) {
    return userRepository.findById(userId)
        .map(User::getFullname)
        .orElse("");
  }

  public void updateFullName(UserId userId, String name) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    user.setUpdatedAt(Instant.now());
    user.setFullname(name.strip());

    userRepository.save(user);
  }
}
