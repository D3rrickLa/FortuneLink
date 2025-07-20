package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.SimpleExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class PortfolioTestPart3 {
    private UUID userId;
	private String name;
	private String desc;
	private Money portfolioCashBalance;
	private ExchangeRateService exchangeRateService;

    private Currency usd;
    private Currency cad;
    private Portfolio portfolio;
    private UUID portfolioId;
    private Instant testDate;

    @BeforeEach
    void init() {
        usd = Currency.getInstance("USD");
        cad = Currency.getInstance("CAD");
        portfolioId = UUID.randomUUID();
        userId = UUID.randomUUID();
        name = "SOME NAME";
        desc = "DESC";
        exchangeRateService = new SimpleExchangeRateService();
        portfolioCashBalance = new Money("10000", cad);

        // Start with a substantial cash balance for all tests
        portfolio = new Portfolio(portfolioId, userId, name, desc, portfolioCashBalance, cad, exchangeRateService);
        testDate = Instant.now();
    }

    // --- Helper methods for test setup ---
    private AssetIdentifier createAppleStockIdentifier() {
        return new AssetIdentifier("APPL", AssetType.STOCK, "US0378331005", "Apple", "NASDAQ", "DESCRIPTION");
    }

    private CommonTransactionInput createCommonInput(TransactionType type, String description, List<Fee> fees) {
        return new CommonTransactionInput(
            UUID.randomUUID(), null, type,
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, description, testDate, testDate),
            fees
        );
    }

    // --- Test Cases ---

    @Test
    void testReverseDepositTransaction() {
        // Initial cash balance: 10000.00 CAD
        Money depositAmount = new Money("500.00", cad);
        List<Fee> depositFees = List.of(new Fee(FeeType.DEPOSIT_FEE, new Money("0.10", cad))); // Fee for deposit

        // Expected net cash impact of original deposit: 500 - 0.10 = 499.90
        Money expectedCashAfterDeposit = portfolio.getPortfolioCashBalance().add(depositAmount).subtract(depositFees.get(0).amount());

        // 1. Record a DEPOSIT transaction
        CashflowTransactionDetails depositDetails = new CashflowTransactionDetails(
            depositAmount, depositAmount, Money.ZERO(cad), null
        );
        CommonTransactionInput depositInput = createCommonInput(TransactionType.DEPOSIT, "Test Deposit", depositFees);
        portfolio.recordCashflow(depositDetails, depositInput, testDate);

        // Assert state after original deposit
        assertEquals(1, portfolio.getTransactions().size(), "Should have 1 transaction after deposit.");
        assertEquals(expectedCashAfterDeposit, portfolio.getPortfolioCashBalance(), "Cash balance should be correct after deposit.");
        UUID originalTxId = portfolio.getTransactions().get(0).getTransactionId();

        // 2. Reverse the DEPOSIT transaction
        String reversalReason = "Customer requested reversal";
        Instant reversalDate = testDate.plusSeconds(100);
        portfolio.reverseTransaction(originalTxId, reversalReason, reversalDate);

        // Assert state after reversal
        assertEquals(3, portfolio.getTransactions().size(), "Should have 3 transactions: original, sub-reversal (withdrawal), and main REVERSAL.");

        // Find the original deposit transaction
        Transaction originalDepositTx = portfolio.getTransactions().stream()
            .filter(t -> t.getTransactionId().equals(originalTxId))
            .findFirst().orElseThrow();
        assertEquals(TransactionType.DEPOSIT, originalDepositTx.getTransactionType());

        // Find the sub-reversal (WITHDRAWAL) transaction
        Transaction reversalWithdrawalTx = portfolio.getTransactions().stream()
            .filter(t -> t.getParentTransactionId() != null && t.getParentTransactionId().equals(originalTxId) && t.getTransactionType() == TransactionType.WITHDRAWAL)
            .findFirst().orElseThrow();
        assertEquals(TransactionType.WITHDRAWAL, reversalWithdrawalTx.getTransactionType());
        assertEquals(depositAmount, reversalWithdrawalTx.getTotalTransactionAmount(), "Reversal withdrawal amount should match original deposit amount.");
        assertEquals(depositFees, reversalWithdrawalTx.getFees(), "Reversal withdrawal fees should match original deposit fees.");

        // Find the main REVERSAL transaction
        Transaction mainReversalTx = portfolio.getTransactions().stream()
            .filter(t -> t.getParentTransactionId() != null && t.getParentTransactionId().equals(originalTxId) && t.getTransactionType() == TransactionType.REVERSAL)
            .findFirst().orElseThrow();
        assertEquals(TransactionType.REVERSAL, mainReversalTx.getTransactionType());
        assertEquals(reversalReason, mainReversalTx.getTransactionMetadata().description());
        assertEquals(originalTxId, mainReversalTx.getParentTransactionId());

        // Expected final cash balance: Initial cash - (original fee + reversal fee)
        // 10000.00 - (0.10 + 0.10) = 9999.80 CAD
        Money expectedFinalCashBalance = new Money("9999.80", cad);
        assertEquals(expectedFinalCashBalance, portfolio.getPortfolioCashBalance(), "Cash balance should reflect original and reversal fees.");
    }

    @Test
    void testReverseWithdrawalTransaction() {
        // Initial cash balance: 10000.00 CAD
        Money withdrawalAmount = new Money("300.00", cad);
        List<Fee> withdrawalFees = List.of(new Fee(FeeType.BROKERAGE, new Money("0.08", cad))); // Fee for withdrawal

        // Expected net cash impact of original withdrawal: -300 - 0.08 = -300.08
        Money expectedCashAfterWithdrawal = portfolio.getPortfolioCashBalance().subtract(withdrawalAmount).subtract(withdrawalFees.get(0).amount());

        // 1. Record a WITHDRAWAL transaction
        CashflowTransactionDetails withdrawalDetails = new CashflowTransactionDetails(
            withdrawalAmount, withdrawalAmount, Money.ZERO(cad), null
        );
        CommonTransactionInput withdrawalInput = createCommonInput(TransactionType.WITHDRAWAL, "Test Withdrawal", withdrawalFees);
        portfolio.recordCashflow(withdrawalDetails, withdrawalInput, testDate);

        // Assert state after original withdrawal
        assertEquals(1, portfolio.getTransactions().size(), "Should have 1 transaction after withdrawal.");
        assertEquals(expectedCashAfterWithdrawal, portfolio.getPortfolioCashBalance(), "Cash balance should be correct after withdrawal.");
        UUID originalTxId = portfolio.getTransactions().get(0).getTransactionId();

        // 2. Reverse the WITHDRAWAL transaction
        String reversalReason = "Error in withdrawal request";
        Instant reversalDate = testDate.plusSeconds(100);
        portfolio.reverseTransaction(originalTxId, reversalReason, reversalDate);

        // Assert state after reversal
        assertEquals(3, portfolio.getTransactions().size(), "Should have 3 transactions: original, sub-reversal (deposit), and main REVERSAL.");

        // Find the sub-reversal (DEPOSIT) transaction
        Transaction reversalDepositTx = portfolio.getTransactions().stream()
            .filter(t -> t.getParentTransactionId() != null && t.getParentTransactionId().equals(originalTxId) && t.getTransactionType() == TransactionType.DEPOSIT)
            .findFirst().orElseThrow();
        assertEquals(TransactionType.DEPOSIT, reversalDepositTx.getTransactionType());
        assertEquals(withdrawalAmount, reversalDepositTx.getTotalTransactionAmount(), "Reversal deposit amount should match original withdrawal amount.");
        assertEquals(withdrawalFees, reversalDepositTx.getFees(), "Reversal deposit fees should match original withdrawal fees.");

        // Expected final cash balance: Initial cash - (original fee + reversal fee)
        // 10000.00 - (0.08 + 0.08) = 9999.84 CAD
        Money expectedFinalCashBalance = new Money("9999.84", cad);
        assertEquals(expectedFinalCashBalance, portfolio.getPortfolioCashBalance(), "Cash balance should reflect original and reversal fees.");
    }

    @Test
    void testReverseBuyTransaction() {
        // Initial cash balance: 10000.00 CAD
        AssetIdentifier apple = createAppleStockIdentifier();
        BigDecimal quantity = new BigDecimal("10");
        Money pricePerUnit = new Money("150.00", usd); // USD price
        ExchangeRate usdToCadRate = new ExchangeRate(usd, cad, new BigDecimal("1.25"), testDate, "TestFX");

        // Calculate expected values for original BUY
        Money assetValueInAssetCurrency = pricePerUnit.multiply(quantity); // 1500.00 USD
        Money assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad, usdToCadRate); // 1500 * 1.25 = 1875.00 CAD

        List<Fee> buyFees = List.of(
            new Fee(FeeType.BROKERAGE, new Money("5.00", cad)), // CAD brokerage fee
            new Fee(FeeType.FOREIGN_EXCHANGE_CONVERSION, new Money("1.00", usd)) // USD FX fee
        );
        Money totalFeesInPortfolioCurrency = buyFees.get(0).amount().add(buyFees.get(1).amount().convertTo(cad, usdToCadRate)); // 5.00 + 1.00 * 1.25 = 6.25 CAD
        Money totalFeesInAssetCurrency = buyFees.get(1).amount().add(buyFees.get(0).amount().convertTo(usd, new ExchangeRate(cad, usd, new BigDecimal("0.80"), testDate, "TestFX"))); // 1.00 + 5.00 * 0.80 = 5.00 USD

        Money costBasisInPortfolioCurrency = assetValueInPortfolioCurrency.add(totalFeesInPortfolioCurrency); // 1875.00 + 6.25 = 1881.25 CAD
        Money costBasisInAssetCurrency = assetValueInAssetCurrency.add(totalFeesInAssetCurrency); // 1500.00 + 5.00 = 1505.00 USD

        AssetTransactionDetails buyDetails = new AssetTransactionDetails(
            apple, quantity, pricePerUnit, assetValueInAssetCurrency, assetValueInPortfolioCurrency,
            costBasisInPortfolioCurrency, costBasisInAssetCurrency,
            totalFeesInPortfolioCurrency, totalFeesInAssetCurrency
        );
        CommonTransactionInput buyInput = createCommonInput(TransactionType.BUY, "Test Stock Buy", buyFees);

        Money cashBeforeBuy = portfolio.getPortfolioCashBalance(); // 10000.00 CAD

        // 1. Record the BUY transaction
        portfolio.recordAssetPurchase(buyDetails, buyInput, testDate);

        // Assert state after original BUY
        assertEquals(1, portfolio.getTransactions().size(), "Should have 1 transaction after buy.");
        assertEquals(1, portfolio.getAssetHoldings().size(), "Should have 1 asset holding after buy.");
        assertEquals(cashBeforeBuy.subtract(costBasisInPortfolioCurrency.add(totalFeesInPortfolioCurrency)), portfolio.getPortfolioCashBalance(), "Cash balance should be correct after buy."); // Assuming recordAssetPurchase adds commonInput fees to costBasis

        UUID originalTxId = portfolio.getTransactions().get(0).getTransactionId();
        Money cashAfterBuy = portfolio.getPortfolioCashBalance();
        AssetHolding appleHoldingAfterBuy = portfolio.getAssetHoldings().get(0);

        // 2. Reverse the BUY transaction
        String reversalReason = "Accidental purchase";
        Instant reversalDate = testDate.plusSeconds(100);
        portfolio.reverseTransaction(originalTxId, reversalReason, reversalDate);

        // Assert state after reversal
        assertEquals(3, portfolio.getTransactions().size(), "Should have 3 transactions: original, sub-reversal (sell), and main REVERSAL.");

        // Verify asset holding is reversed
        assertEquals(0, portfolio.getAssetHoldings().size(), "Asset holding should be removed after buy reversal."); // Or quantity is zero if you keep empty holdings

        // Verify cash balance is reversed (back to initial, assuming no new fees on reversal sell)
        // Original BUY cost: costBasisInPortfolioCurrency (1881.25 CAD) + commonInput fees (6.25 CAD) = 1887.50 CAD
        // Cash before BUY: 10000.00. Cash after BUY: 10000.00 - 1887.50 = 8112.50
        // Reversal SELL brings back 1887.50 (original cost basis + fees), assuming no new fees on sell reversal.
        // Final cash should be 10000.00 CAD.
        Money expectedFinalCashBalance = new Money("10000.00", cad);
        assertEquals(expectedFinalCashBalance, portfolio.getPortfolioCashBalance(), "Cash balance should revert to initial after buy reversal.");
    }

    @Test
    void testReverseSellTransaction() {
        // Initial cash balance: 10000.00 CAD
        AssetIdentifier apple = createAppleStockIdentifier();
        BigDecimal initialQuantity = new BigDecimal("20");
        Money initialCostBasis = new Money("2000.00", usd); // Total ACB for 20 shares
        
        // Setup initial asset holding
        AssetHolding initialHolding = new AssetHolding(UUID.randomUUID(), portfolioId, apple, initialQuantity, initialCostBasis, testDate);
        portfolio.getAssetHoldings().add(initialHolding); // Manually add for test setup

        Money cashBeforeSale = portfolio.getPortfolioCashBalance(); // 10000.00 CAD

        // 1. Record a SELL transaction
        BigDecimal quantityToSell = new BigDecimal("10");
        Money salePricePerUnit = new Money("160.00", usd); // USD price
        ExchangeRate usdToCadRate = new ExchangeRate(usd, cad, new BigDecimal("1.20"), testDate, "TestFX");

        Money assetValueInAssetCurrency = salePricePerUnit.multiply(quantityToSell); // 1600.00 USD
        Money assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad, usdToCadRate); // 1600 * 1.20 = 1920.00 CAD

        List<Fee> sellFees = List.of(
            new Fee(FeeType.BROKERAGE, new Money("7.00", cad)), // CAD brokerage fee
            new Fee(FeeType.FOREIGN_EXCHANGE_CONVERSION, new Money("1.50", usd)) // USD FX fee
        );
        Money totalFeesInPortfolioCurrency = sellFees.get(0).amount().add(sellFees.get(1).amount().convertTo(cad, usdToCadRate)); // 7.00 + 1.50 * 1.20 = 8.80 CAD
        Money totalFeesInAssetCurrency = sellFees.get(1).amount().add(sellFees.get(0).amount().convertTo(usd, new ExchangeRate(cad, usd, new BigDecimal("0.833333"), testDate, "TestFX"))); // 1.50 + 7.00 * 0.833333 = 7.333331 USD

        // For SELL, costBasisInPortfolioCurrency in details usually means proceeds.
        Money saleProceedsInPortfolioCurrency = assetValueInPortfolioCurrency.subtract(totalFeesInPortfolioCurrency); // 1920.00 - 8.80 = 1911.20 CAD
        Money saleProceedsInAssetCurrency = assetValueInAssetCurrency.subtract(totalFeesInAssetCurrency); // 1600.00 - 7.333331 = 1592.666669 USD

        AssetTransactionDetails sellDetails = new AssetTransactionDetails(
            apple, quantityToSell, salePricePerUnit, assetValueInAssetCurrency, assetValueInPortfolioCurrency,
            saleProceedsInPortfolioCurrency, saleProceedsInAssetCurrency, // These are proceeds for sell
            totalFeesInPortfolioCurrency, totalFeesInAssetCurrency
        );
        CommonTransactionInput sellInput = createCommonInput(TransactionType.SELL, "Test Stock Sell", sellFees);

        portfolio.recordAssetSale(sellDetails, sellInput, testDate);

        // Assert state after original SELL
        assertEquals(1, portfolio.getTransactions().size(), "Should have 1 transaction after sell.");
        assertEquals(1, portfolio.getAssetHoldings().size(), "Should still have 1 asset holding after sell.");
        assertEquals(initialQuantity.subtract(quantityToSell), portfolio.getAssetHoldings().get(0).getTotalQuantity(), "Asset quantity should decrease after sell.");
        assertEquals(cashBeforeSale.add(saleProceedsInPortfolioCurrency), portfolio.getPortfolioCashBalance(), "Cash balance should be correct after sell.");

        UUID originalTxId = portfolio.getTransactions().get(0).getTransactionId();
        Money cashAfterSale = portfolio.getPortfolioCashBalance();
        AssetHolding appleHoldingAfterSale = portfolio.getAssetHoldings().get(0);

        // 2. Reverse the SELL transaction
        String reversalReason = "Customer changed mind";
        Instant reversalDate = testDate.plusSeconds(100);
        portfolio.reverseTransaction(originalTxId, reversalReason, reversalDate);

        // Assert state after reversal
        assertEquals(3, portfolio.getTransactions().size(), "Should have 3 transactions: original, sub-reversal (buy), and main REVERSAL.");

        // Verify asset holding is reversed
        assertEquals(initialQuantity, portfolio.getAssetHoldings().get(0).getTotalQuantity(), "Asset quantity should revert to initial after sell reversal.");

        // Verify cash balance is reversed (back to initial, assuming no new fees on reversal buy)
        // Original SELL brought in: saleProceedsInPortfolioCurrency (1911.20 CAD)
        // Cash before SELL: 10000.00. Cash after SELL: 10000.00 + 1911.20 = 11911.20
        // Reversal BUY costs: 1911.20 (original proceeds), assuming no new fees on buy reversal.
        // Final cash should be 10000.00 CAD.
        Money expectedFinalCashBalance = new Money("10000.00", cad);
        assertEquals(expectedFinalCashBalance, portfolio.getPortfolioCashBalance(), "Cash balance should revert to initial after sell reversal.");
    }

    @Test
    void testReverseLiabilityIncurrenceTransaction() {
        // Initial cash balance: 10000.00 CAD
        Money loanAmount = new Money("2000.00", cad);
        List<Fee> loanFees = List.of(new Fee(FeeType.BROKERAGE, new Money("10.00", cad))); // Loan origination fee
        
        // Expected net cash inflow from loan: 2000 - 10 = 1990.00 CAD
        Money expectedCashAfterIncurrence = portfolio.getPortfolioCashBalance().add(loanAmount).subtract(loanFees.get(0).amount());

        // 1. Record a LIABILITY_INCURRENCE transaction
        LiabilityIncurrenceTransactionDetails incurrenceDetails = new LiabilityIncurrenceTransactionDetails(
            "Car Loan", "Loan for car purchase", loanAmount, loanAmount,
            new Percentage(new BigDecimal("0.05")), Instant.now().plusSeconds(3600*24*365*5), // 5% interest, 5 years
            loanFees.get(0).amount(), Money.ZERO(usd) // totalFeesInPortfolioCurrency, totalFeesInLiabilityCurrency
        );
        CommonTransactionInput incurrenceInput = createCommonInput(TransactionType.DEPOSIT, "Loan Received", loanFees); // Loan incurrence often treated as DEPOSIT

        portfolio.recordNewLiability(incurrenceDetails, incurrenceInput, testDate);

        // Assert state after original incurrence
        assertEquals(1, portfolio.getTransactions().size(), "Should have 1 transaction after incurrence.");
        assertEquals(1, portfolio.getLiabilities().size(), "Should have 1 liability after incurrence.");
        assertEquals(expectedCashAfterIncurrence, portfolio.getPortfolioCashBalance(), "Cash balance should be correct after loan incurrence.");
        assertEquals(loanAmount, portfolio.getLiabilities().get(0).getCurrentBalance(), "Liability balance should be correct after incurrence.");

        UUID originalTxId = portfolio.getTransactions().get(0).getTransactionId();
        Money cashAfterIncurrence = portfolio.getPortfolioCashBalance();

        // 2. Reverse the LIABILITY_INCURRENCE transaction
        String reversalReason = "Loan cancelled before disbursement";
        Instant reversalDate = testDate.plusSeconds(100);
        portfolio.reverseTransaction(originalTxId, reversalReason, reversalDate);

        // Assert state after reversal
        assertEquals(3, portfolio.getTransactions().size(), "Should have 3 transactions: original, sub-reversal (withdrawal), and main REVERSAL.");

        // Verify liability is removed
        assertTrue(portfolio.getLiabilities().isEmpty(), "Liability should be removed after incurrence reversal.");

        // Verify cash balance is reversed (back to initial, considering fees)
        // Original Incurrence: +2000.00 (loan amount) - 10.00 (fee) = +1990.00
        // Reversal (Withdrawal): -1990.00 (net loan proceeds) - 0.00 (no new fees for internal reversal cashflow) = -1990.00
        // Net effect: +1990.00 - 1990.00 = 0.00
        // Final cash should be 10000.00 CAD.
        Money expectedFinalCashBalance = new Money("10000.00", cad);
        assertEquals(expectedFinalCashBalance, portfolio.getPortfolioCashBalance(), "Cash balance should revert to initial after loan incurrence reversal.");
    }

    @Test
    void testReverseLiabilityPaymentTransaction() {
        // Initial cash balance: 10000.00 CAD
        // Setup a liability first
        Money loanAmount = new Money("1500.00", cad);
        portfolio.recordNewLiability(
            new LiabilityIncurrenceTransactionDetails("Test Loan", "", loanAmount, loanAmount, new Percentage(BigDecimal.ZERO), testDate.plusSeconds(10000), Money.ZERO(cad), Money.ZERO(cad)),
            createCommonInput(TransactionType.DEPOSIT, "Initial Loan", new ArrayList<>()),
            testDate
        );
        UUID liabilityId = portfolio.getLiabilities().get(0).getLiabilityId();
        Money initialLiabilityBalance = portfolio.getLiabilities().get(0).getCurrentBalance(); // 1500.00 CAD
        Money cashAfterLoanIncurrence = portfolio.getPortfolioCashBalance(); // 10000.00 + 1500.00 = 11500.00 CAD

        // 1. Record a PAYMENT transaction
        Money paymentAmount = new Money("200.00", cad);
        Money interestPaid = new Money("10.00", cad);
        Money feesPaid = new Money("5.00", cad); // Fees part of the payment applied to liability

        // Total cash outflow from portfolio: 200 (total payment) + 0 (no separate transaction fees) = 200 CAD
        // Principal reduction on liability: 200 - 10 (interest) - 5 (fees) = 185 CAD
        Money expectedCashAfterPayment = cashAfterLoanIncurrence.subtract(paymentAmount); // 11500.00 - 200.00 = 11300.00 CAD
        Money expectedLiabilityBalanceAfterPayment = initialLiabilityBalance.subtract(new Money("185.00", cad)); // 1500.00 - 185.00 = 1315.00 CAD

        LiabilityPaymentTransactionDetails paymentDetails = new LiabilityPaymentTransactionDetails(
            liabilityId,
            paymentAmount, interestPaid, feesPaid, // In liability currency
            paymentAmount, interestPaid, feesPaid  // In portfolio currency (total cash outflow)
        );
        CommonTransactionInput paymentInput = createCommonInput(TransactionType.PAYMENT, "Test Payment", new ArrayList<>()); // No separate transaction fees

        portfolio.recordLiabilityPayment(paymentDetails, paymentInput, testDate.plusSeconds(1));

        // Assert state after original payment
        assertEquals(2, portfolio.getTransactions().size(), "Should have 2 transactions after payment."); // Loan + Payment
        assertEquals(expectedCashAfterPayment, portfolio.getPortfolioCashBalance(), "Cash balance should be correct after payment.");
        assertEquals(expectedLiabilityBalanceAfterPayment, portfolio.getLiabilities().get(0).getCurrentBalance(), "Liability balance should be correct after payment.");
        
        UUID originalTxId = portfolio.getTransactions().get(1).getTransactionId(); // Get the ID of the payment transaction
        Money cashBeforeReversal = portfolio.getPortfolioCashBalance();
        Money liabilityBalanceBeforeReversal = portfolio.getLiabilities().get(0).getCurrentBalance();

        // 2. Reverse the PAYMENT transaction
        String reversalReason = "Payment error";
        Instant reversalDate = testDate.plusSeconds(100);
        portfolio.reverseTransaction(originalTxId, reversalReason, reversalDate);

        // Assert state after reversal
        assertEquals(4, portfolio.getTransactions().size(), "Should have 4 transactions: original loan, original payment, sub-reversal (deposit), and main REVERSAL.");

        // Verify cash balance is reversed (back to before payment)
        // Original Payment: -200.00 (cash outflow)
        // Reversal (Deposit): +200.00 (cash inflow)
        // Net effect: 0.00
        // Final cash should be 11500.00 CAD (cash after loan incurrence)
        Money expectedFinalCashBalance = cashAfterLoanIncurrence;
        assertEquals(expectedFinalCashBalance, portfolio.getPortfolioCashBalance(), "Cash balance should revert to before payment after reversal.");

        // Verify liability balance is reversed (back to before payment)
        // Original Payment reduced principal by 185.00
        // Reversal adds back 185.00
        // Final liability balance should be 1500.00 CAD (initial loan amount)
        Money expectedFinalLiabilityBalance = initialLiabilityBalance;
        assertEquals(expectedFinalLiabilityBalance, portfolio.getLiabilities().get(0).getCurrentBalance(), "Liability balance should revert to before payment after reversal.");
    }

    @Test
    void testReverseNonExistentTransaction() {
        UUID nonExistentId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> {
            portfolio.reverseTransaction(nonExistentId, "Non-existent", testDate);
        }, "Should throw IllegalArgumentException if transaction to reverse does not exist.");

        assertEquals(0, portfolio.getTransactions().size(), "No transactions should be added if reversal fails due to non-existent ID.");
        assertEquals(new Money("10000.00", cad), portfolio.getPortfolioCashBalance(), "Cash balance should not change if reversal fails.");
    }

    @Test
    void testReverseReversalTransaction() {
        // First, create a transaction and reverse it
        Money depositAmount = new Money("100.00", cad);
        CashflowTransactionDetails depositDetails = new CashflowTransactionDetails(depositAmount, depositAmount, Money.ZERO(cad), null);
        CommonTransactionInput depositInput = createCommonInput(TransactionType.DEPOSIT, "Initial Deposit", new ArrayList<>());
        portfolio.recordCashflow(depositDetails, depositInput, testDate);
        UUID originalTxId = portfolio.getTransactions().get(0).getTransactionId();

        portfolio.reverseTransaction(originalTxId, "First Reversal", testDate.plusSeconds(1));
        
        // The main REVERSAL transaction is the third one (index 2)
        Transaction mainReversalTx = portfolio.getTransactions().stream()
            .filter(t -> t.getTransactionType() == TransactionType.REVERSAL && t.getParentTransactionId().equals(originalTxId))
            .findFirst().orElseThrow();
        
        // Now try to reverse the REVERSAL transaction itself
        assertThrows(IllegalArgumentException.class, () -> {
            portfolio.reverseTransaction(mainReversalTx.getTransactionId(), "Attempt to reverse reversal", testDate.plusSeconds(2));
        }, "Should throw IllegalArgumentException when attempting to reverse a REVERSAL transaction.");

        assertEquals(3, portfolio.getTransactions().size(), "No new transactions should be added if reversal of reversal fails.");
        // Cash balance should be as it was after the first reversal
        // Initial 10000 + 100 (deposit) - 100 (withdrawal for reversal) = 10000
        assertEquals(new Money("10000.00", cad), portfolio.getPortfolioCashBalance(), "Cash balance should remain unchanged after failed reversal of reversal.");
    }
}
