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
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

@Component
public class TransactionCommandValidator {
  private static final int REASON_LENGTH = 500;

  public ValidationResult validate(RecordPurchaseCommand command) {
    return validateCommand(command, errors -> {
      ValidationUtils.validateSymbol(command.symbol(), errors);
      ValidationUtils.validateQuantity(command.quantity(), errors);
      ValidationUtils.validateAmount(command.price().amount(), errors);
      validateFees(command.fees(), errors);
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  public ValidationResult validate(RecordSaleCommand command) {
    return validateCommand(command, errors -> {
      ValidationUtils.validateSymbol(command.symbol(), errors);
      ValidationUtils.validateQuantity(command.quantity(), errors);
      ValidationUtils.validateAmount(command.price().amount(), errors);
      validateFees(command.fees(), errors);
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  public ValidationResult validate(RecordDepositCommand command) {
    return validateCommand(command, errors -> {
      ValidationUtils.validateAmount(command.amount().amount(), errors);
      validateFees(command.fees(), errors);
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  public ValidationResult validate(RecordWithdrawalCommand command) {
    return validateCommand(command, errors -> {
      ValidationUtils.validateAmount(command.amount().amount(), errors);
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  // TODO: when bonds are fully supported, validate that an open position
  // exists for assetSymbol before recording asset-level interest.
  // Right now we accept the symbol on faith.
  public ValidationResult validate(RecordInterestCommand command) {
    return validateCommand(command, errors -> {
      if (command.isAssetInterest()) {
        ValidationUtils.validateSymbol(command.assetSymbol(), errors);
      }
      ValidationUtils.validateAmount(command.amount().amount(), errors);
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  public ValidationResult validate(RecordDividendCommand command) {
    return validateCommand(command, errors -> {
      ValidationUtils.validateSymbol(command.assetSymbol(), errors);
      ValidationUtils.validateAmount(command.amount().amount(), errors);
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  public ValidationResult validate(RecordDividendReinvestmentCommand command) {
    return validateCommand(command, errors -> {
      ValidationUtils.validateSymbol(command.assetSymbol(), errors);
      if (command.execution() == null) {
        errors.add("Drip execution is required");
      } else {
        ValidationUtils.validateAmount(command.execution().pricePerShare().amount(), errors);
        ValidationUtils.validateQuantity(command.execution().sharesPurchased(), errors);
      }
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  public ValidationResult validate(RecordSplitCommand command) {
    return validateCommand(command, errors -> {
      ValidationUtils.validateSymbol(command.symbol(), errors);
      if (command.ratio() == null) {
        errors.add("Split ratio is required");
      }else if (command.ratio().numerator() == command.ratio().denominator()) {
        errors.add("Ratio: A 1:1 split is a no-op and is not permitted");
      }
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  public ValidationResult validate(RecordReturnOfCaptialCommand command) {
    return validateCommand(command, errors -> {
      ValidationUtils.validateSymbol(command.assetSymbol(), errors);
      ValidationUtils.validateAmount(command.distributionPerUnit().amount(), errors);
      ValidationUtils.validateQuantity(command.heldQuantity(), errors);
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  public ValidationResult validate(RecordFeeCommand command) {
    return validateCommand(command, errors -> {
      if (command.feeType() == null) {
        errors.add("Fee type is required");
      }
      ValidationUtils.validateAmount(command.amount().amount(), errors);
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  public ValidationResult validate(ExcludeTransactionCommand command) {
    return validateCommand(command, errors -> {
      if (command.transactionId() == null)
        errors.add("TransactionId is required");
      validateStringLength(command.reason(), errors);
    });
  }

  public ValidationResult validate(RestoreTransactionCommand command) {
    return validateCommand(command, errors -> {
      if (command.transactionId() == null) {
        errors.add("TransactionId is required");
      }
    });
  }

  public ValidationResult validate(RecordTransferInCommand command) {
    return validateCommand(command, errors -> {
      ValidationUtils.validateAmount(command.amount().amount(), errors);
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  public ValidationResult validate(RecordTransferOutCommand command) {
    return validateCommand(command, errors -> {
      ValidationUtils.validateAmount(command.amount().amount(), errors);
      ValidationUtils.validateDate(command.transactionDate(), null, errors);
    });
  }

  private <T extends TransactionCommand> ValidationResult validateCommand(T command,
      Consumer<List<String>> specificValidation) {
    Objects.requireNonNull(command);
    List<String> errors = new ArrayList<>();
    validateCommonIds(command, errors);
    specificValidation.accept(errors);
    return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
  }

  private void validateCommonIds(TransactionCommand command, List<String> errors) {
    if (command.portfolioId() == null)
      errors.add("PortfolioId is required");
    if (command.userId() == null)
      errors.add("UserId is required");
    if (command.accountId() == null)
      errors.add("AccountId is required");
  }

  private void validateFees(List<Fee> fees, List<String> errors) {
    for (Fee fee : fees) {
      ValidationUtils.validateAmount(fee.nativeAmount().amount(), errors);
    }
  }

  private void validateStringLength(String reason, List<String> errors) {
    if (reason == null || reason.trim().isEmpty()) {
      errors.add("Reason is required");
    } else if (reason.length() > REASON_LENGTH) {
      errors.add("Reason must be 500 characters or less");
    }
  }
}
