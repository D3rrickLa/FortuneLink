package com.laderrco.fortunelink.portfolio_management.application.services;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioDeletionException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioDeletionRequiresConfirmationException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioLimitReachedException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio_management.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio_management.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio_management.application.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// ONLY PORTFOLIO + ACCOUNT LIFECYCLE STUFF, NOT TRANSACTION
@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioLifecycleService {
    private final PortfolioRepository portfolioRepository;
    private final PortfolioViewMapper portfolioViewMapper;
    private final PortfolioLifecycleCommandValidator validator;

    private static final int MAX_PORTFOLIOS_PER_USER = 1; // MVP constraint
    private static final String DEFAULT_NAME = "Default Account";

    public PortfolioView createPortfolio(CreatePortfolioCommand command) {
        ValidationResult result = validator.validate(command);
        if (!result.isValid()) {
            throw new InvalidCommandException("Invalid create portfolio command", result.errors());
        }
        long currentCount = portfolioRepository.countByUserId(command.userId());

        if (currentCount >= MAX_PORTFOLIOS_PER_USER) {
            throw new PortfolioLimitReachedException("Already reached max allowed portfolio limit");
        }

        Portfolio portfolio = Portfolio.createNew(command.userId(), command.name(), command.description());

        if (command.createDefaultAccount()) {

            portfolio.createAccount(
                    DEFAULT_NAME,
                    AccountType.NON_REGISTERED_INVESTMENT,
                    command.currency(),
                    command.defaultStrategy());
        }

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return portfolioViewMapper.toPortfolioView(savedPortfolio, command.currency());

    }

    public PortfolioView updatePortfolio(UpdatePortfolioCommand command) {
        ValidationResult result = validator.validate(command);
        if (!result.isValid()) {
            throw new InvalidCommandException("Invalid update portfolio command", result.errors());
        }
        Optional<Portfolio> existingPortfolio = portfolioRepository.findById(command.id());

        Portfolio updatePortfolio = existingPortfolio.orElseThrow(
                () -> new PortfolioNotFoundException("Cannot find portfolio with id: " + command.id()));

        updatePortfolio.updateDetails(command.name(), command.description());
        portfolioRepository.save(updatePortfolio);

        return portfolioViewMapper.toPortfolioView(updatePortfolio, command.currency());

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

        if (command.softDelete()) {
            try {
                portfolio.markAsDeleted(command.userId());
                portfolioRepository.save(portfolio);

            } catch (IllegalStateException e) {
                if (e.getMessage().contains("Portfolio is already deleted")) {
                    throw new PortfolioDeletionException("Portfolio already deleted");
                } else {
                    throw new PortfolioNotEmptyException("Portfolio is not empty and can't be deleted");
                }
            }
        } else {
            portfolioRepository.delete(command.portfolioId());
            portfolioRepository.save(portfolio);
        }

    }

    public AccountView createAccount(CreateAccountCommand command) {
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
}
