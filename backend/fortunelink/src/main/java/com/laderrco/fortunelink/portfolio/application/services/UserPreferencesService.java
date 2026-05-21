package com.laderrco.fortunelink.portfolio.application.services;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserPreferencesRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class UserPreferencesService {
  private final UserPreferencesRepository userPreferencesRepository;

  public UserPreferences get(UserId userId) {
    return userPreferencesRepository.findById(userId)
        .orElseThrow(() -> new IllegalStateException("Preferences not initialized"));
  }

  public Currency getBaseCurrency(UserId userId) {
    return get(userId).getBaseCurrency();
  }

  public void updateBaseCurrency(UserId userId, Currency currency) {
    UserPreferences prefs = get(userId);
    prefs.setBaseCurrency(currency);
    userPreferencesRepository.save(prefs);
  }

  public void createDefault(UserId userId) {
    if (userPreferencesRepository.existsById(userId)) return;

    userPreferencesRepository.save(
        new UserPreferences(
            userId,
            Currency.CAD,
            true,
            true,
            "MM/DD/YYYY"
        )
    );
  }
}