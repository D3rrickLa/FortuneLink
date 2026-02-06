package com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions;

public sealed interface PositionResult permits AcbPosition.ApplyResult, FifoPosition.ApplyResult {
    Position getUpdatedPosition();
    default boolean isNoChange() {
        return this instanceof AcbPosition.ApplyResult.NoChange ||
               this instanceof FifoPosition.ApplyResult.NoChange;
    }
}
