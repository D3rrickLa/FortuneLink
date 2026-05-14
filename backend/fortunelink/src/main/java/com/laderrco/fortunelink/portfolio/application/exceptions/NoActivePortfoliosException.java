package com.laderrco.fortunelink.portfolio.application.exceptions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public class NoActivePortfoliosException extends RuntimeException {

  public NoActivePortfoliosException(UserId userId) {
    super("No active portfolio in user id: " + userId.id().toString());
  }
  
}
