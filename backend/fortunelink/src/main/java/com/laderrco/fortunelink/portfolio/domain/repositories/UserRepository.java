package com.laderrco.fortunelink.portfolio.domain.repositories;

import java.util.Optional;

import com.laderrco.fortunelink.portfolio.domain.model.entities.User;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public interface UserRepository {
  User save(User user);
  Optional<User> findById(UserId userId);
  Currency getBaseCurrency(UserId userId);
}
