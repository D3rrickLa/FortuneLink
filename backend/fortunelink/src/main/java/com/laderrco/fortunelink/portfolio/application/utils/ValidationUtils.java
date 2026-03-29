package com.laderrco.fortunelink.portfolio.application.utils;

import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio.application.utils.annotations.HasPortfolioId;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.shared.enums.Precision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

public class ValidationUtils {
  private ValidationUtils() {
  }

  public static <T> void validate(T command, Function<T, ValidationResult> fn, String method) {
    ValidationResult result = fn.apply(command);
    if (!result.isValid()) {
      throw new InvalidCommandException("Invalid " + method + " command", result.errors());
    }
  }

  public static void validatePortfolioAndUserIds(HasPortfolioId command, List<String> errors) {
    if (command.portfolioId() == null) {
      errors.add("PortfolioId is required");
    }

    if (command.userId() == null) {
      errors.add("UserId is required");
    }
  }

  public static void validateAmount(BigDecimal amount, List<String> errors) {
    if (amount == null) {
      errors.add("Amount is required");
      return;
    }
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      errors.add("Amount must be positive");
    }
  }

  public static void validateDate(Instant date, Instant accountCreatedAt, List<String> errors) {
    if (date == null) {
      errors.add("Transaction date is required");
      return;
    }
    if (date.isAfter(Instant.now())) {
      errors.add("Transaction date cannot be in the future");
    }
    if (accountCreatedAt != null && date.isBefore(accountCreatedAt)) {
      errors.add("Transaction date predates account creation");
    }
  }

  public static void validateQuantity(Quantity quantity, List<String> errors) {
    if (quantity == null) {
      errors.add("Quantity is required");
      return;
    }

    if (quantity.compareTo(Quantity.ZERO) <= 0) {
      errors.add("Quantity must be greater than zero");
    }

    if (quantity.amount().scale() > Precision.QUANTITY.getDecimalPlaces()) {
      errors.add("Quantity can have at most 8 decimal places");
    }

  }

  public static void validateSymbol(String symbol, List<String> errors) {
    if (symbol == null || symbol.trim().isEmpty()) {
      errors.add("Asset symbol is required");
      return;
    }
    if (!symbol.matches("^[A-Z0-9.\\-]{1,20}$")) {
      errors.add("Invalid asset symbol format");
    }
  }

  public static boolean isValidCurrency(String code) {
    try {
      Currency.of(code);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
