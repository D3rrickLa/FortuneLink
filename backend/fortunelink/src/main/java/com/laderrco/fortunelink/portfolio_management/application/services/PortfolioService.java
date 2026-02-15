package com.laderrco.fortunelink.portfolio_management.application.services;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio_management.application.validators.CommandValidator;
import com.laderrco.fortunelink.portfolio_management.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// combo of the old app service and query service
// ONLY PORTFOLIO LIFECYCLE STUFF, NOT TRANSACTION
@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;
    private final CommandValidator validator;

    public PortfolioView createPortfolio(CreatePortfolioCommand command) {
        ValidationResult result = validator.validate(command);
        if (!result.isValid()) {
            throw new InvalidCommandException("Invalid create portfolio command", result.errors());
        }
        long currentCount = portfolioRepository.countByUserId(command.userId());
        int maxProfileAllowed = 1;
        if (currentCount >= maxProfileAllowed) {
            // throws
        }

        Portfolio portfolio = Portfolio.createNew(command.userId(), command.name(), command.description());

        if (command.createDefaultAccount()) {

            portfolio.createAccount(
                    "Default Name",
                    AccountType.NON_REGISTERED_INVESTMENT,
                    Currency.of(Locale.getDefault().toString()), // we should pull from the 'web' as it could work only on my machine
                    PositionStrategy.LIFO);
        }

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return 

    }

    public void updatePortfolio() {
    }

    public void deletePortfolio() {
    }

    public void createAccount(AddAccountCommand command) {
    }

    public void updateAccount() {
    }

    public void deleteAccount() {
    }
}
