package com.laderrco.fortunelink.portfolio.application.commands.records;

import com.laderrco.fortunelink.portfolio.application.utils.annotations.AdditionalInfoTransactionCommand;
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
 * replayFullAccount() to overstate cash. NOTE: swtich notes to be the last thing, it's optional
 */
public record RecordDividendReinvestmentCommand(
    PortfolioId portfolioId,
    UserId userId,
    AccountId accountId,
    String assetSymbol,
    DripExecution execution,
    Instant transactionDate,
    String notes) implements AdditionalInfoTransactionCommand {

  // price is explicit, not derived
  public record DripExecution(Quantity sharesPurchased, Price pricePerShare) {
    public Money totalCost() {
      return pricePerShare.pricePerUnit().multiply(sharesPurchased.amount());
    }
  }
}
