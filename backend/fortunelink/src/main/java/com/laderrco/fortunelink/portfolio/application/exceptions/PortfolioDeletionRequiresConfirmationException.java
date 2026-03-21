package com.laderrco.fortunelink.portfolio.application.exceptions;

public class PortfolioDeletionRequiresConfirmationException extends RuntimeException {

  public PortfolioDeletionRequiresConfirmationException() {
    super("Portfolio deletetion requires explicit confirmation");
  }

}
