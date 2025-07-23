package com.laderrco.fortunelink.portfoliomanagment.application.dtos;

import java.util.Currency;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

public record PortfolioDetailsDto(
    UUID portfolioId,
    String name,
    String description,
    Money initialBalance,
    Currency currencyPreference
) {
    
}
