package com.laderrco.fortunelink.portfolio.application.validators;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.shared.enums.Precision;

@Component
public class TransactionCommandValidator {
    public ValidationResult validate(RecordPurchaseCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }

        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }

        if (command.symbol() == null || command.symbol().trim().isEmpty()) {
            errors.add("Asset symbol is required");
        } else if (!isValidSymbol(command.symbol())) {
            errors.add("Invalid asset symbol format");
        }

        ValidationResult quantityValidation = validateQuantity(command.quantity());
        if (!quantityValidation.isValid()) {
            errors.addAll(quantityValidation.errors());
        }

        ValidationResult priceValidation = validateAmount(command.price().amount());
        if (!priceValidation.isValid()) {
            errors.addAll(priceValidation.errors());
        }

        for (Fee fee : command.fees()) {
            ValidationResult feeValidation = validateAmount(fee.nativeAmount().amount());
            if (!feeValidation.isValid()) {
                errors.add("Invalid fee: " + String.join(", ", feeValidation.errors()));
            }

        }

        if (command.price().currency() == null) {
            errors.add("Currency is required");
        } else if (!isValidCurrency(command.price().currency().getCode())) {
            errors.add("Invalid currency code");
        }

        ValidationResult dateValidation = validateDate(command.transactionDate());
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(RecordSaleCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }

        if (command.userId() == null) {
            errors.add("UserId is required");
        }

        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }

        if (command.symbol() == null) {
            errors.add("Symbol is required");
        }

        ValidationResult quantityValidation = validateQuantity(command.quantity());
        if (!quantityValidation.isValid()) {
            errors.addAll(quantityValidation.errors());
        }

        ValidationResult priceValidation = validateAmount(command.price().amount());
        if (!priceValidation.isValid()) {
            errors.addAll(priceValidation.errors());
        }

        if (command.price().currency() == null) {
            errors.add("Currency is required");
        }

        ValidationResult dateValidation = validateDate(command.transactionDate());
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(RecordDepositCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }
        if (command.userId() == null) {
            errors.add("UserId is required");
        }

        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }

        ValidationResult amountValidation = validateAmount(command.amount().amount());
        if (!amountValidation.isValid()) {
            errors.addAll(amountValidation.errors());
        }

        ValidationResult dateValidation = validateDate(command.transactionDate());
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(RecordWithdrawalCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }

        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }

        ValidationResult amountValidation = validateAmount(command.amount().amount());
        if (!amountValidation.isValid()) {
            errors.addAll(amountValidation.errors());
        }

        ValidationResult dateValidation = validateDate(command.transactionDate());
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(RecordIncomeCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }

        if (command.userId() == null) {
            errors.add("UserId is required");
        }

        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }

        if (command.assetSymbol() == null) {
            errors.add("Symbol is required");
        }

        ValidationResult amountValidation = validateAmount(command.amount().amount());
        if (!amountValidation.isValid()) {
            errors.addAll(amountValidation.errors());
        }

        if (command.type() == null ||
                (!command.type().equals(TransactionType.DIVIDEND)
                        && !command.type().equals(TransactionType.INTEREST))) {
            errors.add("Income type must be either DIVIDEND or INTEREST");
        }

        ValidationResult dateValidation = validateDate(command.transactionDate());
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(RecordFeeCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();

        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }

        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }

        ValidationResult amountValidation = validateAmount(command.totalAmount().amount());
        if (!amountValidation.isValid()) {
            errors.addAll(amountValidation.errors());
        }

        if (command.currency() == null) {
            errors.add("Currency is required");
        }

        ValidationResult dateValidation = validateDate(command.transactionDate());
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(UpdateTransactionCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();
        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }

        if (command.userId() == null) {
            errors.add("UserId is required");
        }

        if (command.date() == null) {
            errors.add("UpdateDate is required");
        }

        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }

        if (command.type() == null) {
            errors.add("TransactionType is required");
        }

        if (command.quantity() == null) {
            errors.add("Quantity is required");
        } else if (command.quantity().compareTo(Quantity.ZERO) <= 0) {
            errors.add("Quantity must be positive");
        }

        if (command.price() == null) {
            errors.add("Price is required");
        } else if (command.price().amount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Price must be positive");
        }

        if (command.fee() == null) {
            errors.add("Fee is required");
        }

        if (command.date() == null) {
            errors.add("Date is required");
        } else if (command.date().isAfter(Instant.now())) {
            errors.add("Date cannot be in the future");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    public ValidationResult validate(DeleteTransactionCommand command) {
        Objects.requireNonNull(command);
        List<String> errors = new ArrayList<>();
        if (command.portfolioId() == null) {
            errors.add("PortfolioId is required");
        }
        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }
        if (command.transactionId() == null) {
            errors.add("TransactionId is required");
        }

        return errors.isEmpty()
                ? ValidationResult.success()
                : ValidationResult.failure(errors);
    }

    private ValidationResult validateAmount(BigDecimal amount) {
        if (amount == null) {
            return ValidationResult.failure("Amount is required");
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.failure("Amount cannot be negative");
        }

        if (amount.scale() > Precision.getMoneyPrecision()) {
            return ValidationResult.failure("Amount can have at most 34 decimal places. Scale is " + amount.scale());
        }

        return ValidationResult.success();
    }

    private boolean isValidCurrency(String currency) {
        try {
            Currency.of(currency);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private ValidationResult validateQuantity(Quantity quantity) {
        if (quantity == null) {
            return ValidationResult.failure("Quantity is required");
        }

        if (quantity.compareTo(Quantity.ZERO) <= 0) {
            return ValidationResult.failure("Quantity must be greater than zero");
        }

        if (quantity.amount().scale() > Precision.QUANTITY.getDecimalPlaces()) {
            return ValidationResult.failure("Quantity can have at most 8 decimal places");
        }

        return ValidationResult.success();
    }

    private boolean isValidSymbol(String symbol) {
        // Basic validation: 1-10 uppercase letters/numbers,
        // possibly with dots or dashes
        return symbol != null && symbol.matches("^[A-Z0-9.\\-]{1,10}$");
    }

    private ValidationResult validateDate(Instant date) {
        if (date == null) {
            return ValidationResult.failure("Transaction date is required");
        }

        if (date.isAfter(Instant.now())) {
            return ValidationResult.failure("Transaction date cannot be in the future");
        }

        // Prevent transactions too far in the past (e.g., 50 years)
        if (date.isBefore(Instant.now().minus(Period.ofYears(50)))) {
            return ValidationResult.failure("Transaction date is too far in the past");
        }

        return ValidationResult.success();
    }
}
