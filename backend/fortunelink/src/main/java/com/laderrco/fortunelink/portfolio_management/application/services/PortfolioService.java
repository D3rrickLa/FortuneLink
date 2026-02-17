package com.laderrco.fortunelink.portfolio_management.application.services;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioDeletionRequiresConfirmationException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio_management.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio_management.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio_management.application.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// combo of the old app service and query service
// ONLY PORTFOLIO + ACCOUNT LIFECYCLE STUFF, NOT TRANSACTION
@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioLifecycleCommandValidator validator;
    private final PortfolioRepository portfolioRepository;
    private final PortfolioViewMapper portfolioViewMapper;

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
                    Currency.of(command.locale()), // we should pull from the 'web' as it could work only on my machine
                    PositionStrategy.LIFO);
        }

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return portfolioViewMapper.toPortfolioView(savedPortfolio, Locale.of(command.locale()));

    }

    public void updatePortfolio(UpdatePortfolioCommand command) {
        ValidationResult result = validator.validate(command);
        if (!result.isValid()) {
            throw new InvalidCommandException("Invalid update portfolio command", result.errors());
        }
        Optional<Portfolio> existingPortfolio = portfolioRepository.findById(command.id());

        Portfolio updatePortfolio = existingPortfolio.orElseThrow(
                () -> new PortfolioNotFoundException("Cannot find portfolio with id: " + command.id()));

        updatePortfolio.updateDetails(command.name(), command.description());
        updatePortfolio = portfolioRepository.save(updatePortfolio);

    }
    

    public void deletePortfolio(DeletePortfolioCommand command) {
        ValidationResult validationResult = validator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidCommandException("Invalid delete portfolio command", validationResult.errors());
        }

        if (!command.confirmed()) {
            throw new PortfolioDeletionRequiresConfirmationException();
        }

        Portfolio portfolio = portfolioRepository.findById(command.portfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId().toString()));


        portfolio.markAsDeleted(command.userId());

        if (command.softDelete()) {
            portfolio.markAsDeleted(command.userId());
        }
        else {
            portfolioRepository.delete(command.portfolioId());
        }

    }

    public AccountView createAccount(AddAccountCommand command) {
        ValidationResult validationResult = validator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidCommandException("Invalid create account command", validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(), command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found or access denied for ID: " + command.portfolioId()));

        Account account = portfolio.createAccount(
                command.accountName(),
                command.accountType(),
                command.baseCurrency(),
                command.strategy());

        portfolioRepository.save(portfolio);

        return portfolioViewMapper.toNewAccountView(account);
    }

    public void updateAccount(UpdateAccountCommand command) {
        ValidationResult validationResult = validator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidCommandException("Invalid update account command", validationResult.errors());
        }

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(), command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found or access denied for ID: " + command.portfolioId()));

        portfolio.renameAccount(command.accountId(), command.accountName());

        portfolioRepository.save(portfolio);
    }

    public void deleteAccount(DeleteAccountCommand command) {
        ValidationResult validationResult = validator.validate(command);
        if (!validationResult.isValid()) {
            throw new InvalidCommandException("Invalid delete account command", validationResult.errors());
        }
        // always soft deletes
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(), command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(
                        "Portfolio not found or access denied for ID: " + command.portfolioId()));

        portfolio.closeAccount(command.accountId());
    }

    // QUERY METHODS //
}
