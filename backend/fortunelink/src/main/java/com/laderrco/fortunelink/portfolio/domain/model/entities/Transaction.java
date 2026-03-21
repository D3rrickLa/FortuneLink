package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

import com.laderrco.fortunelink.portfolio.domain.model.enums.CashImpact;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

/**
 * Represents an immutable record of a financial event within an account.
 * <p>
 * This is the "source of truth" used to reconstruct account balances and portfolio positions. It
 * enforces strict invariants between the transaction type, trade execution details, and the
 * resulting impact on cash (cashDelta).
 * </p>
 *
 * @param transactionId        Unique identifier for this transaction.
 * @param accountId            The account to which this transaction belongs.
 * @param transactionType      The nature of the event (e.g., BUY, SELL, DIVIDEND, SPLIT).
 * @param execution            Details of the asset trade (required for trade-based types).
 * @param split                Ratio details (required for stock splits).
 * @param cashDelta            The net change in account cash ('+' for inflows, '-' for outflows).
 * @param fees                 A list of charges associated with this transaction.
 * @param notes                User or system-generated remarks.
 * @param occurredAt           The date and time the transaction took place.
 * @param relatedTransactionId Reference to another transaction (e.g., for reversals or linked
 *                             trades).
 * @param metadata             Audit data, source tracking, and exclusion status.
 */
@Builder
public record Transaction(
    TransactionId transactionId,
    AccountId accountId,
    TransactionType transactionType,
    TradeExecution execution,
    Ratio split,
    Money cashDelta,
    List<Fee> fees,
    String notes,
    Instant occurredAt,
    TransactionId relatedTransactionId,
    TransactionMetadata metadata) {
  public Transaction {
    notNull(transactionId, "transactionId");
    notNull(accountId, "accountId");
    notNull(transactionType, "transactionType");
    notNull(cashDelta, "cashDelta");
    notNull(fees, "fees");
    notNull(metadata, "metadata");
    notNull(occurredAt, "occurredAt");
    notNull(notes, "notes");

    validateConsistency("execution", transactionType.requiresExecution(), execution != null);
    validateConsistency("split details", transactionType.requiresSplitDetails(), split != null);

    fees = List.copyOf(fees);
    notes = notes.trim();

    if (transactionType.cashImpact() == CashImpact.NONE && !cashDelta.isZero()) {
      throw new IllegalArgumentException(transactionType + " cannot affect cash");
    }

    if (transactionType.requiresExecution()) {
      validateTradeConsistency(transactionType, execution, cashDelta, fees);
    } else if (!fees.isEmpty()) {
      throw new IllegalArgumentException(transactionType + " cannot have fees");
    }
  }

  private static void validateTradeConsistency(TransactionType transactionType,
      TradeExecution execution, Money cashDelta, List<Fee> fees) {
    Money grossValue = execution.grossValue();
    Money totalFees = Fee.totalInAccountCurrency(fees, cashDelta.currency());

    Money expectedCashDelta = switch (transactionType.cashImpact()) {
      case IN -> grossValue.subtract(totalFees);
      case OUT -> grossValue.add(totalFees).negate();
      case NONE -> Money.zero(cashDelta.currency());
    };

    if (!cashDelta.equals(expectedCashDelta)) {
      throw new IllegalArgumentException(
          "Cash delta mismatch. Expected: " + expectedCashDelta + ", got: " + cashDelta);
    }
  }

  public Transaction markAsExcluded(UserId userId, String reason) {
    return new Transaction(transactionId, accountId, transactionType, execution, split, cashDelta,
        fees, notes, occurredAt, relatedTransactionId, metadata.markAsExcluded(userId, reason));
  }

  public Transaction restore() {
    return new Transaction(transactionId, accountId, transactionType, execution, split, cashDelta,
        fees, notes, occurredAt, relatedTransactionId, metadata.restore());
  }

  public boolean isExcluded() {
    return metadata.isExcluded();
  }

  /**
   * Sums all fees associated with this transaction in the currency of the cashDelta.
   */
  public Money totalFeesInAccountCurrency() {
    return Fee.totalInAccountCurrency(fees, cashDelta.currency());
  }

  private void validateConsistency(String label, boolean isRequired, boolean isPresent) {
    if (isRequired && !isPresent) {
      throw new IllegalArgumentException(transactionType + " requires " + label);
    }
    if (!isRequired && isPresent) {
      throw new IllegalArgumentException(transactionType + " cannot have " + label);
    }
  }

  public record TradeExecution(AssetSymbol asset, Quantity quantity, Price pricePerUnit) {
    public TradeExecution {
      notNull(asset, "Asset symbol cannot be null");
      notNull(quantity, "Quantity cannot be null");
      notNull(pricePerUnit, "Price per unit cannot be null");

      if (quantity.isZero()) {
        throw new IllegalArgumentException("Trade quantity cannot be zero");
      }
    }

    /**
     * Gross value of the trade before fees.
     */
    public Money grossValue() {
      return pricePerUnit.pricePerUnit().multiply(quantity);
    }
  }
}
