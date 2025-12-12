package com.laderrco.fortunelink.portfolio_management.domain.exceptions;

import java.math.BigDecimal;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }

    public InsufficientFundsException(Money totalCost, Money cashBalance, AccountId accountId) {
        String s = String.format("Insufficent funds. Total cost is $%s. Account, %s, only has a cash balance of %s",
                totalCost.amount().toString(), cashBalance.amount().toString(), accountId.toString());
        super(s); // this is allowed in JDK 25+, we just have to make sure the stuff we are passing is initialized properly if being used
    }

    public InsufficientFundsException(String symbol, BigDecimal quantity, BigDecimal quantity2) {
        String s = String.format("Quantity to sell %s is greater than what you own %s in %s", quantity, quantity2, symbol);
        super(s);
    }
}
