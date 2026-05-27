package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.application.commands.UpdateUserPreferencesCommand;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserPreferencesRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UserPreferencesService {
  private final UserPreferencesRepository repository;

  public UserPreferences get(UserId userId) {
    return repository.findById(userId)
        .orElseThrow(() -> new IllegalStateException("Preferences not initialized"));
  }

  public void updatePreferences(UserId userId, UpdateUserPreferencesCommand command) {
    UserPreferences prefs = get(userId);

    prefs.setBaseCurrency(command.baseCurrency());
    prefs.setEmailNotifications(command.emailNotifications());
    prefs.setPriceAlerts(command.priceAlerts());
    prefs.setDateFormat(command.dateFormat());

    repository.save(prefs);
  }

  public UserPreferences createDefault(UserId userId) {
    if (repository.existsById(userId)) {
      return get(userId);
    }

    UserPreferences defaultPrefs = new UserPreferences(userId, Currency.CAD, true, true,
        "MM/DD/YYYY");

    return repository.save(defaultPrefs);
  }
}