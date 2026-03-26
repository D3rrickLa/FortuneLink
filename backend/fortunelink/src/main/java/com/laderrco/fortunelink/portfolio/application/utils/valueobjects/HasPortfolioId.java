package com.laderrco.fortunelink.portfolio.application.utils.valueobjects;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;

public interface HasPortfolioId extends HasUserId {
  PortfolioId portfolioId();
}