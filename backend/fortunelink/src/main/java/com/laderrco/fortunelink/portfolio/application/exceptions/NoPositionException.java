package com.laderrco.fortunelink.portfolio.application.exceptions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public class NoPositionException extends RuntimeException {
  public NoPositionException(AssetSymbol symbol, AccountId accountId) {
    super(String.format("Position for symbol %s cannot be found in account: %s", symbol.symbol(),
        accountId.id()));
  }
}
