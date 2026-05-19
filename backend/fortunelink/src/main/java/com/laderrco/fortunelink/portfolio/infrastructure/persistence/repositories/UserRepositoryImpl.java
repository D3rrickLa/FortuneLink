package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.laderrco.fortunelink.portfolio.domain.model.entities.User;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.UserRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.UserJpaEntity;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
  private final JpaUserRepository jpaRepository;

  @Override
  public Currency getBaseCurrency(UserId userId) {
    return jpaRepository.findById(userId.id())
        .map(c -> Currency.of(c.getBaseCurrency()))
        .orElse(Currency.CAD);
  }

  @Override
  public Optional<User> findById(UserId userId) {
    return Optional.of(UserJpaEntity.toDomainUser(jpaRepository.findById(userId.id()).get()));
  }

  @Override
  public User save(User user) {
    return UserJpaEntity.toDomainUser(jpaRepository.save(UserJpaEntity.toEntity(user)));
  }
}