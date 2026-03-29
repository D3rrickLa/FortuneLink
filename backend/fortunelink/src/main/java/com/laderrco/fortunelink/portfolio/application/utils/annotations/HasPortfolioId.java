package com.laderrco.fortunelink.portfolio.application.utils.annotations;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;

public interface HasPortfolioId extends HasUserId {
  PortfolioId portfolioId();
}