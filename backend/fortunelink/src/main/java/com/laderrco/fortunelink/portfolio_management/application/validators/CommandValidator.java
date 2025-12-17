package com.laderrco.fortunelink.portfolio_management.application.validators;

import java.math.BigDecimal;
import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class CommandValidator {
    public ValidationResult validate(RecordPurchaseCommand recordPurchaseCommand) {
        return null;
    }

    public ValidationResult validate(RecordSaleCommand recordSaleCommand) {
        return null;
    }

    public ValidationResult validate(RecordDepositCommand recordDepositCommand) {
        return null;
    }

    public ValidationResult validate(RecordWithdrawalCommand recordWithdrawalCommand) {
        return null;
    }

    public ValidationResult validate(RecordIncomeCommand recordDividendCommand) {
        return null;
    }

    public ValidationResult validate(RecordFeeCommand recordFeeCommand) {
        return null;
    }

    public ValidationResult validate(AddAccountCommand addAccountCommand) {
        return null;
    }

    public ValidationResult validate(CreatePortfolioCommand command) {
        return null;
    }

    public ValidationResult validateAmount(Money money) {
        return null;
    }
    public ValidationResult validateQuantity(BigDecimal quantity) {
        return null;
    }
    public ValidationResult validateDate(Instant TransactionDate) {
        return null;
    }
    public ValidationResult validateSymbol(String assetSymbol) {
        return null;
    }

}
