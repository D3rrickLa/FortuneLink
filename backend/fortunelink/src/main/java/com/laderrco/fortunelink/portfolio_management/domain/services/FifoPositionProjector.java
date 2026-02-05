package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.util.Comparator;
import java.util.List;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.positions.FifoPosition.ApplyResult;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

public class FifoPositionProjector implements Projector<FifoPosition, Transaction> {
    private final AssetSymbol symbol;
    private final AssetType type;
    private final Currency accountCurrency;

    public FifoPositionProjector(
            AssetSymbol symbol,
            AssetType type,
            Currency accountCurrency) {
        this.symbol = symbol;
        this.type = type;
        this.accountCurrency = accountCurrency;
    }

    @Override
    public FifoPosition project(List<Transaction> transactions) {
        FifoPosition current = FifoPosition.empty(symbol, type, accountCurrency);

        List<Transaction> sorted = transactions.stream()
                .sorted(Comparator.comparing(tx -> tx.occurredAt().timestamp()))
                .toList();

        for (Transaction tx : sorted) {
            // apply returns to an ApplyResult, so we extract the position from it
            ApplyResult result = current.apply(tx);
            current = result.newPosition();
        }

        return current;
    }
}
