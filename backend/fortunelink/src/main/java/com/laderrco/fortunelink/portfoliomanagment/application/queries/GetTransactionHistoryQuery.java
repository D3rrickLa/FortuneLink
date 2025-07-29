package com.laderrco.fortunelink.portfoliomanagment.application.queries;

import java.time.Instant;
import java.util.UUID;

public record GetTransactionHistoryQuery(
    UUID portfolioId,
    Instant startDate, 
    Instant endDate
) {
    
}
