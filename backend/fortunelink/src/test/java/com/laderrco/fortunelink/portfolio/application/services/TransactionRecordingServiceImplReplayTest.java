package com.laderrco.fortunelink.portfolio.application.services;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.FeeType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.Rounding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Tests the replay service layer - not Position in isolation.
 * <p>
 * These tests exist to verify that:
 * 1. The correct value (cashDelta vs grossValue) is passed to position.sell()
 * 2. Realized gains are captured on Account, not silently discarded
 * 3. CRA ACB math is correct end-to-end
 * <p>
 * You need a Transaction builder/fixture that can set:
 * - transactionType
 * - execution.asset (AssetSymbol)
 * - execution.quantity
 * - execution.grossValue  (qty × price, no commission)
 * - cashDelta             (net cash movement including commission)
 * - occurredAt.timestamp
 * - metadata.assetType
 * <p>
 * Adjust the builder calls below to match your actual Transaction construction.
 */
public class TransactionRecordingServiceImplReplayTest {
    private static final Currency CAD = Currency.CAD;
    private static final AssetSymbol VFV = new AssetSymbol("VFV.TO");
    private static final Instant BUY_DATE = Instant.parse("2024-01-15T14:30:00Z");
    private static final Instant SELL_DATE = Instant.parse("2024-03-20T14:30:00Z");
    private static final Instant SELL2_DATE = Instant.parse("2024-06-10T14:30:00Z");
    public static final RoundingMode ROUNDING_MODE = Rounding.MONEY.getMode();
    public static final int MONEY_PRECISION = Precision.getMoneyPrecision();

    private TransactionRecordingServiceImpl service;
    private Account account;

    @BeforeEach
    void setUp() {
        service = new TransactionRecordingServiceImpl();
        account = new Account(
                new AccountId(UUID.randomUUID()),
                "Test TFSA",
                AccountType.TFSA,
                CAD,
                PositionStrategy.ACB);
    }

    private Transaction buildBuyTx(String quantity, String pricePerUnit, String commission, Instant at) {
        Money price = Money.of(pricePerUnit, CAD);
        Money fee = Money.of(commission, CAD);
        Quantity qty = new Quantity(new BigDecimal(quantity));
        Money grossValue = price.multiply(qty.amount().abs());
        Money cashDelta = grossValue.add(fee).negate(); // -(qty x price + commission)

        return Transaction.builder()
                .transactionId(new TransactionId(UUID.randomUUID()))
                .accountId(account.getAccountId())
                .transactionType(TransactionType.BUY)
                .execution(new Transaction.TradeExecution(VFV, qty, new Price(price)))
                .fees(List.of(buildCommissionFee(commission, at)))
                .cashDelta(cashDelta)
                .occurredAt(new TransactionDate(at))
                .notes("")
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    //
    // cashDelta is DERIVED here using the same formula as validateTradeConsistency:
    //   BUY  -> -(grossValue + fee)
    //   SELL ->   grossValue - fee
    //
    // Do NOT pass cashDelta as a raw string independently — Transaction will
    // reject it with a mismatch error if it doesn't match what it derives.
    //
    // Parameters:
    //   pricePerUnit : price per single share (NOT grossValue)
    //   commission   : positive fee amount in CAD
    // -------------------------------------------------------------------------

    private Transaction buildSellTx(String quantity, String pricePerUnit, String commission, Instant at) {
        Money price = Money.of(pricePerUnit, CAD);
        Money fee = Money.of(commission, CAD);
        Quantity qty = new Quantity(new BigDecimal(quantity));
        Money grossValue = price.multiply(qty.amount().abs());
        Money cashDelta = grossValue.subtract(fee); // (qty x price) - commission

        return Transaction.builder()
                .transactionId(new TransactionId(UUID.randomUUID()))
                .accountId(account.getAccountId())
                .transactionType(TransactionType.SELL)
                .execution(new Transaction.TradeExecution(VFV, qty, new Price(price)))
                .fees(List.of(buildCommissionFee(commission, at)))
                .cashDelta(cashDelta)
                .occurredAt(new TransactionDate(at))
                .notes("")
                .build();
    }

    /**
     * Commission fee in CAD. accountAmount and exchangeRate are null for
     * same-currency trades per Fee contract.
     */
    private Fee buildCommissionFee(String amount, Instant at) {
        return new Fee(
                FeeType.COMMISSION,
                Money.of(amount, CAD),
                null,   // accountAmount — null: same currency, no conversion
                null,   // exchangeRate  — null: same currency
                at,
                new Fee.FeeMetadata(null));
    }

    @Nested
    @DisplayName("replayTransaction — position-only path")
    class ReplayTransactionTests {

        @Test
        @DisplayName("SELL: realized gain is captured on account after replay")
        void sell_realized_gain_is_captured_on_account() {
            // BUY 100 @ $100.00 + $9.99 commission
            //   cashDelta = -(100 x 100.00 + 9.99) = -10009.99
            //   CRA ACB   = $10,009.99 (commission included)
            service.replayTransaction(account, buildBuyTx("100", "100.00", "9.99", BUY_DATE));

            // SELL 40 @ $120.00 - $9.99 commission
            //   cashDelta = (40 x 120.00) - 9.99 = +4790.01  net proceeds
            //   ACB/unit  = 10009.99 / 100 = 100.0999
            //   ACB sold  = 40 x 100.0999  = 4003.996
            //   Gain      = 4790.01 - 4003.996 = 786.014
            service.replayTransaction(account, buildSellTx("40", "120.00", "9.99", SELL_DATE));

            List<RealizedGainRecord> gains = account.getRealizedGains();
            assertThat(gains).hasSize(1);
            assertThat(gains.get(0).symbol()).isEqualTo(VFV);
            assertThat(gains.get(0).realizedGainLoss().amount())
                    .as("CRA-correct gain: net proceeds (4790.01) minus ACB sold (4003.996)")
                    .isEqualByComparingTo(new BigDecimal("786.014").setScale(MONEY_PRECISION, ROUNDING_MODE));
        }

        @Test
        @DisplayName("SELL: grossValue would overstate gain by exactly the sell commission")
        void sell_grossvalue_would_overstate_gain_by_commission() {
            // Documents the wrong number. If this assertion flips, the service regressed.
            service.replayTransaction(account, buildBuyTx("100", "100.00", "9.99", BUY_DATE));
            service.replayTransaction(account, buildSellTx("40", "120.00", "9.99", SELL_DATE));

            BigDecimal actualGain = account.getRealizedGains().get(0).realizedGainLoss().amount();
            BigDecimal grossValueBasedGain = new BigDecimal("796.004"); // (4800.00 - 4003.996) wrong

            assertThat(actualGain)
                    .as("Gain must NOT be the grossValue-based amount of 796.004")
                    .isNotEqualByComparingTo(grossValueBasedGain);

            assertThat(grossValueBasedGain.subtract(actualGain))
                    .as("Overstatement equals exactly the sell commission")
                    .isEqualByComparingTo(new BigDecimal("9.99").setScale(MONEY_PRECISION, ROUNDING_MODE));
        }

        @Test
        @DisplayName("total realized gain accumulates correctly across multiple sells")
        void total_realized_gain_accumulates_across_sells() {
            // BUY 100 @ $100.00 + $9.99 -> ACB = $10,009.99, ACB/unit = $100.0999
            service.replayTransaction(account, buildBuyTx("100", "100.00", "9.99", BUY_DATE));

            // SELL 40 @ $120.00 - $9.99 -> cashDelta = $4,790.01
            //   Gain = 4790.01 - (40 x 100.0999) = 4790.01 - 4003.996 = 786.014
            service.replayTransaction(account, buildSellTx("40", "120.00", "9.99", SELL_DATE));

            // SELL 30 @ $130.00 - $9.99 -> cashDelta = $3,890.01
            //   ACB/unit still $100.0999 (never changes on a sell in ACB method)
            //   Gain = 3890.01 - (30 x 100.0999) = 3890.01 - 3002.997 = 887.013
            service.replayTransaction(account, buildSellTx("30", "130.00", "9.99", SELL2_DATE));

            assertThat(account.getRealizedGains()).hasSize(2);

            // 786.014 + 887.013 = 1673.027
            assertThat(account.getTotalRealizedGainLoss().amount())
                    .isEqualByComparingTo(new BigDecimal("1673.027").setScale(MONEY_PRECISION, ROUNDING_MODE));
        }

        @Test
        @DisplayName("capital loss is recorded as negative realizedGainLoss")
        void sell_at_a_loss_records_negative_gain() {
            // 1. BUY 100 @ $100.00 + $9.99 commission
            // Total ACB = 10009.99. Unit ACB = 100.0999
            service.replayTransaction(account, buildBuyTx("100", "100.00", "9.99", BUY_DATE));

            // 2. SELL 40 @ $80.00 - $9.99 commission
            // Net Proceeds = (40 * 80) - 9.99 = 3200 - 9.99 = 3190.01
            // ACB of 40 shares = 40 * 100.0999 = 4003.996
            // Gain/Loss = 3190.01 - 4003.996 = -813.986
            service.replayTransaction(account, buildSellTx("40", "80.00", "9.99", SELL_DATE));

            List<RealizedGainRecord> gains = account.getRealizedGains();
            assertThat(gains).hasSize(1);

            RealizedGainRecord loss = gains.getFirst();

            // Use a delta or specific scale to handle the math
            BigDecimal expectedLoss = new BigDecimal("-813.986").setScale(MONEY_PRECISION, ROUNDING_MODE);

            assertThat(loss.realizedGainLoss().amount())
                    .as("CRA Loss: Net Proceeds (3190.01) - ACB Sold (4003.996)")
                    .isEqualByComparingTo(expectedLoss);
        }

        @Test
        @DisplayName("position quantity and cost basis are correct after replay sell")
        void position_state_is_correct_after_sell() {
            service.replayTransaction(account, buildBuyTx("100", "100.00", "9.99", BUY_DATE));
            service.replayTransaction(account, buildSellTx("40", "120.00", "9.99", SELL_DATE));

            Position remaining = account.getPosition(VFV).orElseThrow();

            assertThat(remaining.totalQuantity().amount())
                    .as("60 shares remain")
                    .isEqualByComparingTo(new BigDecimal("60"));

            // Remaining ACB = 10009.99 - (40 x 100.0999) = 10009.99 - 4003.996 = 6005.994
            assertThat(remaining.totalCostBasis().amount())
                    .as("Remaining ACB = original minus ACB of sold shares")
                    .isEqualByComparingTo(new BigDecimal("6005.994").setScale(MONEY_PRECISION, ROUNDING_MODE));
        }

        @Test
        @DisplayName("costBasisSold on the realized gain record is correct")
        void cost_basis_sold_is_recorded_correctly() {
            service.replayTransaction(account, buildBuyTx("100", "100.00", "9.99", BUY_DATE));
            service.replayTransaction(account, buildSellTx("40", "120.00", "9.99", SELL_DATE));

            RealizedGainRecord gain = account.getRealizedGains().getFirst();

            // ACB of 40 = 40 x (10009.99 / 100) = 4003.996
            assertThat(gain.costBasisSold().amount())
                    .as("Cost basis sold must reflect ACB of the 40 shares disposed")
                    .isEqualByComparingTo(new BigDecimal("4003.996").setScale(MONEY_PRECISION, ROUNDING_MODE));
        }
    }
}
