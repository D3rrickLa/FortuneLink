package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;

public interface HasPortfolioId extends HasUserId {
  PortfolioId portfolioId();
}