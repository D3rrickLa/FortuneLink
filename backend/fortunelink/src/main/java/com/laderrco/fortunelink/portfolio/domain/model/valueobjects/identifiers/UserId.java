package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers;

import com.laderrco.fortunelink.shared.interfaces.GenericId;
import jakarta.annotation.Nonnull;
import java.util.UUID;

public record UserId(UUID id) implements GenericId {
  public UserId {
    GenericId.validate(id);
  }

  public static UserId random() {
    return GenericId.generate(UserId::new);
  }

  public static UserId fromString(String value) {
    return GenericId.fromString(UserId::new, value);
  }

  @Override
  public String toString() {
    return id.toString();
  }
}