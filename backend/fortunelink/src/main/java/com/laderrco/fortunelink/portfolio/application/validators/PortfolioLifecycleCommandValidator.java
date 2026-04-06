package com.laderrco.fortunelink.portfolio.application.validators;

import com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class PortfolioLifecycleCommandValidator {
  private static final int PORTFOLIO_NAME_LENGTH = 100;
  private static final int DESCRIPTION_NAME_LENGTH = 500;

  public ValidationResult validate(CreatePortfolioCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    if (command.userId() == null) {
      errors.add("UserId is required");
    }

    validatePortfoliotName(command.name(), errors);

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

    ValidationUtils.validatePortfolioAndUserIds(command, errors);
    validatePortfoliotName(command.name(), errors);

    if (command.description() != null && command.description().length() > DESCRIPTION_NAME_LENGTH) {
      errors.add("Description must be 500 characters or less");
    }

    if (command.currency() != null && !ValidationUtils.isValidCurrency(
        command.currency().getCode())) {
      errors.add("Invalid currency code");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(DeletePortfolioCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();
    ValidationUtils.validatePortfolioAndUserIds(command, errors);
    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  private void validatePortfoliotName(String name, List<String> errors) {
    if (name == null || name.trim().isEmpty()) {
      errors.add("Portfolio name is required");
    } else if (name.length() > PORTFOLIO_NAME_LENGTH) {
      errors.add("Portfolio name must be 100 characters or less");
    }
  }
}
