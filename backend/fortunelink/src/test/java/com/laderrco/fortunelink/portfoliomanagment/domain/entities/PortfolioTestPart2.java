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
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class PortfolioTestPart2 {

    private Portfolio portfolio;
	private UUID userId;
	private UUID portfolioId;
	private String name;
	private String desc;
	private Money portfolioCashBalance;
	private Currency cad;
	private Currency usd;
	private ExchangeRateService exchangeRateService;
    
    private String liabilityName;
    private String description;
    private Money originalLoanAmount;
    private Money originalLoanAmountInPortfolioCur;
    private Percentage annualInterestRate;
    private Instant maturityDate;
    private Money totalFeesInLiaCur;
    private Money totalFeesInPorfolioCur;

    private LiabilityIncurrenceTransactionDetails defaultIncurence;
    private CommonTransactionInput commonTransactionInput;

	@BeforeEach
	void init() {
		userId = UUID.randomUUID();
		portfolioId = UUID.randomUUID();
		name = "Portfolio name ";
		desc = "some desc";

		cad = Currency.getInstance("CAD");
		usd = Currency.getInstance("USD");

		portfolioCashBalance = new Money(12000, cad);
		exchangeRateService = new SimpleExchangeRateService();

		portfolio = new Portfolio(
				portfolioId, userId, name, desc, portfolioCashBalance, cad, exchangeRateService);
   
        liabilityName = "CAR LOAN";
        description = "some car loan";
        
        originalLoanAmount = new Money("1400.54", cad);
        originalLoanAmountInPortfolioCur = new Money("1400.54", cad);
        annualInterestRate = Percentage.fromPercentage(BigDecimal.valueOf(7.32));
        maturityDate = Instant.now();
        totalFeesInLiaCur = Money.ZERO(cad);
        totalFeesInPorfolioCur = Money.ZERO(cad);
        defaultIncurence = new LiabilityIncurrenceTransactionDetails(liabilityName, description, originalLoanAmount, originalLoanAmountInPortfolioCur, annualInterestRate, maturityDate, totalFeesInLiaCur, totalFeesInPorfolioCur);

        commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), 
            null, 
            TransactionType.LIABILITY_INCURRENCE, 
            new TransactionMetadata(
                TransactionStatus.COMPLETED, 
                TransactionSource.MANUAL_INPUT, 
                "SOME DESC", 
                maturityDate, 
                maturityDate
            ), 
            null
        );

       
	}

    @Test
    void testRecordNewLiabilityPayment() {
        portfolio.recordNewLiability(defaultIncurence, commonTransactionInput, maturityDate);
        assertTrue(portfolio.getLiabilities().size() == 1);

        UUID liabilityId = portfolio.getLiabilities().get(0).getLiabilityId();
        Money initialLiabilityBalance = portfolio.getLiabilities().get(0).getCurrentBalance();

        Money principalToPay = new Money("95.00", cad); // Let's say you want to reduce principal by 95 CAD
        Money interestAccrued = new Money("5.00", cad);   // And 5 CAD of interest needs to be paid
        Money paymentFees = Money.ZERO(cad);              // No fees for the payment itself in this example

        // --- Calculate the total payment amounts (what goes INTO the LiabilityPaymentTransactionDetails) ---
        // Total amount applied to the liability (in its currency)
        Money totalPaymentAmountInLiability = principalToPay.add(interestAccrued).add(paymentFees); // This will be 95 + 5 + 0 = 100 CAD

        // Total cash outflow from the portfolio (in portfolio currency)
        // For this same-currency example, it's the same as totalPaymentAmountInLiability
        Money totalPaymentAmountInPortfolioCurrency = totalPaymentAmountInLiability;

        // Fees & Interest amounts are straightforward
        Money interestAmountInPortfolioCurrency = interestAccrued;
        Money feesAmountInPortfolioCurrency = paymentFees;

        LiabilityPaymentTransactionDetails paymentTransactionDetails = new LiabilityPaymentTransactionDetails(
            liabilityId,
            totalPaymentAmountInLiability,       // Total payment amount on the liability (100 CAD)
            interestAccrued,                     // Interest portion (5 CAD)
            paymentFees,                         // Fees portion (0 CAD)
            totalPaymentAmountInPortfolioCurrency, // Total cash outflow from portfolio (100 CAD)
            interestAmountInPortfolioCurrency,
            feesAmountInPortfolioCurrency
        );

        commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(),
            null,
            TransactionType.PAYMENT, // Make sure this is TransactionType.PAYMENT
            new TransactionMetadata(
                TransactionStatus.COMPLETED,
                TransactionSource.MANUAL_INPUT,
                "Loan Payment",
                Instant.now(),
                Instant.now()
            ),
            null
        );

        // Get cash balance BEFORE payment to calculate expected cash AFTER payment
        Money cashBeforePayment = portfolio.getPortfolioCashBalance();

        portfolio.recordLiabilityPayment(paymentTransactionDetails, commonTransactionInput, maturityDate);

        assertTrue(portfolio.getTransactions().size() == 2); // 1 (incurrence) + 1 (payment)

        // --- Assertions ---

        // Expected Liability Balance: Initial Liability Balance - Principal Paid
        // Principal Paid = totalPaymentAmountInLiability - interestAccrued - paymentFees = 100 - 5 - 0 = 95 CAD
        Money expectedLiabilityBalance = initialLiabilityBalance.subtract(principalToPay);
        assertEquals(expectedLiabilityBalance, portfolio.getLiabilities().get(0).getCurrentBalance(),
            "Liability balance should be reduced by the principal portion of the payment.");

        // Expected Cash Balance: Cash Before Payment - Total Cash Outflow
        Money expectedCashBalance = cashBeforePayment.subtract(totalPaymentAmountInPortfolioCurrency);
        assertEquals(expectedCashBalance, portfolio.getPortfolioCashBalance(),
            "Portfolio cash balance should decrease by the total payment amount.");

    }

// Your existing happy path test (slightly adjusted for consistency)
    @Test
    void testRecordNewLiabilityPayment_HappyPath_SameCurrency_NoFeesNoInterest() {
        // Incur a liability
        portfolio.recordNewLiability(defaultIncurence, commonTransactionInput, maturityDate);
        assertTrue(portfolio.getLiabilities().size() == 1);
        UUID liabilityId = portfolio.getLiabilities().get(0).getLiabilityId();
        Money initialLiabilityBalance = portfolio.getLiabilities().get(0).getCurrentBalance();

        // Payment components
        Money principalToPay = new Money("95.00", cad);
        Money interestAccrued = new Money("5.00", cad);
        Money paymentFees = Money.ZERO(cad); // Fees included in total payment for the liability itself

        // Calculate total payment amounts
        Money totalPaymentAmountInLiability = principalToPay.add(interestAccrued).add(paymentFees);
        Money totalPaymentAmountInPortfolioCurrency = totalPaymentAmountInLiability; // Same currency

        LiabilityPaymentTransactionDetails paymentTransactionDetails = new LiabilityPaymentTransactionDetails(
            liabilityId,
            totalPaymentAmountInLiability, interestAccrued, paymentFees,
            totalPaymentAmountInPortfolioCurrency, interestAccrued, paymentFees
        );

        // Common transaction input for payment
        CommonTransactionInput paymentCommonInput = new CommonTransactionInput(
            UUID.randomUUID(), null, TransactionType.PAYMENT,
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, "Loan Payment", Instant.now(), Instant.now()),
            new ArrayList<>() // No separate transaction fees for this test
        );

        Money cashBeforePayment = portfolio.getPortfolioCashBalance();

        portfolio.recordLiabilityPayment(paymentTransactionDetails, paymentCommonInput, Instant.now());

        assertEquals(2, portfolio.getTransactions().size()); // 1 (incurrence) + 1 (payment)

        Money expectedLiabilityBalance = initialLiabilityBalance.subtract(principalToPay);
        assertEquals(expectedLiabilityBalance, portfolio.getLiabilities().get(0).getCurrentBalance(),
            "Liability balance should be reduced by the principal portion.");

        Money expectedCashBalance = cashBeforePayment.subtract(totalPaymentAmountInPortfolioCurrency);
        assertEquals(expectedCashBalance, portfolio.getPortfolioCashBalance(),
            "Portfolio cash balance should decrease by total payment amount.");
    }

    // --- New Test Cases ---

    @Test
    void testRecordLiabilityPayment_WithTransactionFees() {
        // Incur a liability
        portfolio.recordNewLiability(defaultIncurence, commonTransactionInput, maturityDate);
        UUID liabilityId = portfolio.getLiabilities().get(0).getLiabilityId();
        Money initialLiabilityBalance = portfolio.getLiabilities().get(0).getCurrentBalance();

        // Payment components
        Money principalToPay = new Money("100.00", cad);
        Money interestAccrued = new Money("10.00", cad);
        Money liabilitySpecificFees = new Money("0.00", cad); // Fees that go towards the liability balance if any

        // Calculate total payment to the liability
        Money totalPaymentToLiability = principalToPay.add(interestAccrued).add(liabilitySpecificFees); // 110 CAD

        // Separate transaction fee (e.g., bank processing fee)
        Money separateTransactionFee = new Money("2.50", cad);

        // Total cash outflow from portfolio
        // This is total payment to liability + separate transaction fees
        Money totalCashOutflow = totalPaymentToLiability.add(separateTransactionFee); // 110 + 2.50 = 112.50 CAD

        LiabilityPaymentTransactionDetails paymentTransactionDetails = new LiabilityPaymentTransactionDetails(
            liabilityId,
            totalPaymentToLiability, interestAccrued, liabilitySpecificFees, // For liability update
            totalPaymentToLiability, interestAccrued, liabilitySpecificFees  // For portfolio cash update (without separate transaction fees here)
        );

        // Common transaction input for payment, INCLUDING separate fees
        CommonTransactionInput paymentCommonInput = new CommonTransactionInput(
            UUID.randomUUID(), null, TransactionType.PAYMENT,
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, "Payment with transaction fees", Instant.now(), Instant.now()),
            List.of(new Fee(FeeType.BROKERAGE, separateTransactionFee)) // Pass the separate fee here
        );

        Money cashBeforePayment = portfolio.getPortfolioCashBalance();

        // Perform the payment
        portfolio.recordLiabilityPayment(paymentTransactionDetails, paymentCommonInput, Instant.now());

        // Assertions
        assertEquals(2, portfolio.getTransactions().size(), "Should have two transactions (incurrence + payment).");

        // Liability balance should only reduce by principal
        Money expectedLiabilityBalance = initialLiabilityBalance.subtract(principalToPay);
        assertEquals(expectedLiabilityBalance, portfolio.getLiabilities().get(0).getCurrentBalance(),
            "Liability balance should be reduced by the principal portion of the payment.");

        // Cash balance should reduce by total cash outflow (payment to liability + separate transaction fees)
        Money expectedCashBalance = cashBeforePayment.subtract(totalCashOutflow);
        assertEquals(expectedCashBalance, portfolio.getPortfolioCashBalance(),
            "Portfolio cash balance should decrease by total payment to liability plus separate transaction fees.");
    }

    @Test
    void testRecordLiabilityPayment_OnlyInterestAndFees_NoPrincipalReduction() {
        // Incur a liability (e.g., 1400.54 CAD)
        portfolio.recordNewLiability(defaultIncurence, commonTransactionInput, maturityDate);
        UUID liabilityId = portfolio.getLiabilities().get(0).getLiabilityId();
        Money initialLiabilityBalance = portfolio.getLiabilities().get(0).getCurrentBalance();

        // Payment covers only interest and a small liability-specific fee, no principal
        Money principalToPay = Money.ZERO(cad);
        Money interestAccrued = new Money("15.00", cad);
        Money liabilitySpecificFees = new Money("2.00", cad); // e.g., a late fee charged by the lender

        // Total amount applied to the liability
        Money totalPaymentToLiability = principalToPay.add(interestAccrued).add(liabilitySpecificFees); // 17.00 CAD

        LiabilityPaymentTransactionDetails paymentTransactionDetails = new LiabilityPaymentTransactionDetails(
            liabilityId,
            totalPaymentToLiability, interestAccrued, liabilitySpecificFees,
            totalPaymentToLiability, interestAccrued, liabilitySpecificFees
        );

        CommonTransactionInput paymentCommonInput = new CommonTransactionInput(
            UUID.randomUUID(), null, TransactionType.PAYMENT,
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, "Interest & Fees Payment", Instant.now(), Instant.now()),
            new ArrayList<>()
        );

        Money cashBeforePayment = portfolio.getPortfolioCashBalance();

        Exception e1 = assertThrows(IllegalArgumentException.class, () -> portfolio.recordLiabilityPayment(paymentTransactionDetails, paymentCommonInput, Instant.now()));
        assertEquals("Payment amount must be a positive number.", e1.getMessage());

        // Assertions
        assertEquals(1, portfolio.getTransactions().size());

        // Liability balance should NOT change as principal reduction is zero
        assertEquals(initialLiabilityBalance, portfolio.getLiabilities().get(0).getCurrentBalance(),
            "Liability principal balance should not change if only interest and fees are paid.");

        // Cash balance should decrease by the total payment amount
        Money expectedCashBalance = cashBeforePayment.subtract(totalPaymentToLiability);
        assertEquals(expectedCashBalance, portfolio.getPortfolioCashBalance(),
            "Portfolio cash balance should decrease by total payment amount.");
    }

    @Test
    void testRecordLiabilityPayment_FullPayoffScenario() {
        // Incur a small liability for easy full payoff
        Money smallLoanAmount = new Money("50.00", cad);
        portfolio.recordNewLiability(
            new LiabilityIncurrenceTransactionDetails("Small Loan", "", smallLoanAmount, smallLoanAmount, new Percentage(BigDecimal.ZERO), maturityDate, Money.ZERO(cad), Money.ZERO(cad)),
            new CommonTransactionInput(UUID.randomUUID(), null, TransactionType.DEPOSIT, new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, "", Instant.now(), Instant.now()), new ArrayList<>()),
            Instant.now()
        );
        UUID liabilityId = portfolio.getLiabilities().get(0).getLiabilityId();
        Money initialLiabilityBalance = portfolio.getLiabilities().get(0).getCurrentBalance(); // Should be 50.00 CAD

        // Payment to cover the exact remaining principal
        Money principalToPay = initialLiabilityBalance;
        Money interestAccrued = Money.ZERO(cad);
        Money liabilitySpecificFees = Money.ZERO(cad);

        Money totalPaymentToLiability = principalToPay.add(interestAccrued).add(liabilitySpecificFees); // 50.00 CAD

        LiabilityPaymentTransactionDetails paymentTransactionDetails = new LiabilityPaymentTransactionDetails(
            liabilityId,
            totalPaymentToLiability, interestAccrued, liabilitySpecificFees,
            totalPaymentToLiability, interestAccrued, liabilitySpecificFees
        );

        CommonTransactionInput paymentCommonInput = new CommonTransactionInput(
            UUID.randomUUID(), null, TransactionType.PAYMENT,
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, "Full Payoff", Instant.now(), Instant.now()),
            new ArrayList<>()
        );

        Money cashBeforePayment = portfolio.getPortfolioCashBalance();

        portfolio.recordLiabilityPayment(paymentTransactionDetails, paymentCommonInput, Instant.now());

        // Assertions
        assertEquals(2, portfolio.getTransactions().size());

        // Liability balance should be zero after full payoff
        assertEquals(Money.ZERO(cad), portfolio.getLiabilities().get(0).getCurrentBalance(),
            "Liability balance should be zero after full payoff.");

        // Cash balance should decrease by the total payment amount
        Money expectedCashBalance = cashBeforePayment.subtract(totalPaymentToLiability);
        assertEquals(expectedCashBalance, portfolio.getPortfolioCashBalance(),
            "Portfolio cash balance should decrease by total payment amount for full payoff.");
    }

    @Test
    void testRecordLiabilityPayment_ZeroPaymentAmount() {
        // Incur a liability
        portfolio.recordNewLiability(defaultIncurence, commonTransactionInput, maturityDate);
        UUID liabilityId = portfolio.getLiabilities().get(0).getLiabilityId();
        Money initialLiabilityBalance = portfolio.getLiabilities().get(0).getCurrentBalance();
        Money cashBeforePayment = portfolio.getPortfolioCashBalance();
        int initialTransactionCount = portfolio.getTransactions().size();

        // Payment with all zero amounts
        Money zeroAmount = Money.ZERO(cad);

        LiabilityPaymentTransactionDetails paymentTransactionDetails = new LiabilityPaymentTransactionDetails(
            liabilityId,
            zeroAmount, zeroAmount, zeroAmount,
            zeroAmount, zeroAmount, zeroAmount
        );

        CommonTransactionInput paymentCommonInput = new CommonTransactionInput(
            UUID.randomUUID(), null, TransactionType.PAYMENT,
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, "Zero Payment", Instant.now(), Instant.now()),
            new ArrayList<>()
        );

        // Depending on your business logic, this might throw an error or simply do nothing.
        // For this test, assuming it might go through but has no effect.
        Exception e1 = assertThrows(IllegalArgumentException.class, () ->portfolio.recordLiabilityPayment(paymentTransactionDetails, paymentCommonInput, Instant.now()));
        assertEquals("Payment amount must be a positive number.", e1.getMessage());


        // Assertions
        // No change expected
        assertEquals(initialLiabilityBalance, portfolio.getLiabilities().get(0).getCurrentBalance(),
            "Liability balance should not change for a zero payment.");
        assertEquals(cashBeforePayment, portfolio.getPortfolioCashBalance(),
            "Cash balance should not change for a zero payment.");
        assertEquals(initialTransactionCount, portfolio.getTransactions().size(), // Still records a transaction
            "A transaction for zero payment should still be recorded if allowed.");
    }


    // Test for negative payment amounts (should throw IllegalArgumentException)
    @Test
    void testRecordLiabilityPayment_NegativePaymentAmounts() {
        // Incur a liability
        portfolio.recordNewLiability(defaultIncurence, commonTransactionInput, maturityDate);
        UUID liabilityId = portfolio.getLiabilities().get(0).getLiabilityId();

        // Attempt to pay with negative amounts
        Money negativePrincipal = new Money("-50.00", cad);
        Money positiveInterest = new Money("5.00", cad);

        // A negative total payment is generally invalid
        Money totalPayment = negativePrincipal.add(positiveInterest); // Will be negative if principal is larger

        LiabilityPaymentTransactionDetails paymentTransactionDetails = new LiabilityPaymentTransactionDetails(
            liabilityId,
            totalPayment, positiveInterest, Money.ZERO(cad), // Liability currency details
            totalPayment, positiveInterest, Money.ZERO(cad)  // Portfolio currency details
        );

        CommonTransactionInput paymentCommonInput = new CommonTransactionInput(
            UUID.randomUUID(), null, TransactionType.PAYMENT,
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, "Negative Payment", Instant.now(), Instant.now()),
            new ArrayList<>()
        );

        // Expect an exception when trying to subtract a negative amount or if Money.isNegative() checks are in place
        // The Money class's subtract method will throw if it makes an amount negative if you have that check.
        // Or if you directly try to use a negative Money amount where positive is expected.
        assertThrows(IllegalArgumentException.class, () -> {
            portfolio.recordLiabilityPayment(paymentTransactionDetails, paymentCommonInput, Instant.now());
        }, "Should throw IllegalArgumentException for negative payment amounts.");

        // Optionally, assert that no state changes occurred (cash, liability balance, transaction count)
    }
}
