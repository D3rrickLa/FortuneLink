package com.laderrco.fortunelink.portfolio.domain.model.entities;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {
  private final UserId userId;
  private final Instant createdAt;
  private String email;
  private Instant updatedAt;
  private Instant lastSignInAt;
}