package com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces;

import java.time.Period;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Portfolio;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.NetWorthSummary;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;

// About services in domain. They do operations that don't naturally fit within an Entity or VO.
// This usually means that they have actions involving multiple aggregates or perform domain logic spanning across different entities
// but are not tied to a single Lifecycle. they are our 'verbs' that act on our domain objects
public interface INetWorthCalculationService {
    NetWorthSummary calculateNetWorth(Portfolio portfolio);
    NetWorthSummary calculateProjectedNetWorth(Portfolio portfolio, int monthsAhead);
    Percentage calculateNetWorthGrowthRate(Portfolio portfolio, Period period);
}
