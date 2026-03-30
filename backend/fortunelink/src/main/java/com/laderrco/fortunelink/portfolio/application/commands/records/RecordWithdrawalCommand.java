package com.laderrco.fortunelink.portfolio.application.commands.records;

import com.laderrco.fortunelink.portfolio.application.utils.annotations.TransactionCommand;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;

/**
 * Withdrawals do not carry fees in this model. If a broker charges a withdrawal fee, record it as a
 * separate FEE transaction.
 */
public record RecordWithdrawalCommand(
    PortfolioId portfolioId,
    UserId userId,
    AccountId accountId,
    Money amount,
    Instant transactionDate,
    String notes) implements TransactionCommand {
}
