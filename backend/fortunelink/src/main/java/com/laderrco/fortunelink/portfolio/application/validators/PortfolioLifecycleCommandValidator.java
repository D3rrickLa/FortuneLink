package com.laderrco.fortunelink.portfolio.application.validators;

import com.laderrco.fortunelink.portfolio.application.commands.*;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PortfolioLifecycleCommandValidator {
  private static final int ACCOUNT_NAME_LENGTH = 100;

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

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(UpdatePortfolioCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validatePortfolioAndUserIds(command, errors);

    if (command.name() == null) {
      errors.add("Portfolio name is required");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(DeletePortfolioCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validatePortfolioAndUserIds(command, errors);

    if (!command.confirmed()) {
      errors.add("Confirm delete cannot be false");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(CreateAccountCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validatePortfolioAndUserIds(command, errors);

    if (command.accountName() == null || command.accountName().trim().isEmpty()) {
      errors.add("Account name is required");
    } else if (command.accountName().length() > ACCOUNT_NAME_LENGTH) {
      errors.add("Account name must be 100 characters or less");
    }

    if (command.accountType() == null) {
      errors.add("Account type is required");
    }

    if (command.baseCurrency() == null) {
      errors.add("Base currency is required");
    } else if (!ValidationUtils.isValidCurrency(command.baseCurrency().getCode())) {
      errors.add("Invalid currency code");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(UpdateAccountCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validatePortfolioAndUserIds(command, errors);

    if (command.accountId() == null) {
      errors.add("AccountId is required");
    }

    if (command.accountName() == null || command.accountName().trim().isEmpty()) {
      errors.add("Account name is required");
    } else if (command.accountName().length() > ACCOUNT_NAME_LENGTH) {
      errors.add("Account name must be 100 characters or less");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(ReopenAccountCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validatePortfolioAndUserIds(command, errors);

    if (command.accountId() == null) {
      errors.add("AccountId is required");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(DeleteAccountCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validatePortfolioAndUserIds(command, errors);

    if (command.accountId() == null) {
      errors.add("AccountId is required");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

    private void validatePortfolioAndUserIds(HasPortfolioId command, List<String> errors) {
    if (command.portfolioId() == null) {
      errors.add("PortfolioId is required");
    }

    if (command.userId() == null) {
      errors.add("UserId is required");
    }
  }
}
