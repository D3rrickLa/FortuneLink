package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreateAccountRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreatePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.DeletePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetUsersPortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

// maps between the request to the command used in application
@Component
public class PortoflioHttpMapper {
    public CreatePortfolioCommand toCommand(CreatePortfolioRequest request) {
        return new CreatePortfolioCommand(
            toUserId(request.getUserId()),
            request.getName(),
            toCurrency(request.getCurrencyPreference()),
            request.getDescription(),
            request.getCreateAccount()
        );
    }

    public GetPortfolioByIdQuery toCommand(String id) {
        return new GetPortfolioByIdQuery(new PortfolioId(UUID.fromString(id)));
    }

    public GetPortfoliosByUserIdQuery toCommand(GetUsersPortfolioRequest request) {
        return new GetPortfoliosByUserIdQuery(new UserId(UUID.fromString(request.userId())));
    }

    public UpdatePortfolioCommand toCommand(String id, CreatePortfolioRequest request) {
        return new UpdatePortfolioCommand(
            toPortfolioId(id), 
            request.getName(),
            toCurrency(request.getCurrencyPreference()), 
            request.getDescription()
        );
    }
    
    public DeletePortfolioCommand toCommand(DeletePortfolioRequest request) {
        return new DeletePortfolioCommand(
            toPortfolioId(request.getPortfolioId()),
            toUserId(request.getUserId()),
            request.getConfirmed(),
            request.getSoftDelete()
        );
    }

    public AddAccountCommand toCommand(String id, CreateAccountRequest request) {
        return new AddAccountCommand(
            toPortfolioId(id),
            request.getName(),
            AccountType.valueOf(request.getAccountType()),
            toCurrency(request.getBaseCurrency())
        );
    }

    public RemoveAccountCommand toCommand(String portoflioId, String accountId) {
        return new RemoveAccountCommand(toPortfolioId(accountId), toAccountId(accountId));
    }

    private ValidatedCurrency toCurrency(String currency) {
        return ValidatedCurrency.of(currency);
    }

    private UserId toUserId(String id) {
        return new UserId(UUID.fromString(id));
    }

    private PortfolioId toPortfolioId(String id) {
        return new PortfolioId(UUID.fromString(id));
    }

    private AccountId toAccountId(String id) {
        return new AccountId(UUID.fromString(id));
    }


}
