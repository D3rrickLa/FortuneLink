package com.laderrco.fortunelink.portfolio_management.application.exceptions;

import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;

public class AccountNotEmptyException extends RuntimeException {
    
    public AccountNotEmptyException(String s) {
        super(s);
    }
    
    public AccountNotEmptyException(String accountId, int assetCount) {
        super("Account " + accountId + " is not empty. Contains " + assetCount + " assets.");
    }

    public AccountNotEmptyException(AccountId accountId, int size) {
        this(accountId.accountId().toString(), size);
    }
}