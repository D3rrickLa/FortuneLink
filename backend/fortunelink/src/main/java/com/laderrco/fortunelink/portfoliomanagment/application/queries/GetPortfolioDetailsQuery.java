package com.laderrco.fortunelink.portfoliomanagment.application.queries;

import java.util.UUID;

// represent requrests to yoru system to get data
public record GetPortfolioDetailsQuery(
    UUID portfolioId
) {
    
}
