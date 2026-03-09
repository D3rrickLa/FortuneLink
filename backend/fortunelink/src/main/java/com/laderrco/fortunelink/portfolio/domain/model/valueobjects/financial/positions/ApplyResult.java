package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;

public sealed interface ApplyResult<P extends Position> extends PositionResult {
    P newPosition();

    @Override
    default Position getUpdatedPosition() {
        return newPosition();
    }

    record Purchase<P extends Position>(P newPosition) implements ApplyResult<P> {
    }

    record Sale<P extends Position>(P newPosition, Money costBasisSold, Money realizedGainLoss)
            implements ApplyResult<P> {
    }

    public record RocAdjustment<P extends Position>(P newPosition, Money exxcessCaptialGain)
            implements ApplyResult<P> {
    }

    record Adjustment<P extends Position>(P newPosition) implements ApplyResult<P> {
    }

    record NoChange<P extends Position>(P newPosition) implements ApplyResult<P> {
    }

    record ClosedPosition<P extends Position>(P newPosition, RealizedGainRecord gain)
            implements ApplyResult<P> {
    }

}