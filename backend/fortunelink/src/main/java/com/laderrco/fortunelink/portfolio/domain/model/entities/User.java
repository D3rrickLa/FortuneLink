package com.laderrco.fortunelink.portfolio.domain.model.entities;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {
  private final UserId userId;
  private String email;
  private final Instant createdAt;
  private Instant updatedAt;
  private Instant lastSignInAt;
}