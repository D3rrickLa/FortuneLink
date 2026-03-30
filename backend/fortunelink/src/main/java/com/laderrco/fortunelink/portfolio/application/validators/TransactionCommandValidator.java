package com.laderrco.fortunelink.portfolio.application.validators;

import com.laderrco.fortunelink.portfolio.application.commands.ExcludeTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.RestoreTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDividendCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDividendReinvestmentCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordInterestCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordReturnOfCaptialCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSplitCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordTransferInCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordTransferOutCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio.application.utils.ValidationUtils;
import com.laderrco.fortunelink.portfolio.application.utils.annotations.TransactionCommand;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class TransactionCommandValidator {
  public ValidationResult validate(RecordPurchaseCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    if (!ValidationUtils.isValidCurrency(command.price().currency().getCode())) {
      errors.add("Invalid currency code");
    }

    ValidationUtils.validateSymbol(command.symbol(), errors);
    ValidationUtils.validateQuantity(command.quantity(), errors);
    ValidationUtils.validateAmount(command.price().amount(), errors);

    for (Fee fee : command.fees()) {
      ValidationUtils.validateAmount(fee.nativeAmount().amount(), errors);
    }

    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RecordSaleCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    ValidationUtils.validateSymbol(command.symbol(), errors);
    ValidationUtils.validateQuantity(command.quantity(), errors);
    ValidationUtils.validateAmount(command.price().amount(), errors);
    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RecordDepositCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    ValidationUtils.validateAmount(command.amount().amount(), errors);
    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RecordWithdrawalCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    ValidationUtils.validateAmount(command.amount().amount(), errors);
    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  // TODO: when bonds are fully supported, validate that an open position
  // exists for assetSymbol before recording asset-level interest.
  // Right now we accept the symbol on faith.
  public ValidationResult validate(RecordInterestCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    // Only validate symbol if it's asset-level interest
    if (command.isAssetInterest()) {
      ValidationUtils.validateSymbol(command.assetSymbol(), errors);
    }
    ValidationUtils.validateAmount(command.amount().amount(), errors);
    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RecordDividendCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    ValidationUtils.validateSymbol(command.assetSymbol(), errors);
    ValidationUtils.validateAmount(command.amount().amount(), errors);
    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RecordDividendReinvestmentCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    ValidationUtils.validateSymbol(command.assetSymbol(), errors);

    if (command.execution() == null) {
      errors.add("Drip execution is required");
    } else {
      ValidationUtils.validateAmount(command.execution().pricePerShare().amount(), errors);
      ValidationUtils.validateQuantity(command.execution().sharesPurchased(), errors);

    }

    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RecordSplitCommand command) {
    Objects.requireNonNull(command, "Command cannot be null");
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    ValidationUtils.validateSymbol(command.symbol(), errors);

    if (command.ratio() == null) {
      errors.add("Ratio: Split ratio is required");
    } else {
      // Guard against the 1:1 No-Op
      if (command.ratio().numerator() == command.ratio().denominator()) {
        errors.add("Ratio: A 1:1 split is a no-op and is not permitted");
      }
      // Basic ratio check (though constructor handles <= 0, we check for
      // null/integrity)
      if (command.ratio().numerator() <= 0 || command.ratio().denominator() <= 0) {
        errors.add("Ratio: Split ratio must contain positive integers");
      }
    }

    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RecordReturnOfCaptialCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    ValidationUtils.validateSymbol(command.assetSymbol(), errors);
    ValidationUtils.validateAmount(command.distributionPerUnit().amount(), errors);
    ValidationUtils.validateQuantity(command.heldQuantity(), errors);
    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RecordFeeCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    ValidationUtils.validateAmount(command.amount().amount(), errors);
    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RestoreTransactionCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    if (command.transactionId() == null) {
      errors.add("TransactionId is required");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(ExcludeTransactionCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);

    if (command.transactionId() == null) {
      errors.add("TransactionId is required");
    }

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RecordTransferInCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);
    ValidationUtils.validateAmount(command.amount().amount(), errors);
    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  public ValidationResult validate(RecordTransferOutCommand command) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();

    validateCommonIds(command, errors);
    ValidationUtils.validateAmount(command.amount().amount(), errors);
    ValidationUtils.validateDate(command.transactionDate(), null, errors);

    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  private void validateCommonIds(TransactionCommand command, List<String> errors) {
    if (command.portfolioId() == null) {
      errors.add("PortfolioId is required");
    }

    if (command.userId() == null) {
      errors.add("UserId is required");
    }

    if (command.accountId() == null) {
      errors.add("AccountId is required");
    }
  }

}
