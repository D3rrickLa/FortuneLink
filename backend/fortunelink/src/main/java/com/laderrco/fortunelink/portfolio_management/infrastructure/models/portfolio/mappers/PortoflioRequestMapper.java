package com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.mappers;

import java.util.UUID;

import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.models.portfolio.requests.CreatePortfolioRequest;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

// maps between the request to the command used in application
public class PortoflioRequestMapper {
    public CreatePortfolioCommand toCommand(CreatePortfolioRequest request) {
        return new CreatePortfolioCommand(
            toUserId(request.userId()),
            request.name(),
            toCurrency(request.currencyPreference()),
            request.description(),
            request.createAccount()
        );
    }


    private ValidatedCurrency toCurrency(String currency) {
        return ValidatedCurrency.of(currency);
    }

    private UserId toUserId(UUID id) {
        return new UserId(id);
    }
}
