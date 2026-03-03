package com.laderrco.fortunelink.portfolio.domain.utils;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;

public interface TaxMethodResolver {
    /**
     * CRA: Cost = Gross Price + Commissions/Fees.
     * Calculated as the absolute value of the cash outflow.
     */
    Money buyerCost(Transaction tx);

    /**
     * CRA: Proceeds = Gross Sale - Commissions/Fees.
     * Calculated as the net cash inflow.
     */
    Money sellerProceeds(Transaction tx);
}
