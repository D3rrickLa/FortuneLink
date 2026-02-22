package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

public interface PortfolioLifecycleCommand {
    UserId userId();
}
