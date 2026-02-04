package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.Position;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.TaxLot;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.TransactionId;

public class Account {
    private final AccountId accountId;
    private Currency baseCurrency;
    private Map<AssetSymbol, Position> positions;
    private List<RealizedGain> realizedGains;

    public record RealizedGain(
            TransactionId transactionId,
            AssetSymbol assetSymbol,
            Money costBasis,
            Money proceeds,
            Money gainLoss,
            List<TaxLot> lotsConsumed,
            Instant soldAt) {
    }

    public Account recordTransaction(Transaction tx) {
    if (tx.isTrade()) {
        AssetSymbol symbol = tx.execution().asset();
        Position current = positions.getOrDefault(
            symbol,
            new Position(symbol, getAssetType(tx), baseCurrency, List.of())
        );
        
        Position.ApplyResult result = current.apply(tx);
        
        // Handle sale-specific logic
        if (result instanceof Position.ApplyResult.Sale sale) {
            realizedGains.add(new RealizedGain(
                tx.transactionId(),
                symbol,
                sale.costBasisRealized(),
                tx.cashDelta(),
                sale.realizedGainLoss(),
                sale.lotsConsumed(),
                tx.occurredAt()
            ));
        }
        
        Position updated = result.newPosition();
        
        if (updated.getTotalQuantity().isZero()) {
            positions.remove(symbol);
        } else {
            positions.put(symbol, updated);
        }
    }
    
    return this;
}
}
