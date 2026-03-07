package com.laderrco.fortunelink.portfolio.domain.services;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.utils.TradeValueResolver;

import org.springframework.stereotype.Component;

@Component
public class CraMethodResolver implements TradeValueResolver {

    @Override
    public Money buyerCost(Transaction tx) {
        // For a BUY, cashDelta is negative. We want the positive total cost.
        return tx.cashDelta().abs();
    }

    @Override
    public Money sellerProceeds(Transaction tx) {
        // For a SELL, cashDelta is already the net amount after fees.
        return tx.cashDelta();
    }
}
