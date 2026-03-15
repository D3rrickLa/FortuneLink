package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers;

import com.laderrco.fortunelink.shared.interfaces.GenericId;
import java.util.UUID;

public record AccountId(UUID id) implements GenericId {
  public AccountId {
    GenericId.validate(id);
  }

  public static AccountId newId() {
    return GenericId.generate(AccountId::new);
  }

  public static AccountId fromString(String value) {
    return GenericId.fromString(AccountId::new, value);
  }

  @Override
  public String toString() {
    return id.toString();
  }
}