package com.laderrco.fortunelink.portfolio_management.application.validators;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CorrectAssetTickerCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

@Component
public class CommandValidator implements ClassValidation {
   public ValidationResult validate(RecordPurchaseCommand command) {
        ClassValidation.validateParameter(command);
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
        } 
        else if (!isValidCurrency(command.price().currency().getCode())) {
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
        ClassValidation.validateParameter(command);
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
        ClassValidation.validateParameter(command);
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
        ClassValidation.validateParameter(command);
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
        ClassValidation.validateParameter(command);
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
        ClassValidation.validateParameter(command);
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

    public ValidationResult validate(UpdateTransactionCommand command) {
        ClassValidation.validateParameter(command);
        List<String> errors = new ArrayList<>();
        if (command.userId() == null) {
            errors.add("UserId is requried");
        }
        if(command.date() == null) {
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
        }
        else if (command.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            errors. add("Quantity must be positive");
        }

        if (command.price() == null) {
            errors.add("Price is required");
        }
        else if (command.price().amount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Price must be positive");
        }
        
        if(command.fee() == null) {
            errors.add("Fee is required");
        }

        if (command.date() == null) {
            errors.add("Date is required");
        }
        else if (command.date().isAfter(Instant.now())) {
            errors.add("Date cannot be in the future");
        }

        return errors.isEmpty() 
            ? ValidationResult.success()
            : ValidationResult.failure(errors);
    }

    public ValidationResult validate(DeleteTransactionCommand command) {
        ClassValidation.validateParameter(command);
        List<String> errors = new ArrayList<>();
        if (command.userId() == null) {
            errors.add("UserId is required");
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

    public ValidationResult validate(AddAccountCommand command) {
        ClassValidation.validateParameter(command);
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
        } 
        else if (!isValidAccountType(command.accountType().name())) {
            errors.add("Invalid account type");
        }
        
        if (command.baseCurrency() == null) {
            errors.add("Base currency is required");
        } 
        else if (!isValidCurrency(command.baseCurrency().getCode())) {
            errors.add("Invalid currency code");
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }

    public ValidationResult validate(RemoveAccountCommand command) {
        ClassValidation.validateParameter(command);
        List<String> errors = new ArrayList<>();
        
        if (command.userId() == null) {
            errors.add("UserId is required");
        }
        
        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);

    }

    public ValidationResult validate(CreatePortfolioCommand command) {
        ClassValidation.validateParameter(command);
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

    public ValidationResult validate(DeletePortfolioCommand command) {
        ClassValidation.validateParameter(command);
        List<String> errors = new ArrayList<>();
        
        if (command.userId() == null) {
            errors.add("UserId is required");
        }
        
        if (command.confirmed() == false && command.softDelete()) {
            errors.add("Cannot soft delete without confirming");
        }

        return errors.isEmpty() 
            ? ValidationResult.success() 
            : ValidationResult.failure(errors);
    }
    
    public ValidationResult validate(CorrectAssetTickerCommand command) {
        ClassValidation.validateParameter(command);
        List<String> errors = new ArrayList<>();
        
        if (command.userId() == null) {
            errors.add("UserId is required");
        }
        
        if (command.accountId() == null) {
            errors.add("AccountId is required");
        }

        if (command.wrongAssetIdentifier() == null) {
            errors.add("Wrong AssetIdentifier is required");
        }

        if (command.correctAssetIdentifier() == null) {
            errors.add("Correct AssetIdentifier is required");
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
        
        if (amount.scale() > Precision.getMoneyPrecision()) {
            return ValidationResult.failure("Amount can have at most 34 decimal places. Scale is " + amount.scale());
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
