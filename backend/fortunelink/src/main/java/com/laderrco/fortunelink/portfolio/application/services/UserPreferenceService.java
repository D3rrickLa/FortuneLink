package com.laderrco.fortunelink.portfolio.application.services;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.application.exceptions.UserNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.User;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserPreferenceService {
  private final UserRepository userRepository;

  @Transactional
  public Currency getBaseCurrency(UserId userId) {
    return userRepository.findById(userId)
        .map(User::getBaseCurrency)
        .orElse(Currency.CAD); // defensive fallback only
  }

  @Transactional
  public void updateBaseCurrency(UserId userId, Currency currency) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));

    user.setUpdatedAt(Instant.now());
    user.setBaseCurrency(currency);

    userRepository.save(user); // IMPORTANT: explicit persistence
  }
}
