package com.laderrco.fortunelink.portfolio.application.exceptions;

public class AssetNotFoundException extends RuntimeException {
  public AssetNotFoundException(String message) {
    super(message);
  }
}
