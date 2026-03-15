package com.laderrco.fortunelink.portfolio.domain.services;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.time.Instant;
import java.util.List;

/**
 * Service responsible for recording financial transactions and updating the state of an
 * {@link Account} aggregate. * <p>Each recording method handles the domain logic for a specific
 * transaction type, ensuring that both the {@link Transaction} record is created and the internal
 * account state (positions and cash) is updated accordingly.</p>
 */
public interface TransactionRecordingService {
  /**
   * Records a buy order and updates the account's position and cash balance.
   */
  Transaction recordBuy(Account account, AssetSymbol symbol, AssetType type, Quantity quantity,
      Price price, List<Fee> fees, String notes, Instant date);

  /**
   * Records a sell order and updates the account's position and cash balance.
   */
  Transaction recordSell(Account account, AssetSymbol symbol, Quantity quantity, Price price,
      List<Fee> fees, String notes, Instant date);

  /**
   * Records a cash deposit into the account.
   */
  Transaction recordDeposit(Account account, Money amount, String notes, Instant date);

  /**
   * Records a cash withdrawal from the account.
   */
  Transaction recordWithdrawal(Account account, Money amount, String notes, Instant date);

  /**
   * Records an account-level fee.
   */
  Transaction recordFee(Account account, Money amount, String notes, Instant date);

  /**
   * Records interest earned on an asset position.
   */
  Transaction recordInterest(Account account, AssetSymbol symbol, Money amount, String notes,
      Instant date);

  /**
   * Records a dividend payment received for an asset.
   */
  Transaction recordDividend(Account account, AssetSymbol symbol, Money amount, String notes,
      Instant date);

  /**
   * Records the reinvestment of dividends into an asset.
   */
  Transaction recordDividendReinvestment(Account account, AssetSymbol symbol, Quantity quantity,
      Price price, String notes, Instant date);

  /**
   * Records a return of capital for an asset position.
   */
  Transaction recordReturnOfCapital(Account account, AssetSymbol symbol, Quantity quantity,
      Price price, String notes, Instant date);

  /**
   * Records a transfer of funds into the account.
   */
  Transaction recordTransferIn(Account account, Money amount, String notes, Instant date);

  /**
   * Records a transfer of funds out of the account.
   */
  Transaction recordTransferOut(Account account, Money amount, String notes, Instant date);

  /**
   * Performs a position-only replay of an existing transaction.
   * <p><b>Note:</b> Cash state is not modified. Use for partial recalculations
   * on a specific symbol.</p>
   *
   * @param account The account to update.
   * @param tx      The transaction to apply.
   * @implNote The caller must invoke {@code account.clearPosition(symbol)} prior to this method to
   * ensure idempotency.
   */
  void replayTransaction(Account account, Transaction tx);

  /**
   * Performs a full replay of an existing transaction, affecting both position and cash balances.
   * <p><b>Warning:</b> This should only be used for full account reconstruction
   * (e.g., migration or corruption recovery).</p>
   *
   * @param account The account to update.
   * @param tx      The transaction to apply.
   * @implNote The caller MUST reset both positions and cash to zero before invoking; otherwise,
   * balances will be double-counted.
   */
  void replayFullTransaction(Account account, Transaction tx);
}
