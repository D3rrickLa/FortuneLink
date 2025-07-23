package com.laderrco.fortunelink.portfoliomanagment.application.commands;

import java.time.Instant;
import java.util.UUID;

public record AccrueInterestCommand(
    UUID portfolioId,
    Instant accuralDate
) {
    
}
