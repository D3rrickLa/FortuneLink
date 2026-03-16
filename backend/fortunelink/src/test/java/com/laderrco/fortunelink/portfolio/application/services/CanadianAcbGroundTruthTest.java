package com.laderrco.fortunelink.portfolio.application.services;

import static com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType.BUY;
import static com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType.SELL;
import static com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency.CAD;
import static org.assertj.core.api.Assertions.assertThat;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.ExchangeRate;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.ApplyResult;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.FifoPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * GROUND TRUTH: Canadian CRA Adjusted Cost Base (ACB) Calculation
 * <p>
 * This test class exists to settle one question permanently: Should sell/buy use cashDelta or
 * grossValue for ACB purposes?
 * <p>
 * ANSWER (CRA IT-451R / Folio S3-F4-C1): BUY → ACB = (qty × price) + commission → cashDelta.abs() ✓
 * SELL → Proceeds = (qty × price) - commission → cashDelta ✓
 * <p>
 * Capital Gain = Net Proceeds (after commission) − ACB of shares sold
 * <p>
 * The numbers in each test are manually verifiable. Do not change them without also verifying
 * against the CRA's Adjusted Cost Base rules.
 * <p>
 */

@DisplayName("CRA ACB Ground Truth Test")
public class CanadianAcbGroundTruthTest {

  public static final RoundingMode ROUNDING_MODE = Rounding.MONEY.getMode();
  public static final int MONEY_PRECISION = Precision.getMoneyPrecision();
  private static final AssetSymbol VFV = new AssetSymbol("VFV.TO");
  private static final Currency CAD = Currency.CAD;
  private static final AssetType ETF = AssetType.ETF;
  private static final Instant T1 = Instant.parse("2024-01-15T14:30:00Z");
  private static final Instant T2 = Instant.parse("2024-03-20T14:30:00Z");
  private static final Instant T3 = Instant.parse("2024-06-10T14:30:00Z");

  private final TransactionRecordingServiceImpl service = new TransactionRecordingServiceImpl();

  // -------------------------------------------------------------------------
  // Scenario: Single buy, then partial sell
  //
  // BUY 100 shares @ $100.00 with $9.99 commission
  // cashDelta = -(100 × $100.00) - $9.99 = -$10,009.99
  // grossValue = 100 × $100.00 = $10,000.00
  // CRA ACB = $10,009.99 (commission IS part of ACB per CRA)
  //
  // SELL 40 shares @ $120.00 with $9.99 commission
  // cashDelta = (40 × $120.00) - $9.99 = +$4,790.01
  // grossValue = 40 × $120.00 = $4,800.00
  // ACB per unit = $10,009.99 / 100 = $100.0999
  // ACB of 40 = 40 × $100.0999 = $4,003.996 → $4,004.00 (rounded)
  //
  // Capital Gain = Proceeds(cashDelta) − ACB sold
  // = $4,790.01 − $4,004.00
  // = $786.01
  //
  // If you wrongly use grossValue as proceeds:
  // Capital Gain = $4,800.00 − $4,004.00 = $796.00 ← WRONG, overstates gain by
  // $10
  // -------------------------------------------------------------------------

  @Test
  @DisplayName("replayTransaction SELL: realized gain uses net proceeds (cashDelta), not grossValue")
  void replayTransaction_sell_uses_cashDelta_for_cra_correct_proceeds() {
    Account account = new Account(AccountId.newId(), "test-account",
        AccountType.NON_REGISTERED_INVESTMENT, CAD, PositionStrategy.ACB);
    AssetSymbol symbol = new AssetSymbol("VFV.TO");

    // Buy 100 shares @ 100.00 + 9.99 commission
    // Total Cost = 10,009.99 | Unit ACB = 100.0999
    Transaction buyTx = TransactionFixtures.buy().accountId(account.getAccountId())
        .symbol(symbol.value()).quantity(100).pricePerUnit(new Price(Money.of(100.00, "CAD")))
        .commission(9.99).build();

    service.replayTransaction(account, buyTx);

    // Replay a sell of 40 shares @ 120.00 - 9.99 commission
    // Net Proceeds (cashDelta) = 4,790.01
    Transaction sellTx = TransactionFixtures.sell().accountId(account.getAccountId())
        .symbol(symbol.value()).quantity(40).pricePerUnit(new Price(Money.of(120.00, "CAD")))
        .commission(9.99).build();

    service.replayTransaction(account, sellTx);

    // Check the resulting Position state
    AcbPosition position = (AcbPosition) account.getPosition(symbol).orElseThrow();

    // Verify Remaining Quantity: 100 - 40 = 60
    assertThat(position.totalQuantity().amount()).isEqualByComparingTo(new BigDecimal("60"));

    // Verify Remaining Cost Basis:
    // Old Total Cost (10009.99) - [(40/100) * 10009.99] = 10009.99 - 4003.996 =
    // 6005.994
    // Note: Adjust the expected decimal based on your Money/Quantity scale
    assertThat(position.totalCostBasis().amount()).as(
            "Remaining cost basis should be reduced proportionally")
        .isEqualByComparingTo(new BigDecimal("6005.994").setScale(MONEY_PRECISION, ROUNDING_MODE));

    // 4. Assert - Verify the Result Record specifically
    List<RealizedGainRecord> gains = account.getRealizedGains();
    assertThat(gains).hasSize(1);

    // CRA Math: Net Proceeds (4790.01) - ACB of shares sold (4004.00) = 786.01
    assertThat(gains.getFirst().realizedGainLoss().amount()
        .setScale(2, ROUNDING_MODE)).isEqualByComparingTo(new BigDecimal("786.01"));
  }

  @Nested
  @DisplayName("ACB Position (average cost)")
  class AcbPositionTests {

    @Test
    @DisplayName("BUY: commission is included in ACB, use cashDelta.abs(), not grossValue")
    void buy_includes_commission_in_acb() {
      // Arrange
      Money cashDeltaOnBuy = new Money(new BigDecimal("-10009.99"),
          CAD); // what leaves your account
      Money grossValue = new Money(new BigDecimal("10000.00"), CAD); // qty × price, no commission
      Quantity qty = new Quantity(new BigDecimal("100"));

      AcbPosition empty = AcbPosition.empty(VFV, ETF, CAD);

      // Act — using cashDelta.abs() (CRA-correct)
      var resultWithFee = empty.buy(qty, cashDeltaOnBuy.abs(), T1);
      var resultWithoutFee = empty.buy(qty, grossValue, T1);

      AcbPosition positionCraCorrect = (AcbPosition) resultWithFee.newPosition();
      AcbPosition positionBuggy = (AcbPosition) resultWithoutFee.newPosition();

      // Assert — CRA correct: ACB = $10,009.99
      assertThat(positionCraCorrect.totalCostBasis().amount()).as("CRA ACB must include commission")
          .isEqualByComparingTo(new BigDecimal("10009.99"));

      // Assert — Buggy: ACB = $10,000.00 (understates ACB, overstates future capital
      // gains)
      assertThat(positionBuggy.totalCostBasis().amount()).as(
              "grossValue-based ACB is missing the commission — this overstates future capital gains")
          .isEqualByComparingTo(new BigDecimal("10000.00"));

      // This assertion documents the dollar impact of the bug
      assertThat(positionCraCorrect.totalCostBasis().amount()).as(
          "CRA-correct ACB should be $9.99 higher than grossValue-based ACB").isEqualByComparingTo(
          positionBuggy.totalCostBasis().amount().add(new BigDecimal("9.99")));
    }

    @Test
    @DisplayName("SELL: proceeds are NET of commission — use cashDelta, not grossValue")
    void sell_proceeds_are_net_of_commission() {
      // Arrange — establish position first using CRA-correct ACB
      Money buyAmount = new Money(new BigDecimal("10009.99"), CAD); // cashDelta.abs() on buy
      Quantity buyQty = new Quantity(new BigDecimal("100"));

      AcbPosition empty = AcbPosition.empty(VFV, ETF, CAD);

      // Using var here relies on the covariant return mentioned above
      var buyResult = empty.buy(buyQty, buyAmount, T1);
      AcbPosition position = (AcbPosition) buyResult.newPosition();

      // Sell 40 shares
      // cashDelta on sell = (40 × 120) - 9.99 = +4790.01 (net proceeds, CRA correct)
      // grossValue = (40 × 120) = 4800.00 (overstates proceeds)
      Quantity sellQty = new Quantity(new BigDecimal("40"));
      Money cashDeltaOnSell = new Money(new BigDecimal("4790.01"), CAD);
      Money grossValueSell = new Money(new BigDecimal("4800.00"), CAD);

      // ACB per unit = 10009.99 / 100 = 100.0999
      // ACB of 40 = 40 × 100.0999 = 4003.996 → stored as is, let Money handle scale
      Money expectedAcbSold = new Money(new BigDecimal("4003.996"), CAD);

      // No cast needed! The compiler knows 'sell' returns
      // ApplyResult.Sale<AcbPosition>
      var craCorrect = position.sell(sellQty, cashDeltaOnSell, T2);
      var buggy = position.sell(sellQty, grossValueSell, T2);

      // Assert cost basis sold — should be identical regardless of which proceeds
      // value used
      assertThat(craCorrect.costBasisSold().amount()).as("ACB of shares sold")
          .isEqualByComparingTo(expectedAcbSold.amount());

      // Assert realized gain — CRA correct
      // 4790.01 - 4003.996 = 786.014
      assertThat(craCorrect.realizedGainLoss().amount()).as(
              "CRA-correct realized gain (net proceeds minus ACB)")
          .isEqualByComparingTo(new BigDecimal("786.014"));

      // Assert realized gain — buggy (grossValue inflates gain by $9.99 commission)
      // 4800.00 - 4003.996 = 796.004
      assertThat(buggy.realizedGainLoss().amount()).as(
              "grossValue-based realized gain overstates gain by the sell commission amount")
          .isEqualByComparingTo(new BigDecimal("796.004"));

      // The delta between correct and wrong is exactly the sell commission
      BigDecimal gainDifference = buggy.realizedGainLoss().amount()
          .subtract(craCorrect.realizedGainLoss().amount());
      assertThat(gainDifference).as("Overstatement equals exactly the sell commission ($9.99)")
          .isEqualByComparingTo(new BigDecimal("9.99").setScale(MONEY_PRECISION,
              ROUNDING_MODE)); // 4800.00-4790.01 = 9.99 applied to acb math
    }

    @Test
    @DisplayName("Full round trip: buy twice, sell partial — ACB per unit recalculates correctly")
    void full_round_trip_two_buys_partial_sell() {
      /*
       * CRA worked example (superficially modelled on CRA Folio S3-F4-C1 example):
       *
       * Jan 15: BUY 100 shares @ $100.00, commission $9.99
       * ACB addition = $10,009.99
       * Total ACB = $10,009.99 / 100 shares
       *
       * Mar 20: BUY 50 shares @ $110.00, commission $9.99
       * ACB addition = $5,509.99
       * Total ACB = $15,519.98 / 150 shares
       * ACB per unit = $15,519.98 / 150 = $103.4665...
       *
       * Jun 10: SELL 60 shares @ $130.00, commission $9.99
       * Net proceeds (cashDelta) = (60 × $130.00) - $9.99 = $7,790.01
       * ACB of 60 = 60 × $103.4665... = $6,207.99
       * Capital Gain = $7,790.01 - $6,207.99 = $1,582.02
       *
       * Remaining position: 90 shares
       * Remaining ACB = $15,519.98 - $6,207.99 = $9,311.99
       */

      AcbPosition pos = AcbPosition.empty(VFV, ETF, CAD);

      // BUY 1
      pos = ((ApplyResult<AcbPosition>) pos.buy(Quantity.of(100), Money.of(10009.99, "CAD"),
          T1)).newPosition();

      assertThat(pos.totalCostBasis().amount()).as("After BUY 1: ACB should be $10,009.99")
          .isEqualByComparingTo(new BigDecimal("10009.99"));
      assertThat(pos.totalQuantity().amount()).isEqualByComparingTo(new BigDecimal("100"));

      // BUY 2
      pos = ((ApplyResult<AcbPosition>) pos.buy(Quantity.of(50), Money.of(5509.99, "CAD"),
          T2)).newPosition();

      assertThat(pos.totalCostBasis().amount()).as("After BUY 2: ACB should be $15,519.98")
          .isEqualByComparingTo(new BigDecimal("15519.98"));
      assertThat(pos.totalQuantity().amount()).isEqualByComparingTo(new BigDecimal("150"));

      // SELL 60 — using net proceeds (cashDelta), CRA correct
      Money netProceeds = Money.of(7790.01, "CAD"); // (60 × 130) - 9.99
      ApplyResult.Sale<AcbPosition> sale = (ApplyResult.Sale<AcbPosition>) pos.sell(Quantity.of(60),
          netProceeds, T3);

      // ACB per unit = 15519.98 / 150 = 103.46653...
      // ACB sold = 60 × 103.46653... = 6207.992
      BigDecimal acbPerUnit = new BigDecimal("15519.98").divide(new BigDecimal("150"),
          MONEY_PRECISION, ROUNDING_MODE);

      BigDecimal expectedAcbSold = acbPerUnit.multiply(new BigDecimal("60"));

      assertThat(sale.costBasisSold().amount().setScale(2, ROUNDING_MODE)).as(
          "ACB of 60 shares sold").isEqualByComparingTo(expectedAcbSold.setScale(2, ROUNDING_MODE));

      // Capital gain = 7790.01 - 6207.992 = 1582.018
      BigDecimal expectedGain = new BigDecimal("7790.01").subtract(expectedAcbSold);
      assertThat(sale.realizedGainLoss().amount().setScale(3, ROUNDING_MODE)).as(
              "Realized capital gain")
          .isEqualByComparingTo(expectedGain.stripTrailingZeros().setScale(3, ROUNDING_MODE));

      // Remaining position
      AcbPosition remaining = sale.newPosition();
      assertThat(remaining.totalQuantity().amount()).as("90 shares remain")
          .isEqualByComparingTo(new BigDecimal("90"));

      BigDecimal expectedRemainingAcb = new BigDecimal("15519.98").subtract(expectedAcbSold);
      assertThat(remaining.totalCostBasis().amount().setScale(2, ROUNDING_MODE)).as(
          "Remaining ACB = original total minus ACB sold").isEqualByComparingTo(
          expectedRemainingAcb.stripTrailingZeros().setScale(2, ROUNDING_MODE));
    }
  }

  @Nested
  @DisplayName("FIFO Position")
  class FifoPositionTests {

    @Test
    @DisplayName("FIFO partial lot: complement math prevents rounding drift")
    void fifo_partial_lot_no_rounding_leak() {
      /*
       * BUY Lot 1: 33 shares @ $10.00 + $9.99 commission = $339.99
       * costPerUnit = $339.99 / 33 = $10.30272... (repeating)
       *
       * SELL 10 shares
       * Consumed cost = 10 × (339.99/33) = $103.027...
       * Remaining cost = $339.99 - $103.027... (complement — not re-derived)
       *
       * The complement approach guarantees:
       * consumedCost + remainingCost == lot.costBasis() exactly
       *
       * The BUG (per-unit × qty twice) accumulates rounding error over many partial
       * sells.
       */

      FifoPosition pos = FifoPosition.empty(VFV, ETF, CAD);
      Money lotCost = Money.of(339.99, "CAD"); // 33 shares, commission included
      pos = ((ApplyResult<FifoPosition>) pos.buy(Quantity.of(33), lotCost, T1)).newPosition();

      Money netProceeds = Money.of(400.01, "CAD"); // 10 × $41.00 - 9.99
      ApplyResult.Sale<FifoPosition> sale = (ApplyResult.Sale<FifoPosition>) pos.sell(
          Quantity.of(10), netProceeds, T2);

      FifoPosition remaining = sale.newPosition();

      // The invariant: consumedCost + remainingCost must equal the original lot cost
      // exactly
      BigDecimal consumed = sale.costBasisSold().amount();
      BigDecimal remaining_ = remaining.lots().getFirst().costBasis().amount();

      assertThat(consumed.add(remaining_)).as(
              "consumed + remaining must equal original lot cost exactly — no rounding leak")
          .isEqualByComparingTo(lotCost.amount());

      assertThat(remaining.totalQuantity().amount()).isEqualByComparingTo(new BigDecimal("23"));
    }
  }

  @Nested
  @DisplayName("Replay contract: which value should the caller pass in?")
  class ReplayContractTests {

    /*
     * This is the money test. It simulates what replayTransaction does
     * and asserts what the correct input to position.sell() must be.
     *
     * For a Canadian app:
     * position.sell() must receive cashDelta (net proceeds)
     * NOT grossValue (qty × price before commission)
     *
     * replayTransaction currently passes cashDelta — that IS correct for Canada.
     * replayFullTransaction was fixed to use cashDelta (net proceeds) — same as
     * replayTransaction.
     *
     *
     * replayFullTransaction SELL should be:
     * position.sell(qty, tx.cashDelta(), at) ← proceeds to position
     * account.deposit(tx.cashDelta(), "REPLAY SELL...") ← cash (same value, net)
     *
     * The fee was already captured as a separate FEE transaction in your tx log.
     * If it wasn't — that's a data ingestion bug, not a replay bug.
     */

    @Test
    @DisplayName("cashDelta (net proceeds) produces CRA-correct capital gain")
    void cashDelta_is_correct_for_cra_sell() {
      var pos = AcbPosition.empty(VFV, ETF, CAD)
          .buy(Quantity.of(100), Money.of(10009.99, "CAD"), T1).newPosition();

      // Simulate replayTransaction (current code — cashDelta)
      Money cashDeltaNet = Money.of(4790.01, "CAD"); // (40 × 120) - 9.99
      ApplyResult.Sale<AcbPosition> withCashDelta = (ApplyResult.Sale<AcbPosition>) pos.sell(
          Quantity.of(40), cashDeltaNet, T2);

      // Simulate replayFullTransaction (current code — grossValue)
      Money grossValue = Money.of(4800.00, "CAD"); // 40 × 120, ignores commission
      ApplyResult.Sale<AcbPosition> withGrossValue = (ApplyResult.Sale<AcbPosition>) pos.sell(
          Quantity.of(40), grossValue, T2);

      // CRA-correct: capital gain does NOT include the commission as income
      assertThat(withCashDelta.realizedGainLoss().amount()).as(
              "cashDelta path: CRA-correct gain (commission not counted as income)")
          .isLessThan(withGrossValue.realizedGainLoss().amount());

      // The difference is exactly the sell commission
      BigDecimal commissionOnSell = new BigDecimal("9.99");
      BigDecimal gainDifference = withGrossValue.realizedGainLoss().amount()
          .subtract(withCashDelta.realizedGainLoss().amount());

      assertThat(gainDifference).as(
              "grossValue path overstates gain by exactly the sell commission")
          .isEqualByComparingTo(commissionOnSell);
    }
  }
}

@Setter
@Accessors(fluent = true, chain = true)
class TransactionFixtures {
  private AccountId accountId;
  private String symbol;
  private double quantity;
  private Price pricePerUnit;
  private double commission;
  private TransactionType type;

  public static TransactionFixtures buy() {
    return new TransactionFixtures().type(BUY);
  }

  public static TransactionFixtures sell() {
    return new TransactionFixtures().type(SELL);
  }

  public Transaction build() {
    Money cashDelta;
    if (type == BUY) {
      BigDecimal amount = pricePerUnit.amount().multiply(BigDecimal.valueOf(quantity))
          .multiply(BigDecimal.valueOf(-1)).subtract(BigDecimal.valueOf(commission))
          .setScale(Precision.getMoneyPrecision(), RoundingMode.HALF_EVEN);

      cashDelta = new Money(amount, pricePerUnit.currency());

    } else {
      BigDecimal amount = pricePerUnit.amount().multiply(BigDecimal.valueOf(quantity))
          .subtract(BigDecimal.valueOf(commission))
          .setScale(Precision.getMoneyPrecision(), RoundingMode.HALF_EVEN);
      cashDelta = new Money(amount, pricePerUnit.currency());
    }

    BigDecimal commissionAmount = BigDecimal.valueOf(commission)
        .setScale(Precision.getMoneyPrecision(), RoundingMode.HALF_EVEN);
    Money feeMoney = new Money(commissionAmount, pricePerUnit.currency());
    List<Fee> fees = List.of(
        new Fee(FeeType.COMMISSION, feeMoney, feeMoney, ExchangeRate.identity(CAD, Instant.now()),
            Instant.now(), new Fee.FeeMetadata(null)));

    Transaction tx = new Transaction(TransactionId.newId(), this.accountId, this.type,
        new Transaction.TradeExecution(new AssetSymbol(this.symbol), Quantity.of(this.quantity),
            this.pricePerUnit), null, cashDelta, fees, "notes", Instant.now(), null,
        TransactionMetadata.manual(AssetType.STOCK));

    return tx;
  }
}