package com.laderrco.fortunelink.portfolio.domain.services;

import java.time.Instant;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public interface TransactionRecordingService {
    // these will mutate the account 'state' as in update position or what not
    Transaction recordBuy(Account account, AssetSymbol symbol, AssetType type, Quantity quantity, Price price, Money fee, Instant date);
    
    Transaction recordSell(Account account, AssetSymbol symbol, Quantity quantity, Price price, Money fee, Instant date);
    
    Transaction recordDeposit(Account account, Money amount, Instant date);
    
    Transaction recordWithdrawal(Account account, Money amount, Instant date);

    Transaction recordFee(Account account, Money amount, Instant date);
    
    Transaction recordDividend(Account account, AssetSymbol symbol, Money amount, Instant date);
    
    Transaction recordDividendReinvestment(Account account, AssetSymbol symbol, Quantity quantity, Price price, Instant date);

    void replayTransaction(Account account, Transaction tx);
}