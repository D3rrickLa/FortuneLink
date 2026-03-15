package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers;

import com.laderrco.fortunelink.shared.interfaces.GenericId;
import java.util.UUID;

public record TransactionId(UUID id) implements GenericId {
  public TransactionId {
    GenericId.validate(id);
  }

  public static TransactionId newId() {
    return GenericId.generate(TransactionId::new);
  }

  public static TransactionId fromString(String value) {
    return GenericId.fromString(TransactionId::new, value);
  }

  @Override
  public String toString() {
    return id.toString();
  }
}