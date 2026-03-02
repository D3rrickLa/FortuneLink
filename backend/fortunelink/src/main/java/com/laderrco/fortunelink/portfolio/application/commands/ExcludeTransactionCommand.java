package com.laderrco.fortunelink.portfolio.application.commands;

import com.laderrco.fortunelink.portfolio.application.commands.records.TransactionCommand;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;


// when we exclude a transaction - we don't restore the cash to the account balance...
// two ways to think. exclues - eclude from psoition calc only, not from cash
// i.e. 'i eclude a bvuy but my cash bal didn't change' OR
// this transaction never happened which in this case we need to revser the cash impact - more complex, need a replayFullAcc
// for every exclude and restore
public record ExcludeTransactionCommand(
        TransactionId transactionId,
        PortfolioId portfolioId,
        UserId userId,
        AccountId accountId,
        String reason) implements TransactionCommand {
}