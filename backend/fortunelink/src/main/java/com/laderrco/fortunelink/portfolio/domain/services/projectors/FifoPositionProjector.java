package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public final class FifoPositionProjector extends BasePositionProjector<FifoPosition> {
  public FifoPositionProjector(AssetSymbol s, AssetType t, Currency c) {
    super(s, t, c, FifoPosition.class);
  }

  @Override
  protected FifoPosition getEmptyPosition(AssetSymbol s, AssetType t, Currency c) {
    return FifoPosition.empty(s, t, c);
  }
}
