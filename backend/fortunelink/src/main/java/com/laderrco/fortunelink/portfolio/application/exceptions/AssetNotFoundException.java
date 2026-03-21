package com.laderrco.fortunelink.portfolio.application.exceptions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public class AssetNotFoundException extends RuntimeException {

  public AssetNotFoundException(String message) {
    super(message);
  }

  public AssetNotFoundException(AssetSymbol assetSymbol) {
    super("Not position found for sybmol: " + assetSymbol.symbol());
  }

}
