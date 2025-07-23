package com.laderrco.fortunelink.portfoliomanagment.application.commands;

import java.util.Currency;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

// Command Class -> plain data holder
// Portfolio ApplicationService is what does the methods stuff
public record CreatePortfolioCommand(
    UUID userId, 
    String name, 
    String description,
    Money initialBalance,
    Currency currencyPerference
) {
}