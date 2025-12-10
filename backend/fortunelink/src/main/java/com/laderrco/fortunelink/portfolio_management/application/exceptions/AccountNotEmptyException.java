package com.laderrco.fortunelink.portfolio_management.application.exceptions;

public class AccountNotEmptyException extends RuntimeException {
    
    public AccountNotEmptyException(String s) {
        super(s);
    }
    
    public AccountNotEmptyException(String accountId, int assetCount) {
        super("Account " + accountId + " is not empty. Contains " + assetCount + " assets.");
    }
}