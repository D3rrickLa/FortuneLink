package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserPreferencesRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserPreferencesRepositoryImpl implements UserPreferencesRepository {
  private final JpaUserRepository jpaRepository;

  @Override
  public Currency getBaseCurrency(UserId userId) {
    return jpaRepository.findById(userId.id())
        .map(c -> Currency.of(c.getBaseCurrency()))
        .orElse(Currency.CAD);
  }
}