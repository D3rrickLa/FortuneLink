package com.laderrco.fortunelink.portfoliomanagment.domain.events;

import java.time.Instant;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.CashflowTransactionDetails;

public record CashflowRecordedEvent(
    PortfolioId portfolioId,
    CashflowTransactionDetails details,
    Money amount, 
    Instant timestamp
) {

}
