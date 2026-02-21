package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

public sealed interface PositionResult permits ApplyResult {

    Position getUpdatedPosition();

    default boolean isNoChange() {
        return this instanceof ApplyResult.NoChange<?>;
    }
}
