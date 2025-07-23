package com.laderrco.fortunelink.portfoliomanagment.application.dtos;

import java.util.Currency;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

public record PortfolioCreatedDto(
    UUID userId,
    String name
) {
    
}
