package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserPreferencesRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.UserPreferencesEntity;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserPreferencesRepositoryImpl implements UserPreferencesRepository {
  private final JpaUserPreferencesRepository jpaRepository;

  @Override
  public Optional<UserPreferences> findById(UserId userId) {
    return jpaRepository.findById(userId.id()).map(this::toDomain);
  }

  @Override
  public boolean existsById(UserId userId) {
    return jpaRepository.existsById(userId.id());
  }

  @Override
  public UserPreferences save(UserPreferences prefs) {
    var entity = toEntity(prefs);
    var saved = jpaRepository.save(entity);
    return toDomain(saved);
  }

  // ── mapping ─────────────────────────────

  private UserPreferences toDomain(UserPreferencesEntity e) {
    return new UserPreferences(
      new UserId(e.getUserId()), 
      Currency.of(e.getBaseCurrency()),
      e.isEmailNotifications(),
      e.isPriceAlerts(),
      e.getDateFormat());
  }

  private UserPreferencesEntity toEntity(UserPreferences d) {
    UserPreferencesEntity e = new UserPreferencesEntity();
    e.setUserId(d.getUserId().id());
    e.setBaseCurrency(d.getBaseCurrency().getCode());
    e.setEmailNotifications(d.isEmailNotifications());
    e.setPriceAlerts(d.isPriceAlerts());
    e.setDateFormat(d.getDateFormat());
    return e;
  }
}