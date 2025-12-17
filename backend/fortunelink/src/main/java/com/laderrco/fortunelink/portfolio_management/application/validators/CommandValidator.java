package com.laderrco.fortunelink.portfolio_management.application.validators;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

@Component
public class CommandValidator {
   public ValidationResult validate(RecordPurchaseCommand command) {
        List<String> errors = new ArrayList<>();
        
        if (command.userId() == null) {
            errors.add("UserId is required");
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
            ValidationResult feeValidation = validateAmount(fee.amountInNativeCurrency().amount());
            if (!feeValidation.isValid()) {
                errors.add("Invalid fee: " + String.join(", ", feeValidation.errors()));
            }
            
        }
        
        if (command.price().currency() == null) {
            errors.add("Currency is required");
        } else if (!isValidCurrency(command.price().currency().getSymbol())) {
            errors.add("Invalid currency code");
        }
        
        ValidationResult dateValidation = validateDate(LocalDateTime.ofInstant(command.transactionDate(), ZoneOffset.UTC));
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }
    
    public ValidationResult validate(RecordSaleCommand command) {
        List<String> errors = new ArrayList<>();
        
        if (command.userId() == null) {
            errors.add("UserId is required");
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
        
        if (command.price().currency() == null) {
            errors.add("Currency is required");
        }
        
        ValidationResult dateValidation = validateDate(LocalDateTime.ofInstant(command.transactionDate(), ZoneOffset.UTC));
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }
    
    public ValidationResult validate(RecordDepositCommand command) {
        List<String> errors = new ArrayList<>();
        
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
        
        if (command.currency() == null) {
            errors.add("Currency is required");
        }
        
        ValidationResult dateValidation = validateDate(LocalDateTime.ofInstant(command.transactionDate(), ZoneOffset.UTC));
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }
    
    public ValidationResult validate(RecordWithdrawalCommand command) {
        List<String> errors = new ArrayList<>();
        
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
        
        if (command.currency() == null) {
            errors.add("Currency is required");
        }
        
        ValidationResult dateValidation = validateDate(LocalDateTime.ofInstant(command.transactionDate(), ZoneOffset.UTC));
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }
    
    public ValidationResult validate(RecordIncomeCommand command) {
        List<String> errors = new ArrayList<>();
        
        if (command.userId() == null) {
            errors.add("UserId is required");
        }
        
        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }
        
        if (command.symbol() == null || command.symbol().trim().isEmpty()) {
            errors.add("Asset symbol is required");
        }
        
        ValidationResult amountValidation = validateAmount(command.amount().amount());
        if (!amountValidation.isValid()) {
            errors.addAll(amountValidation.errors());
        }
        
        if (command.type() == null || 
            (!command.type().equals(TransactionType.DIVIDEND) && !command.type().equals(TransactionType.INTEREST))) {
            errors.add("Income type must be either DIVIDEND or INTEREST");
        }
        
        ValidationResult dateValidation = validateDate(LocalDateTime.ofInstant(command.transactionDate(), ZoneOffset.UTC));
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }
    
    public ValidationResult validate(RecordFeeCommand command) {
        List<String> errors = new ArrayList<>();
        
        if (command.userId() == null) {
            errors.add("UserId is required");
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
        
        ValidationResult dateValidation = validateDate(LocalDateTime.ofInstant(command.transactionDate(), ZoneOffset.UTC));
        if (!dateValidation.isValid()) {
            errors.addAll(dateValidation.errors());
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }
    
    public ValidationResult validate(AddAccountCommand command) {
        List<String> errors = new ArrayList<>();
        
        if (command.userId() == null) {
            errors.add("UserId is required");
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
        } else if (!isValidCurrency(command.baseCurrency().getSymbol())) {
            errors.add("Invalid currency code");
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }

    public ValidationResult validate(CreatePortfolioCommand command) {
        List<String> errors = new ArrayList<>();
        
        if (command.userId() == null) {
            errors.add("UserId is required");
        }
        
        if (command.defaultCurrency() == null) {
            errors.add("Curency is required");
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }
    
    // Helper validation methods
    public ValidationResult validateAmount(BigDecimal amount) {
        if (amount == null) {
            return ValidationResult.failure("Amount is required");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            return ValidationResult.failure("Amount cannot be negative");
        }
        
        if (amount.scale() > 2) {
            return ValidationResult.failure("Amount can have at most 2 decimal places");
        }
        
        return ValidationResult.success();
    }
    
    public ValidationResult validateQuantity(BigDecimal quantity) {
        if (quantity == null) {
            return ValidationResult.failure("Quantity is required");
        }
        
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.failure("Quantity must be greater than zero");
        }
        
        if (quantity.scale() > 8) {
            return ValidationResult.failure("Quantity can have at most 8 decimal places");
        }
        
        return ValidationResult.success();
    }
    
    public ValidationResult validateDate(LocalDateTime date) {
        if (date == null) {
            return ValidationResult.failure("Transaction date is required");
        }
        
        if (date.isAfter(LocalDateTime.now())) {
            return ValidationResult.failure("Transaction date cannot be in the future");
        }
        
        // Prevent transactions too far in the past (e.g., 50 years)
        if (date.isBefore(LocalDateTime.now().minusYears(50))) {
            return ValidationResult.failure("Transaction date is too far in the past");
        }
        
        return ValidationResult.success();
    }
    
    public ValidationResult validateSymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return ValidationResult.failure("Symbol is required");
        }
        
        if (!isValidSymbol(symbol)) {
            return ValidationResult.failure("Invalid symbol format");
        }
        
        return ValidationResult.success();
    }
    
    private boolean isValidSymbol(String symbol) {
        // Basic validation: 1-10 uppercase letters/numbers, possibly with dots or dashes
        return symbol != null && symbol.matches("^[A-Z0-9.\\-]{1,10}$");
    }
    
    private boolean isValidCurrency(String currency) {
        try {
            ValidatedCurrency.of(currency);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
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
