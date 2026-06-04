package com.laderrco.fortunelink.portfolio.domain.model.valueobjects;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public class UserProfile {
  private final UserId userId;
  private String fullName;

  public UserProfile(UserId userId, String fullName) {
    this.userId = userId;
    this.fullName = fullName;
  }

  public UserId getUserId() {
    return userId;
  }

  public String getFullName() {
    return fullName;
  }

  public void updateFullName(String fullName) {
    this.fullName = fullName;
  }
}