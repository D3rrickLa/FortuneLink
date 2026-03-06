package com.laderrco.fortunelink.portfolio.application.commands.records;

import java.time.Instant;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;


/**
 * Bug 16 fix: removed the dead {@code fees} field.
 *
 * The field was never passed to {@code TransactionRecordingService.recordWithdrawal()}, which
 * accepts no fee parameter. Any caller that passed fees in silently had them discarded, which is a
 * data loss bug. Withdrawals do not carry fees in this model — if a broker charges a withdrawal
 * fee, record it as a separate FEE transaction.
 */
public record RecordWithdrawalCommand(PortfolioId portfolioId, UserId userId, AccountId accountId,
        Money amount, Instant transactionDate, String notes) implements TransactionCommand {
}


