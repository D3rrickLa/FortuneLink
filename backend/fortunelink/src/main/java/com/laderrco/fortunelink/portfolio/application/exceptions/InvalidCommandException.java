package com.laderrco.fortunelink.portfolio.application.exceptions;

import java.util.List;

public class InvalidCommandException extends RuntimeException {
  private final List<String> errors; // Store the raw data

  public InvalidCommandException(String message, List<String> errors) {
    super(String.format("%s: %s", message, errors));
    this.errors = List.copyOf(errors);
  }

  public List<String> getErrors() {
    return errors;
  }

}
