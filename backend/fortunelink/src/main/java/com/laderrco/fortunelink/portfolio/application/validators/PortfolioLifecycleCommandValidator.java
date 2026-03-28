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

    ValidationUtils.validatePortfolioAndUserIds(command, errors);

    if (command.name() == null) {
      errors.add("Portfolio name is required");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(DeletePortfolioCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    ValidationUtils.validatePortfolioAndUserIds(command, errors);

    if (!command.confirmed()) {
      errors.add("Confirm delete cannot be false");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }
}
