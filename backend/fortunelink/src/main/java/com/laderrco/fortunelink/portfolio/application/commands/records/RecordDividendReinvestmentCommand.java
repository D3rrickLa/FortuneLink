package com.laderrco.fortunelink.portfolio.application.commands.records;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;

/**
 * Records a DRIP reinvestment. IMPORTANT: Do NOT also call recordDividend() for the same event.
 * DIVIDEND_REINVEST is self-contained - no cash lands in the account. Recording both will cause
 * replayFullAccount() to overstate cash.
 * NOTE: swtich notes to be the last thing, it's optional
 */
public record RecordDividendReinvestmentCommand(
    PortfolioId portfolioId,
    UserId userId,
    AccountId accountId,
    String assetSymbol,
    DripExecution execution,
    String notes,
    // encapsulates the trade details
    Instant transactionDate) implements TransactionCommand {

  public record DripExecution(
      Quantity sharesPurchased, Price pricePerShare // explicit, not derived
  ) {
    public Money totalCost() {
      return pricePerShare.pricePerUnit().multiply(sharesPurchased.amount());
    }
  }
}
