package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

// value object, make it a record
public class PortfolioCurrency {
    private String code;
    private String symbol;

    public PortfolioCurrency(String code, String symbol) {
        this.code = code;
        this.symbol = symbol;
    }
}
