package com.laderrco.fortunelink.portfolio.application.services;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;


/**
 * Records transactions against an account by:
 *   1. Mutating account state (positions, cash balance)
 *   2. Constructing and returning an immutable Transaction record
 *
 * The caller (TransactionService) is responsible for persisting both
 * the mutated portfolio and the returned Transaction.
 *
 * No repositories, no market data. Pure domain logic.
 */
@Service
public class TransactionRecordingServiceImpl implements TransactionRecordingService {

    @Override
    public Transaction recordBuy(Account account, AssetSymbol symbol, AssetType type, Quantity quantity, Price price,
            Money fee, Instant date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordBuy'");
    }

    @Override
    public Transaction recordSell(Account account, AssetSymbol symbol, Quantity quantity, Price price, Money fee,
            Instant date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordSell'");
    }

    @Override
    public Transaction recordDeposit(Account account, Money amount, Instant date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordDeposit'");
    }

    @Override
    public Transaction recordWithdrawal(Account account, Money amount, Instant date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordWithdrawal'");
    }

    @Override
    public Transaction recordFee(Account account, Money amount, Instant date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordFee'");
    }

    @Override
    public Transaction recordDividend(Account account, AssetSymbol symbol, Money amount, Instant date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordDividend'");
    }

    @Override
    public Transaction recordDividendReinvestment(Account account, AssetSymbol symbol, Quantity quantity, Price price,
            Instant date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordDividendReinvestment'");
    }

    @Override
    public void replayTransaction(Account account, Transaction tx) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'replayTransaction'");
    }
    
}
