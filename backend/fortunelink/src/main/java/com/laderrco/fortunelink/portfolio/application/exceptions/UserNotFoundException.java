package com.laderrco.fortunelink.portfolio.application.exceptions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public class UserNotFoundException extends RuntimeException {

  public UserNotFoundException(UserId id) {
    super(id.id() + " does not exist");
  }

}
