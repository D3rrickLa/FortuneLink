package com.laderrco.fortunelink.portfolio.application.validators;

import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.ReopenAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class AccountLifecycleCommandValidator {
  private static final int ACCOUNT_NAME_LENGTH = 100;

  public ValidationResult validate(CreateAccountCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    ValidationUtils.validatePortfolioAndUserIds(command, errors);

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

    ValidationUtils.validatePortfolioAndUserIds(command, errors);

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

    ValidationUtils.validatePortfolioAndUserIds(command, errors);

    if (command.accountId() == null) {
      errors.add("AccountId is required");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(DeleteAccountCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    ValidationUtils.validatePortfolioAndUserIds(command, errors);

    if (command.accountId() == null) {
      errors.add("AccountId is required");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }
}
