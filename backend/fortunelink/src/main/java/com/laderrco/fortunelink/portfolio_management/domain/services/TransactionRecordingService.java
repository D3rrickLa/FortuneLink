package com.laderrco.fortunelink.portfolio_management.domain.services;

import java.time.Instant;

import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

public interface TransactionRecordingService {
    // TODO make these return the actual Transaction rather than void
    void recordBuy(Account account, AssetSymbol symbol, AssetType type, Quantity quantity, Price price, Money fee, Instant date);
    
    void recordSell(Account account, AssetSymbol symbol, Quantity quantity, Price price, Money fee, Instant date);
    
    void recordDeposit(Account account, Money amount, Instant date);
    
    void recordWithdrawal(Account account, Money amount, Instant date);
    
    void recordDividend(Account account, AssetSymbol symbol, Money amount, Instant date);
    
    void recordDividendReinvestment(Account account, AssetSymbol symbol, Quantity quantity, Price price, Instant date);
}