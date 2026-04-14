package com.laderrco.fortunelink.portfolio.application.validators;

import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.ReopenAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import com.laderrco.fortunelink.portfolio.application.utils.annotations.HasAccountId;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
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
    validateAccountName(command.accountName(), errors);

    if (command.accountType() == null) {
      errors.add("Account type is required");
    }

    if (command.strategy() == null) {
      errors.add("Strategy is required");
    } else if (command.strategy() != PositionStrategy.ACB) {
      errors.add("Strategy " + command.strategy() + " is not yet supported. "
          + "Current tax regulations only support ACB.");
    }

    if (command.baseCurrency() == null) {
      errors.add("Base currency is required");
    } else {

      boolean isValid = ValidationUtils.isValidCurrency(command.baseCurrency().getCode());

      if (!isValid) {
        errors.add("Invalid currency code");
      }
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(UpdateAccountCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    ValidationUtils.validatePortfolioAndUserIds(command, errors);
    validateAccountId(command, errors);
    validateAccountName(command.accountName(), errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(ReopenAccountCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    ValidationUtils.validatePortfolioAndUserIds(command, errors);
    validateAccountId(command, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(DeleteAccountCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    ValidationUtils.validatePortfolioAndUserIds(command, errors);
    validateAccountId(command, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  private void validateAccountId(HasAccountId command, List<String> errors) {
    if (command.accountId() == null) {
      errors.add("AccountId is required");
    }
  }

  private void validateAccountName(String name, List<String> errors) {
    if (name == null || name.trim().isEmpty()) {
      errors.add("Account name is required");
    } else if (name.length() > ACCOUNT_NAME_LENGTH) {
      errors.add("Account name must be 100 characters or less");
    }
  }
}
