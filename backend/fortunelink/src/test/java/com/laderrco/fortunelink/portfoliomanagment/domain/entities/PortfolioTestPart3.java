package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.ReversalTransactionDetails;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class PortfolioTestPart3 {
    private UUID userId;
	private String name;
	private String desc;
	private Money initialPortfolioCashBalance;
	private ExchangeRateService exchangeRateService;

    private Currency cad;
    private Portfolio portfolio;
    private UUID portfolioId;
    private Instant testDate;

    @BeforeEach
    void init() {
        cad = Currency.getInstance("CAD");
        portfolioId = UUID.randomUUID();
        userId = UUID.randomUUID();
        name = "SOME NAME";
        desc = "DESC";
        exchangeRateService = new SimpleExchangeRateService();
        initialPortfolioCashBalance = new Money("12000", cad);

        // Start with a substantial cash balance for all tests
        portfolio = new Portfolio(portfolioId, userId, name, desc, initialPortfolioCashBalance, cad, exchangeRateService);
        testDate = Instant.now();
    }

    @Test 
    void testReversalCashflowDeposit() {
        // transaction details setup
		Money orignalMoney = new Money(2000, cad);
		Money convertedMoney = orignalMoney;
		Money totalConversionFee = new Money(0, cad);
		ExchangeRate exchangeRate = null;

		CashflowTransactionDetails cashflowTransactionDetails = new CashflowTransactionDetails(orignalMoney,
				convertedMoney, totalConversionFee, exchangeRate);

		// common transaction setup
		List<Fee> fees = new ArrayList<>();
		fees.add(new Fee(FeeType.DEPOSIT_FEE, new Money(0.05, cad)));
		TransactionMetadata transactionMetadata = new TransactionMetadata(
				TransactionStatus.COMPLETED,
				TransactionSource.MANUAL_INPUT,
				"DEPOSITED MONEY", Instant.now(),
				Instant.now());

		UUID correlationId = UUID.randomUUID();
		UUID parentId = null;
		TransactionType transactionType = TransactionType.DEPOSIT;

		CommonTransactionInput commonTransactionInput = new CommonTransactionInput(correlationId, parentId,
				transactionType, transactionMetadata, fees);

		portfolio.recordCashflow(cashflowTransactionDetails, commonTransactionInput, Instant.now());

		assertEquals(new Money(13999.95, cad), portfolio.getPortfolioCashBalance());
		assertEquals(1, portfolio.getTransactions().size());

        UUID reversalId = portfolio.getTransactions().get(0).getTransactionId();

        portfolio.reverseTransaction(reversalId, "OPP", testDate);

        assertEquals(new Money(12000, cad), portfolio.getPortfolioCashBalance());

        ReversalTransactionDetails reversalTransactionDetails = (ReversalTransactionDetails) portfolio.getTransactions().get(portfolio.getTransactions().size()-1).getTransactionDetails();
        assertEquals(reversalId, reversalTransactionDetails.getOriginalTransactionId());
        assertEquals("OPP", reversalTransactionDetails.getReason());
    }

@Test
    void testReversalCashflowWithdrawal() {
        // Initial cash: 12000.00 CAD
        Money originalWithdrawalAmount = new Money("500.00", cad);
        Money totalCashflowConversionFee = new Money("0.00", cad); // No conversion fee for this test
        ExchangeRate exchangeRate = null;

        CashflowTransactionDetails withdrawalDetails = new CashflowTransactionDetails(
                originalWithdrawalAmount, originalWithdrawalAmount, totalCashflowConversionFee, exchangeRate);

        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.WITHDRAWAL_FEE, new Money("1.50", cad))); // Withdrawal fee

        CommonTransactionInput commonInput = new CommonTransactionInput(
                UUID.randomUUID(), null, TransactionType.WITHDRAWAL,
                new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, "WITHDRAWN MONEY", testDate, testDate),
                fees);

        portfolio.recordCashflow(withdrawalDetails, commonInput, testDate);

        // Expected cash after withdrawal: 12000 (initial) - (500 (withdrawal) + 1.50 (fee)) = 11498.50 CAD
        Money expectedCashAfterWithdrawal = initialPortfolioCashBalance.subtract(originalWithdrawalAmount).subtract(new Money("1.50", cad));
        assertEquals(expectedCashAfterWithdrawal, portfolio.getPortfolioCashBalance(), "Cash balance after initial withdrawal should be correct.");
        assertEquals(1, portfolio.getTransactions().size(), "Should have 1 transaction after withdrawal.");

        UUID originalWithdrawalTxId = portfolio.getTransactions().get(0).getTransactionId();

        // Reverse the withdrawal
        portfolio.reverseTransaction(originalWithdrawalTxId, "Test withdrawal reversal", testDate.plusSeconds(10));

        // Expected cash after reversal: Should revert to initial balance
        assertEquals(initialPortfolioCashBalance, portfolio.getPortfolioCashBalance(), "Cash balance should revert to initial balance after withdrawal reversal.");
        assertEquals(2, portfolio.getTransactions().size(), "Should have 2 transactions (original + reversal) after withdrawal reversal.");
        assertTrue(portfolio.getTransactions().stream().anyMatch(t -> t.getTransactionType() == TransactionType.REVERSAL && t.getParentTransactionId().equals(originalWithdrawalTxId)), "A reversal transaction should exist.");
    }

    // --- New Test Case: Reversing a Liability Incurrence ---
    //------------------------------------------------------------------------------------------------------------------
    @Test
    void testReversalLiabilityIncurrence() {
        // Initial cash: 12000.00 CAD
        Money loanAmount = new Money("3000.00", cad);
        Money totalFeesInPortfolioCurrency = new Money("0.00", cad); // No fees for this incurrence
        Money totalFeesInLiabilityCurrency = new Money("0.00", cad); // No fees for this incurrence

        // IMPORTANT: The actual Liability object needs to be created first in recordNewLiability
        // and its ID passed to LiabilityIncurrenceTransactionDetails
        // Mocking the behavior for the test by directly calling recordNewLiability first.
        
        // This implicitly creates the Liability and populates originalIncurrenceTransactionDetails.getLiabilityId()
        portfolio.recordNewLiability(
                new LiabilityIncurrenceTransactionDetails(UUID.randomUUID(), "Test Loan Incurrence", "", loanAmount, loanAmount,
                        new Percentage(BigDecimal.ZERO), testDate.plusSeconds(10000), totalFeesInLiabilityCurrency, totalFeesInPortfolioCurrency), // Placeholder for ID
                createCommonInput(TransactionType.LIABILITY_INCURRENCE, "Initial Loan", new ArrayList<>()), // Loan incurrence is a DEPOSIT
                testDate
        );
        
        assertEquals(1, portfolio.getLiabilities().size());

        // Expected cash after loan incurrence: 12000 (initial) + 3000 (loan) = 15000.00 CAD
        Money expectedCashAfterIncurrence = initialPortfolioCashBalance.add(loanAmount);
        assertEquals(expectedCashAfterIncurrence, portfolio.getPortfolioCashBalance(), "Cash balance after loan incurrence should be correct.");
        assertEquals(1, portfolio.getTransactions().size(), "Should have 1 transaction after loan incurrence.");
        assertEquals(1, portfolio.getLiabilities().size(), "Should have 1 liability after loan incurrence.");
        assertEquals(loanAmount, portfolio.getLiabilities().get(0).getCurrentBalance(), "Liability balance should match loan amount initially.");

        UUID originalIncurrenceTxId = portfolio.getTransactions().get(0).getTransactionId();

        // Reverse the liability incurrence
        portfolio.reverseTransaction(originalIncurrenceTxId, "Test loan incurrence reversal", testDate.plusSeconds(10));

        // Expected cash after reversal: Should revert to initial balance (12000 CAD)
        assertEquals(initialPortfolioCashBalance, portfolio.getPortfolioCashBalance(), "Cash balance should revert to initial balance after loan incurrence reversal.");
        assertEquals(2, portfolio.getTransactions().size(), "Should have 2 transactions (original + reversal) after loan incurrence reversal.");
    
        // Expected liability after reversal: Should have 0 liabilities or the liability balance should be 0
        assertEquals(0, portfolio.getLiabilities().get(0).getCurrentBalance().amount().compareTo(BigDecimal.ZERO), "Liability balance should be zero or removed after incurrence reversal.");
        // Alternatively, if you remove the liability completely on reversal:
        // assertEquals(0, portfolio.getLiabilities().size(), "Should have 0 liabilities after incurrence reversal.");

        assertTrue(portfolio.getTransactions().stream()
            .anyMatch(t -> t.getTransactionType() == TransactionType.REVERSAL && t.getTransactionId() != null && t.getParentTransactionId().equals(originalIncurrenceTxId)),
            "A reversal transaction should exist that correctly references the original transaction."
        );    
    }

    @Test
    void testReversalLiabilityPayment() {
                // Initial cash: 12000.00 CAD
        Money loanAmount = new Money("3000.00", cad);
        Money totalFeesInPortfolioCurrency = new Money("0.00", cad);
        Money totalFeesInLiabilityCurrency = new Money("0.00", cad); 

        portfolio.recordNewLiability(
                new LiabilityIncurrenceTransactionDetails(UUID.randomUUID(), "Test Loan Incurrence", "", loanAmount, loanAmount,
                        new Percentage(BigDecimal.ZERO), testDate.plusSeconds(10000), totalFeesInLiabilityCurrency, totalFeesInPortfolioCurrency), // Placeholder for ID
                createCommonInput(TransactionType.LIABILITY_INCURRENCE, "Initial Loan", new ArrayList<>()), // Loan incurrence is a DEPOSIT
                testDate
        );
        
        assertEquals(1, portfolio.getLiabilities().size());
        UUID liabilityId = portfolio.getLiabilities().get(0).getLiabilityId();
        Money expectedCashAfterIncurrence = initialPortfolioCashBalance.add(loanAmount); // Assuming no fees on incurrence
        assertEquals(expectedCashAfterIncurrence, portfolio.getPortfolioCashBalance(), "Cash after loan incurrence should be correct.");
        assertEquals(loanAmount, portfolio.getLiabilities().get(0).getCurrentBalance(), "Liability balance after incurrence should be loan amount.");
        assertEquals(1, portfolio.getTransactions().size(), "Should have 1 transaction after incurrence.");
        
        // Store these values to assert against after reversal
        Money cashBeforePayment = portfolio.getPortfolioCashBalance();
        Money liabilityBalanceBeforePayment = portfolio.getLiabilities().get(0).getCurrentBalance();


        // --- Step 2: Make a Payment on the Liability ---
        Money totalPaymentInLiabilityCurrency = new Money("100.00", cad); // Total payment amount
        Money interestAmountInLiabilityCurrency = new Money("0.27", cad); // Interest portion
        Money feesAmountInLiabilityCurrency = new Money("0.00", cad); // Fees portion
        
        // Principal portion: 100 - 0.27 - 0 = 99.73 CAD
        Money principalPaidInLiabilityCurrency = totalPaymentInLiabilityCurrency
                                                    .subtract(interestAmountInLiabilityCurrency)
                                                    .subtract(feesAmountInLiabilityCurrency);

        // Assuming direct mapping for simplicity from liability currency to portfolio currency for now
        // In a real system, these would be converted if currencies differ.
        Money totalPaymentAmountInPortfolioCurrency = totalPaymentInLiabilityCurrency; 
        Money interestAmountInPortfolioCurrency = interestAmountInLiabilityCurrency;
        Money feesAmountInPortfolioCurrency = feesAmountInLiabilityCurrency;

        LiabilityPaymentTransactionDetails paymentDetails = new LiabilityPaymentTransactionDetails(
            liabilityId,
            totalPaymentInLiabilityCurrency,
            interestAmountInLiabilityCurrency,
            feesAmountInLiabilityCurrency,
            totalPaymentAmountInPortfolioCurrency, // This is the total cash outflow
            interestAmountInPortfolioCurrency,
            feesAmountInPortfolioCurrency
        );

        // For a payment, the transaction type should be PAYMENT, not LIABILITY_INCURRENCE or DEPOSIT
        // And the common input's fees list might also apply if there are general payment processing fees
        List<Fee> paymentFees = new ArrayList<>(); // No additional common fees for this payment
        portfolio.recordLiabilityPayment(paymentDetails, 
                                         createCommonInput(TransactionType.PAYMENT, "Loan Payment", paymentFees), // Changed TransactionType to PAYMENT
                                         testDate.plusSeconds(10));

        // Assert state after payment
        Money expectedCashAfterPayment = cashBeforePayment.subtract(totalPaymentAmountInPortfolioCurrency); // Cash decreases by total payment
        Money expectedLiabilityBalanceAfterPayment = liabilityBalanceBeforePayment.subtract(principalPaidInLiabilityCurrency); // Liability decreases by principal

        assertEquals(expectedCashAfterPayment, portfolio.getPortfolioCashBalance(), "Cash balance after payment should be correct.");
        assertEquals(expectedLiabilityBalanceAfterPayment, portfolio.getLiabilities().get(0).getCurrentBalance(), "Liability balance after payment should be correct (reduced by principal).");
        assertEquals(2, portfolio.getTransactions().size(), "Should have 2 transactions (incurrence + payment).");
        
        UUID originalPaymentTxId = portfolio.getTransactions().stream()
                                    .filter(t -> t.getTransactionType() == TransactionType.PAYMENT)
                                    .findFirst()
                                    .orElseThrow(() -> new AssertionError("Payment transaction not found."))
                                    .getTransactionId();


        // --- Step 3: Reverse the Payment ---
        portfolio.reverseTransaction(originalPaymentTxId, "Test payment reversal", testDate.plusSeconds(20));

        // --- Step 4: Assert State After Reversal ---
        // Cash should revert to 'cashBeforePayment' (after incurrence, before payment)
        assertEquals(cashBeforePayment, portfolio.getPortfolioCashBalance(), "Cash balance should revert to before payment after reversal.");
        
        // Liability balance should revert to 'liabilityBalanceBeforePayment' (after incurrence, before payment)
        assertEquals(liabilityBalanceBeforePayment, portfolio.getLiabilities().get(0).getCurrentBalance(), "Liability balance should revert to before payment after reversal.");
        
        assertEquals(3, portfolio.getTransactions().size(), "Should have 3 transactions (incurrence + payment + reversal).");
        
        // Assert that a reversal transaction exists and correctly references the original payment
        assertTrue(portfolio.getTransactions().stream()
            .anyMatch(t -> t.getTransactionType() == TransactionType.REVERSAL && 
                           t.getParentTransactionId() != null && 
                           t.getParentTransactionId().equals(originalPaymentTxId)),
            "A reversal transaction for the payment should exist and reference the original payment."
        );

        
        
    }


    // --- Helper Method ---
    private CommonTransactionInput createCommonInput(TransactionType type, String description, List<Fee> fees) {
        return new CommonTransactionInput(
                UUID.randomUUID(),
                null, // parentId
                type,
                new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.SYSTEM, description, testDate, testDate),
                fees
        );
    }
}
