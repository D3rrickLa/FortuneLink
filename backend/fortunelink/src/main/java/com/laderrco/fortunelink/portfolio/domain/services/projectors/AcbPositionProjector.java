package com.laderrco.fortunelink.portfolio.domain.services.projectors;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public final class AcbPositionProjector extends BasePositionProjector<AcbPosition> {
  public AcbPositionProjector(AssetSymbol s, AssetType t, Currency c) {
    super(s, t, c, AcbPosition.class);
  }

  @Override
  protected AcbPosition getEmptyPosition(AssetSymbol s, AssetType t, Currency c) {
    return AcbPosition.empty(s, t, c);
  }
}
