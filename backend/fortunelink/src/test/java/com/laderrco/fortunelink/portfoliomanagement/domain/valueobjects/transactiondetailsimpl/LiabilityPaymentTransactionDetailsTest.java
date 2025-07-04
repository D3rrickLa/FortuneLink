package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsimpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class LiabilityPaymentTransactionDetailsTest {
// Common test data
    private UUID liabilityId;
    private PortfolioCurrency usdCurrency;
    private PortfolioCurrency cadCurrency;

    @BeforeEach
    void init() {
        liabilityId = UUID.randomUUID();
        usdCurrency = new PortfolioCurrency(Currency.getInstance("USD"));
        cadCurrency = new PortfolioCurrency(Currency.getInstance("CAD"));
    }

    // --- Helper Method for creating Money objects ---
    private Money createMoney(String amount, PortfolioCurrency currency) {
        return new Money(new BigDecimal(amount), currency);
    }

    // --- Test Constructor - Happy Paths (remains largely the same, but values now explicitly follow the rule) ---

    @Test
    void testConstructor_ValidPayment_NoFxAndNoFees() {
        // Arrange: All liability-related amounts in CAD, cash outflow in CAD
        Money totalOriginalPayment = createMoney("100.00", cadCurrency);
        Money principalPaid = createMoney("80.00", cadCurrency);
        Money interestPaid = createMoney("20.00", cadCurrency);
        Money feesPaid = createMoney("0.00", cadCurrency);
        Money cashOutflow = createMoney("100.00", cadCurrency);

        // Act
        LiabilityPaymentTransactionDetails details = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow
        );

        // Assert
        assertNotNull(details);
        assertEquals(liabilityId, details.getLiabilityId());
        assertEquals(totalOriginalPayment, details.getTotalOriginalPaymentAmount());
        assertEquals(principalPaid, details.getPrincipalPaidAmount());
        assertEquals(interestPaid, details.getInterestPaidAmount());
        assertEquals(feesPaid, details.getFeesPaidAmount());
        assertEquals(cashOutflow, details.getCashOutflowInPortfolioCurrency());
    }

    @Test
    void testConstructor_ValidPayment_WithFxAndFees() {
        // Arrange: Liability-related amounts in USD, cash outflow in CAD
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("15.00", usdCurrency);
        Money feesPaid = createMoney("5.00", usdCurrency); // Fees are also in USD, matching liability currency
        Money cashOutflow = createMoney("145.00", cadCurrency); // Total cash out in portfolio currency

        // Act
        LiabilityPaymentTransactionDetails details = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow
        );

        // Assert
        assertNotNull(details);
        assertEquals(liabilityId, details.getLiabilityId());
        assertEquals(totalOriginalPayment, details.getTotalOriginalPaymentAmount());
        assertEquals(principalPaid, details.getPrincipalPaidAmount());
        assertEquals(interestPaid, details.getInterestPaidAmount());
        assertEquals(feesPaid, details.getFeesPaidAmount());
        assertEquals(cashOutflow, details.getCashOutflowInPortfolioCurrency());
    }
    
    // --- Test Constructor - Unhappy Paths (Validation) ---

    // Null checks and amount magnitude checks remain the same as before, they are already comprehensive.
    // I'll re-include the relevant currency mismatch tests, adjusting the messages and scenarios.
   @Test
    void testConstructor_ThrowsExceptionForNullLiabilityId() {
        // Arrange common valid data for other fields
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("20.00", usdCurrency);
        Money feesPaid = createMoney("0.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                null, totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow
            );
        });
        assertTrue(thrown.getMessage().contains("Liability ID cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullTotalOriginalPaymentAmount() {
        // Arrange common valid data for other fields
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("20.00", usdCurrency);
        Money feesPaid = createMoney("0.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, null, principalPaid, interestPaid, feesPaid, cashOutflow
            );
        });
        assertTrue(thrown.getMessage().contains("Total original payment amount cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullPrincipalPaidAmount() {
        // Arrange common valid data for other fields
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money interestPaid = createMoney("20.00", usdCurrency);
        Money feesPaid = createMoney("0.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, totalOriginalPayment, null, interestPaid, feesPaid, cashOutflow
            );
        });
        assertTrue(thrown.getMessage().contains("Principal paid amount cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullInterestPaidAmount() {
        // Arrange common valid data for other fields
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money feesPaid = createMoney("0.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, totalOriginalPayment, principalPaid, null, feesPaid, cashOutflow
            );
        });
        assertTrue(thrown.getMessage().contains("Interest paid amount cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullFeesPaidAmount() {
        // Arrange common valid data for other fields
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("20.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, totalOriginalPayment, principalPaid, interestPaid, null, cashOutflow
            );
        });
        assertTrue(thrown.getMessage().contains("Fees paid amount cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNullCashOutflowInPortfolioCurrency() {
        // Arrange common valid data for other fields
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("20.00", usdCurrency);
        Money feesPaid = createMoney("0.00", usdCurrency);

        // Act & Assert
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid, null
            );
        });
        assertTrue(thrown.getMessage().contains("Cash outflow in portfolio currency cannot be null."));
    }

    @Test
    void testConstructor_ThrowsExceptionForZeroTotalOriginalPaymentAmount() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, createMoney("0.00", usdCurrency), createMoney("0.00", usdCurrency), 
                createMoney("0.00", usdCurrency), createMoney("0.00", usdCurrency), createMoney("100.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Total original payment amount must be positive."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNegativePrincipalPaidAmount() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, createMoney("100.00", usdCurrency), createMoney("-10.00", usdCurrency), 
                createMoney("10.00", usdCurrency), createMoney("0.00", usdCurrency), createMoney("100.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Principal paid amount cannot be negative."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNegativeInterestPaidAmount() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, createMoney("100.00", usdCurrency), createMoney("90.00", usdCurrency), 
                createMoney("-10.00", usdCurrency), createMoney("0.00", usdCurrency), createMoney("100.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Interest paid amount cannot be negative."));
    }

    @Test
    void testConstructor_ThrowsExceptionForNegativeFeesPaidAmount() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, createMoney("100.00", usdCurrency), createMoney("90.00", usdCurrency), 
                createMoney("10.00", usdCurrency), createMoney("-5.00", usdCurrency), createMoney("100.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Fees paid amount cannot be negative."));
    }

    @Test
    void testConstructor_ThrowsExceptionForZeroCashOutflowInPortfolioCurrency() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, createMoney("100.00", usdCurrency), createMoney("80.00", usdCurrency), 
                createMoney("20.00", usdCurrency), createMoney("0.00", usdCurrency), createMoney("0.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Cash outflow in portfolio currency must be positive."));
    }

    @Test
    void testConstructor_ThrowsExceptionForCurrencyMismatch_PrincipalAndTotalOriginalPayment() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, createMoney("100.00", usdCurrency), createMoney("80.00", cadCurrency), // Principal in wrong currency
                createMoney("20.00", usdCurrency), createMoney("0.00", usdCurrency), createMoney("145.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Principal, interest, and fees breakdown amounts must all be in the same currency as the total original payment amount (liability currency)."));
    }
    
    @Test
    void testConstructor_ThrowsExceptionForCurrencyMismatch_InterestAndTotalOriginalPayment() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, createMoney("100.00", usdCurrency), createMoney("80.00", usdCurrency), 
                createMoney("20.00", cadCurrency), // Interest in wrong currency
                createMoney("0.00", usdCurrency), createMoney("145.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Principal, interest, and fees breakdown amounts must all be in the same currency as the total original payment amount (liability currency)."));
    }

    @Test
    void testConstructor_ThrowsExceptionForCurrencyMismatch_FeesAndTotalOriginalPayment() {
        // Act & Assert
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            new LiabilityPaymentTransactionDetails(
                liabilityId, createMoney("100.00", usdCurrency), createMoney("80.00", usdCurrency), 
                createMoney("20.00", usdCurrency), 
                createMoney("5.00", cadCurrency), // Fees in wrong currency (not matching totalOriginalPayment)
                createMoney("145.00", cadCurrency)
            );
        });
        assertTrue(thrown.getMessage().contains("Principal, interest, and fees breakdown amounts must all be in the same currency as the total original payment amount (liability currency)."));
    }

    // The other null and magnitude checks (zero/negative amounts) are unchanged and remain valid.
    // For brevity, I'm not repeating them here, but they should be in your full test file.
    // E.g., testConstructor_ThrowsExceptionForNullLiabilityId, testConstructor_ThrowsExceptionForZeroTotalOriginalPaymentAmount etc.

    // --- Test Equals and HashCode (remains the same, as all fields are covered) ---
    @Test
    void testEqualsIfBranches() {
        // Arrange
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("15.00", usdCurrency);
        Money feesPaid = createMoney("5.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);
    
        LiabilityPaymentTransactionDetails details1 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow
        );
        LiabilityPaymentTransactionDetails details2 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow
        );
    
        // Assert
        assertTrue(details1.equals(details1));
        assertTrue(details2.equals(details1));
        assertTrue(details2.equals(details1));
        assertEquals(details1.hashCode(), details2.hashCode());
        assertFalse(details1.equals(new Object()));
        assertFalse(details1.equals(null));
    }

    @Test
    void testEquals_SameObjects() {
        // Arrange
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("15.00", usdCurrency);
        Money feesPaid = createMoney("5.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        LiabilityPaymentTransactionDetails details1 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow
        );
        LiabilityPaymentTransactionDetails details2 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow
        );

        // Assert
        assertTrue(details1.equals(details2));
        assertTrue(details2.equals(details1));
        assertEquals(details1.hashCode(), details2.hashCode());
    }
 @Test
    void testEquals_DifferentLiabilityId() {
        // Arrange
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("15.00", usdCurrency);
        Money feesPaid = createMoney("5.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        LiabilityPaymentTransactionDetails details1 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow
        );
        LiabilityPaymentTransactionDetails details2 = new LiabilityPaymentTransactionDetails(
            UUID.randomUUID(), totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentTotalOriginalPaymentAmount() {
        // Arrange
        Money totalOriginalPayment1 = createMoney("100.00", usdCurrency);
        Money totalOriginalPayment2 = createMoney("105.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("15.00", usdCurrency);
        Money feesPaid = createMoney("5.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        LiabilityPaymentTransactionDetails details1 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment1, principalPaid, interestPaid, feesPaid, cashOutflow
        );
        LiabilityPaymentTransactionDetails details2 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment2, principalPaid, interestPaid, feesPaid, cashOutflow
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentPrincipalPaidAmount() {
        // Arrange
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid1 = createMoney("80.00", usdCurrency);
        Money principalPaid2 = createMoney("85.00", usdCurrency);
        Money interestPaid = createMoney("15.00", usdCurrency);
        Money feesPaid = createMoney("5.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        LiabilityPaymentTransactionDetails details1 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid1, interestPaid, feesPaid, cashOutflow
        );
        LiabilityPaymentTransactionDetails details2 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid2, interestPaid, feesPaid, cashOutflow
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentInterestPaidAmount() {
        // Arrange
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid1 = createMoney("15.00", usdCurrency);
        Money interestPaid2 = createMoney("20.00", usdCurrency);
        Money feesPaid = createMoney("5.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        LiabilityPaymentTransactionDetails details1 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid1, feesPaid, cashOutflow
        );
        LiabilityPaymentTransactionDetails details2 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid2, feesPaid, cashOutflow
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentFeesPaidAmount() {
        // Arrange
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("15.00", usdCurrency);
        Money feesPaid1 = createMoney("5.00", usdCurrency);
        Money feesPaid2 = createMoney("7.00", usdCurrency);
        Money cashOutflow = createMoney("145.00", cadCurrency);

        LiabilityPaymentTransactionDetails details1 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid1, cashOutflow
        );
        LiabilityPaymentTransactionDetails details2 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid2, cashOutflow
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }

    @Test
    void testEquals_DifferentCashOutflowInPortfolioCurrency() {
        // Arrange
        Money totalOriginalPayment = createMoney("100.00", usdCurrency);
        Money principalPaid = createMoney("80.00", usdCurrency);
        Money interestPaid = createMoney("15.00", usdCurrency);
        Money feesPaid = createMoney("5.00", usdCurrency);
        Money cashOutflow1 = createMoney("145.00", cadCurrency);
        Money cashOutflow2 = createMoney("150.00", cadCurrency);

        LiabilityPaymentTransactionDetails details1 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow1
        );
        LiabilityPaymentTransactionDetails details2 = new LiabilityPaymentTransactionDetails(
            liabilityId, totalOriginalPayment, principalPaid, interestPaid, feesPaid, cashOutflow2
        );

        // Assert
        assertFalse(details1.equals(details2));
        assertNotEquals(details1.hashCode(), details2.hashCode());
    }
}