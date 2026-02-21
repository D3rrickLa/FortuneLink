package com.laderrco.fortunelink.portfolio.application.exceptions;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;

public class AccountNotEmptyException extends RuntimeException {
    
    public AccountNotEmptyException(String s) {
        super(s);
    }
    
    public AccountNotEmptyException(String accountId, int assetCount) {
        super("Account " + accountId + " is not empty. Contains " + assetCount + " assets.");
    }

    public AccountNotEmptyException(AccountId accountId, int size) {
        this(accountId.id().toString(), size);
    }
}