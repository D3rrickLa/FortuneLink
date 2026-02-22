package com.laderrco.fortunelink.portfolio.application.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;

@Component
public class PortfolioLifecycleCommandValidator {
    private static final int ACCOUNT_NAME_LENGTH = 100;

    public ValidationResult validate(CreateAccountCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }

        if (command.accountName() == null || command.accountName().trim().isEmpty()) {
            errors.add("Account name is required");
        } else if (command.accountName().length() > 100) {
            errors.add("Account name must be 100 characters or less");
        }

        if (command.accountType() == null) {
            errors.add("Account type is required");
        } else if (!isValidAccountType(command.accountType().name())) {
            errors.add("Invalid account type");
        }

        if (command.baseCurrency() == null) {
            errors.add("Base currency is required");
        } else if (!ValidationUtils.isValidCurrency(command.baseCurrency().getCode())) {
            errors.add("Invalid currency code");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(UpdateAccountCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        // todo, can probably group this stuff
        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }

        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }

        if (command.userId() == null) {
            errors.add("UserId is required");
        }

        if (command.accountName() == null || command.accountName().trim().isEmpty()) {
            errors.add("Account name is required");
        } else if (command.accountName().length() > ACCOUNT_NAME_LENGTH) {
            errors.add("Account name must be 100 characters or less");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(DeleteAccountCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }

        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);

    }

    public ValidationResult validate(CreatePortfolioCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.userId() == null) {
            errors.add("UserId is required");
        }

        if (command.name() == null) {
            errors.add("Portfolio name is required");
        }

        if (command.currency() == null) {
            errors.add("Currency is required");
        }

        if (command.createDefaultAccount() && command.defaultStrategy() == null) {
            errors.add("Position strategy is required when createDefaultAccount is true");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(UpdatePortfolioCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.userId() == null) {
            errors.add("UserId is required");
        }

        if (command.name() == null) {
            errors.add("Portfolio name is required");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(DeletePortfolioCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }

        if (command.userId() == null) {
            errors.add("UserId is required");
        }

        if (command.confirmed() == false && command.softDelete()) {
            errors.add("Cannot soft delete without confirming");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    private boolean isValidAccountType(String accountType) {
        try {
            AccountType.valueOf(accountType);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

}
