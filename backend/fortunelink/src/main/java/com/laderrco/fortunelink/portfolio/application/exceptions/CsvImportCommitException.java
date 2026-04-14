package com.laderrco.fortunelink.portfolio.application.exceptions;

// TODO add a GlobalExceptionHandler for this
public class CsvImportCommitException extends RuntimeException {
  public CsvImportCommitException(String message, Throwable cause) {
    super(message, cause);
  }
}