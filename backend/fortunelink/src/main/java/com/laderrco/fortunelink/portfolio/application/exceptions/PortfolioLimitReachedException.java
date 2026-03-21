package com.laderrco.fortunelink.portfolio.application.exceptions;

public class PortfolioLimitReachedException extends RuntimeException {
  public PortfolioLimitReachedException(String s) {
    super(s);
  }
}
