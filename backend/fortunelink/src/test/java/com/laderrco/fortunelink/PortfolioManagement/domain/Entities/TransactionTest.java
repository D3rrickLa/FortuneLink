package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.PortfolioCurrency;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionSource;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionStatus;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionType;

public class TransactionTest {
    // Helper method to create a valid base set of parameters for an ACTIVE,
    // MANUAL_INPUT transaction
    private UUID defaultTransactionId = UUID.randomUUID();
    private UUID defaultPortfolioId = UUID.randomUUID();
    private TransactionType defaultTransactionType = TransactionType.BUY;
    private Money defaultAmount = new Money(new BigDecimal("100.00"), new PortfolioCurrency("USD", "$"));
    private Instant defaultTransactionDate = Instant.now();
    private String defaultDescription = "Test transaction";
    private BigDecimal defaultQuantity = new BigDecimal("10");
    private BigDecimal defaultPricePerUnit = new BigDecimal("10.00");
    private UUID defaultAssetHoldingId = UUID.randomUUID();
    private UUID defaultLiabilityId = null; // Default to null for common cases
    private TransactionStatus defaultStatus = TransactionStatus.ACTIVE;
    private String defaultVoidReason = null;
    private Instant defaultVoidedAt = null;
    private TransactionSource defaultSource = TransactionSource.MANUAL_INPUT;

    @Test
    void testConstructorBranches() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;

        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();
        UUID lUuid = UUID.randomUUID();

        // Testing the step 2 stuff
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc,
                        new BigDecimal(-10),
                        cpu, TransactionStatus.ACTIVE, null, null, assetUuid, lUuid, TransactionSource.MANUAL_INPUT));
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction(transicationUuid, portfolioUuid, TransactionType.SELL, money, transDate, desc,
                        quant,
                        new BigDecimal(0), TransactionStatus.ACTIVE, null, null, assetUuid, lUuid,
                        TransactionSource.MANUAL_INPUT));
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction(transicationUuid, portfolioUuid, TransactionType.SELL, money, transDate, desc,
                        quant,
                        null, TransactionStatus.ACTIVE, null, null, assetUuid, lUuid,
                        TransactionSource.MANUAL_INPUT));

        // Testing step 3
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction(transicationUuid, portfolioUuid, TransactionType.SELL, money, transDate, desc,
                        quant, new BigDecimal(0), TransactionStatus.ACTIVE, null, null, null, null,
                        TransactionSource.MANUAL_INPUT));

        // Testing step 4
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction(transicationUuid, portfolioUuid, TransactionType.SELL, money, transDate, desc,
                        quant, new BigDecimal(0), TransactionStatus.VOIDED, "some reason", null, assetUuid, lUuid,
                        TransactionSource.MANUAL_INPUT));

        assertThrows(IllegalArgumentException.class,
                () -> new Transaction(transicationUuid, portfolioUuid, TransactionType.SELL, money, transDate, desc,
                        quant, new BigDecimal(0), TransactionStatus.VOIDED, "   ", null, assetUuid, lUuid,
                        TransactionSource.MANUAL_INPUT));
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction(transicationUuid, portfolioUuid, TransactionType.SELL, money, transDate, desc,
                        quant, new BigDecimal(0), TransactionStatus.VOIDED, "something important", Instant.now(),
                        assetUuid, lUuid,
                        TransactionSource.MANUAL_INPUT));
        // for when viod reason and vioded at are not null but the transaction tpye
        // isn't set to void
        assertThrows(IllegalArgumentException.class,
                () -> new Transaction(transicationUuid, portfolioUuid, TransactionType.SELL, money, transDate, desc,
                        quant, new BigDecimal(0), TransactionStatus.ACTIVE, " something", Instant.now(), assetUuid,
                        lUuid,
                        TransactionSource.MANUAL_INPUT));
    }

    @Test
    void testConstructorIsCashOnly() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();

        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10);
        BigDecimal cpu = new BigDecimal(100);
        UUID lUuid = UUID.randomUUID();

        Transaction t1 = new Transaction(transicationUuid, portfolioUuid, TransactionType.WITHDRAWAL, money, transDate,
                desc,
                quant, cpu, TransactionStatus.ACTIVE, null, null, null, lUuid,
                TransactionSource.MANUAL_INPUT);
        Transaction t2 = new Transaction(transicationUuid, portfolioUuid, TransactionType.DIVIDEND, money, transDate,
                desc,
                quant, cpu, TransactionStatus.ACTIVE, null, null, null, lUuid,
                TransactionSource.MANUAL_INPUT);
        Transaction t3 = new Transaction(transicationUuid, portfolioUuid, TransactionType.INTEREST_INCOME, money,
                transDate,
                desc,
                quant, cpu, TransactionStatus.ACTIVE, null, null, null, lUuid,
                TransactionSource.MANUAL_INPUT);

        assertEquals(t1, t3);
        assertEquals(t1, t2);
        assertEquals(t1, t2);
    }

    // AI coded

    @Test
    void testTransaction_validActiveManualInput_success() {
        assertDoesNotThrow(() -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.ACTIVE, null, null, // Valid for ACTIVE
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
    }

    @Test
    void testTransaction_validVoidedManualInput_success() {
        // This tests the 'if (transactionStatus == TransactionStatus.VOIDED)' path for
        // success
        assertDoesNotThrow(() -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.VOIDED, "Valid void reason", Instant.now(), // Valid for VOIDED
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
    }

    // --- Tests for Quantity and PricePerUnit Validation (from previous discussion)
    // ---
    // (Include these from the previous set of tests if they are in your
    // constructor)
    @Test
    void testTransaction_buyType_nullQuantity_throwsException2() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.BUY, defaultAmount,
                defaultTransactionDate, defaultDescription, null, defaultPricePerUnit, // Null Quantity
                TransactionStatus.ACTIVE, null, null, // Default status for simplicity
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Quantity is required and must be positive for BUY/SELL transactions.", e.getMessage());
    }

    // ... (other quantity/pricePerUnit tests) ...

    // --- Tests for Asset/Liability ID Linkage Validation (from previous
    // discussion) ---
    // (Include these from the previous set of tests if they are in your
    // constructor)
    @Test
    void testTransaction_nonCashOnlyType_noAssetOrLiabilityId_throwsException2() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.BUY, defaultAmount, // Non-cash-only type
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.ACTIVE, null, null, // Default status
                null, null, defaultSource // Both assetHoldingId and liabilityId are null
        ));
        assertEquals(
                "Transactions must be linked to an Asset Holding or Liability, unless it's a cash-only type (DEPOSIT, WITHDRAWAL, DIVIDEND, INTEREST_INCOME).",
                e.getMessage());
    }

    @Test
    void testTransaction_nonTransferType_bothAssetAndLiabilityId_throwsException2() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.BUY, defaultAmount, // Non-transfer type
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.ACTIVE, null, null,
                UUID.randomUUID(), UUID.randomUUID(), defaultSource // Both asset and liability IDs
        ));
        assertEquals(
                "Transaction cannot be linked to both an Asset Holding and a Liability unless it's a specific transfer type.",
                e.getMessage());
    }

    // ... (other asset/liability tests) ...

    // --- REVISED/NEW Tests for Void Details Validation based on Status ---

    // Tests for `if (transactionStatus == TransactionStatus.VOIDED)` block:
    @Test
    void testTransaction_voidedStatus_nullReason_throwsNullPointerException() { // Changed name to reflect exact
                                                                                // exception
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.VOIDED, // Status is VOIDED
                null, // voidReason is null (triggers Objects.requireNonNull)
                Instant.now(),
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Void reason must be provided if status is VOIDED.", e.getMessage());
    }

    @Test
    void testTransaction_voidedStatus_emptyReason_throwsIllegalArgumentException() { // Changed name
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.VOIDED, // Status is VOIDED
                "   ", // voidReason is blank (triggers voidReason.trim().isEmpty())
                Instant.now(),
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Void reason cannot be empty or blank if status is VOIDED.", e.getMessage());
    }

    @Test
    void testTransaction_voidedStatus_nullVoidedAt_throwsNullPointerException() { // Changed name
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.VOIDED, // Status is VOIDED
                "Valid reason for void", // voidReason is valid
                null, // voidedAt is null (triggers Objects.requireNonNull)
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Voided timestamp must be provided if status is VOIDED.", e.getMessage());
    }

    // Tests for the `else` block: `if (voidReason != null || voidedAt != null)`

    @Test
    void testTransaction_activeStatus_withVoidReason_throwsIllegalArgumentException() {
        // Covers the `voidReason != null` part of the else block
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.ACTIVE, // Status is ACTIVE (enters else block)
                "Some reason", // voidReason is NOT null
                null, // voidedAt is null (but `voidReason != null` triggers the exception)
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Void reason and timestamp must be null if status is not VOIDED.", e.getMessage());
    }

    @Test
    void testTransaction_activeStatus_withVoidedAt_throwsIllegalArgumentException() {
        // Covers the `voidedAt != null` part of the else block
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.ACTIVE, // Status is ACTIVE (enters else block)
                null, // voidReason is null
                Instant.now(), // voidedAt is NOT null
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Void reason and timestamp must be null if status is not VOIDED.", e.getMessage());
    }

    @Test
    void testTransaction_completedStatus_withVoidReasonAndVoidedAt_throwsIllegalArgumentException() {
        // Covers a non-VOIDED status with both void details present
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.COMPLETED, // Status is COMPLETED (enters else block)
                "An unexpected reason", Instant.now(), // Both void details present
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Void reason and timestamp must be null if status is not VOIDED.", e.getMessage());
    }

    // --- Tests for Core Null Checks (from Objects.requireNonNull) ---
    // (Ensure all these are present from previous set of tests)

    @Test
    void testTransaction_nullTransactionId_throwsException2() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                null, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Transaction ID cannot be null.", e.getMessage());
    }

    @Test
    void testTransaction_validActiveManualInput_success2() {
        assertDoesNotThrow(() -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));

        // Optionally, create it and assert some values
        Transaction tx = new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource);
        assertEquals(defaultTransactionId, tx.getTransactionId());
        assertEquals(defaultStatus, tx.getTransactionStatus());
        assertEquals(defaultSource, tx.getTransactionSource());
        // ... assert other fields ...
    }

    // --- Tests for Quantity and PricePerUnit Validation (Specific to BUY/SELL) ---

    @Test
    void testTransaction_buyType_nullQuantity_throwsException() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.BUY, defaultAmount,
                defaultTransactionDate, defaultDescription, null, defaultPricePerUnit, // Null Quantity
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Quantity is required and must be positive for BUY/SELL transactions.", e.getMessage());
    }

    @Test
    void testTransaction_sellType_zeroPricePerUnit_throwsException() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.SELL, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, BigDecimal.ZERO, // Zero PricePerUnit
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Price per unit is required and must be positive for BUY/SELL transactions.", e.getMessage());
    }

    @Test
    void testTransaction_otherType_nullQuantityAndPrice_success() {
        // Transactions like DEPOSIT or OTHER should allow null quantity/pricePerUnit
        assertDoesNotThrow(() -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.DEPOSIT, defaultAmount,
                defaultTransactionDate, defaultDescription, null, null, // Null Quantity & PricePerUnit
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                null, null, defaultSource // Also test null asset/liability IDs for cash-only
        ));
    }

    // --- Tests for Asset/Liability ID Linkage Validation ---

    @Test
    void testTransaction_nonCashOnlyType_noAssetOrLiabilityId_throwsException() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.BUY, defaultAmount, // Non-cash-only type
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                null, null, defaultSource // Both assetHoldingId and liabilityId are null
        ));
        assertEquals(
                "Transactions must be linked to an Asset Holding or Liability, unless it's a cash-only type (DEPOSIT, WITHDRAWAL, DIVIDEND, INTEREST_INCOME).",
                e.getMessage());
    }

    @Test
    void testTransaction_cashOnlyType_noAssetOrLiabilityId_success() {
        assertDoesNotThrow(() -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.DEPOSIT, defaultAmount, // Cash-only type
                defaultTransactionDate, defaultDescription, null, null, // Quantity/Price can be null for deposit
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                null, null, defaultSource // Both assetHoldingId and liabilityId are null (valid for cash-only)
        ));
    }

    @Test
    void testTransaction_nonTransferType_bothAssetAndLiabilityId_throwsException() {
        // Assuming TRANSFER_IN and TRANSFER_OUT are the only types that allow both
        // Ensure you uncommented the 'throw' in your actual constructor for this rule!
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.BUY, defaultAmount, // Non-transfer type
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                UUID.randomUUID(), UUID.randomUUID(), defaultSource // Both asset and liability IDs
        ));
        assertEquals(
                "Transaction cannot be linked to both an Asset Holding and a Liability unless it's a specific transfer type.",
                e.getMessage());
    }

    @Test
    void testTransaction_transferInType_bothAssetAndLiabilityId_success() {
        assertDoesNotThrow(() -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.TRANSFER_IN, defaultAmount,
                defaultTransactionDate, defaultDescription, null, null, // Assuming transfers don't need quantity/price
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                UUID.randomUUID(), UUID.randomUUID(), defaultSource // Both asset and liability IDs (valid for transfer)
        ));
        assertDoesNotThrow(() -> new Transaction(
                defaultTransactionId, defaultPortfolioId, TransactionType.TRANSFER_OUT, defaultAmount,
                defaultTransactionDate, defaultDescription, null, null, // Assuming transfers don't need quantity/price
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                UUID.randomUUID(), UUID.randomUUID(), defaultSource // Both asset and liability IDs (valid for transfer)
        ));
    }

    // --- Tests for Void Details Validation based on Status ---

    @Test
    void testTransaction_voidedStatus_nullReason_throwsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.VOIDED, null, Instant.now(), // VOIDED status, but null reason
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Void reason must be provided if status is VOIDED.", e.getMessage());
    }

    @Test
    void testTransaction_voidedStatus_emptyReason_throwsException() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.VOIDED, "   ", Instant.now(), // VOIDED status, but blank reason
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Void reason cannot be empty or blank if status is VOIDED.", e.getMessage());
    }

    @Test
    void testTransaction_voidedStatus_nullVoidedAt_throwsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.VOIDED, "Some reason", null, // VOIDED status, but null voidedAt
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Voided timestamp must be provided if status is VOIDED.", e.getMessage());
    }

    @Test
    void testTransaction_activeStatus_withVoidReason_throwsException() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.ACTIVE, "Some reason", Instant.now(), // ACTIVE status, but void details present
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Void reason and timestamp must be null if status is not VOIDED.", e.getMessage());
    }

    @Test
    void testTransaction_pendingStatus_withVoidedAt_throwsException() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                TransactionStatus.PENDING, null, Instant.now(), // PENDING status, but voidedAt present
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Void reason and timestamp must be null if status is not VOIDED.", e.getMessage());
    }

    // --- Null Checks for Core Required Fields (from Objects.requireNonNull) ---

    @Test
    void testTransaction_nullTransactionId_throwsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                null, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Transaction ID cannot be null.", e.getMessage());
    }

    @Test
    void testTransaction_nullPortfolioId_throwsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, null, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Portfolio ID cannot be null.", e.getMessage());
    }

    @Test
    void testTransaction_nullTransactionType_throwsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, null, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Transaction type cannot be null.", e.getMessage());
    }

    @Test
    void testTransaction_nullAmount_throwsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, null,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Amount cannot be null.", e.getMessage());
    }

    @Test
    void testTransaction_nullTransactionDate_throwsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                null, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Transaction date cannot be null.", e.getMessage());
    }

    @Test
    void testTransaction_nullDescription_throwsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, null, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Description cannot be null.", e.getMessage());
    }

    @Test
    void testTransaction_nullTransactionStatus_throwsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                null, defaultVoidReason, defaultVoidedAt, // Null status
                defaultAssetHoldingId, defaultLiabilityId, defaultSource));
        assertEquals("Transaction status must not be null.", e.getMessage());
    }

    @Test
    void testTransaction_nullTransactionSource_throwsException() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> new Transaction(
                defaultTransactionId, defaultPortfolioId, defaultTransactionType, defaultAmount,
                defaultTransactionDate, defaultDescription, defaultQuantity, defaultPricePerUnit,
                defaultStatus, defaultVoidReason, defaultVoidedAt,
                defaultAssetHoldingId, defaultLiabilityId, null // Null source
        ));
        assertEquals("Transaction source must not be null.", e.getMessage());
    }
    // ---

    @Test
    void testEquals() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;

        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10000);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();

        Transaction transaction = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant,
                cpu, assetUuid, null);
        Transaction transaction2 = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant,
                cpu, assetUuid, null);
        Transaction transaction3 = new Transaction(UUID.randomUUID(), portfolioUuid, type, money, transDate, desc,
                quant, cpu, assetUuid, null);

        assertTrue(transaction.equals(transaction));
        assertTrue(transaction.equals(transaction2));
        assertFalse(transaction.equals(transaction3));
        assertFalse(transaction.equals(null));
        assertFalse(transaction.equals(""));
    }

    // technically transaction shouldn't have the same hash code
    @Test
    void testHashCode() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;

        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10000);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();

        Transaction transaction = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant,
                cpu, assetUuid, null);
        Transaction transaction2 = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant,
                cpu, assetUuid, null);
        Transaction transaction3 = new Transaction(UUID.randomUUID(), portfolioUuid, type, money, transDate, desc,
                quant, cpu, assetUuid, null);

        assertTrue(transaction.hashCode() == transaction2.hashCode());
        assertTrue(transaction.hashCode() != transaction3.hashCode());

    }

    @Test
    void testMarkAsVoidedBranches() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;

        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10000);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();

        Transaction transaction = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant,
                cpu, assetUuid, null);

        assertThrows(NullPointerException.class, () -> transaction.markAsVoided(null));
        assertThrows(IllegalArgumentException.class, () -> transaction.markAsVoided(""));

        Transaction transaction2 = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant,
                cpu, TransactionStatus.ACTIVE, null, null, assetUuid, null, TransactionSource.PLATFORM_SYNC);
        assertThrows(IllegalStateException.class, () -> transaction2.markAsVoided("Some Reason"));

        Transaction transaction3 = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant,
                cpu, TransactionStatus.FAILED, null, null, assetUuid, null, TransactionSource.MANUAL_INPUT);
        assertThrows(IllegalArgumentException.class, () -> transaction3.markAsVoided("Some Reason"));

        Transaction transaction4 = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant,
                cpu, TransactionStatus.ACTIVE, null, null, assetUuid, null, TransactionSource.MANUAL_INPUT);
        transaction4.markAsVoided("some reason");
    }

    @Test
    void testMarkAsVoidedGood() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;

        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10000);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();

        Transaction transaction = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant,
                cpu, assetUuid, null);
        transaction.markAsVoided("some good reason");
    }

    @Test
    void testAllGetters() {
        UUID transicationUuid = UUID.randomUUID();
        UUID portfolioUuid = UUID.randomUUID();
        TransactionType type = TransactionType.BUY;

        Money money = new Money(new BigDecimal(100), new PortfolioCurrency("USD", "$"));
        Instant transDate = Instant.now();
        String desc = "some desc";
        BigDecimal quant = new BigDecimal(10000);
        BigDecimal cpu = new BigDecimal(100);
        UUID assetUuid = UUID.randomUUID();

        Transaction transaction = new Transaction(transicationUuid, portfolioUuid, type, money, transDate, desc, quant,
                cpu, assetUuid, null);

        assertEquals(transicationUuid, transaction.getTransactionId());
        assertEquals(portfolioUuid, transaction.getPortfolioId());
        assertEquals(type, transaction.getTransactionType());
        assertEquals(money, transaction.getAmount());
        assertEquals(transDate, transaction.getTransactionDate());
        assertEquals(desc, transaction.getDescription());
        assertEquals(quant, transaction.getQuantity());
        assertEquals(cpu, transaction.getPricePerUnit());
        assertEquals(assetUuid, transaction.getAssetHoldingId());
        assertEquals(null, transaction.getLiabilityId());
        assertEquals(com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionStatus.ACTIVE,
                transaction.getTransactionStatus());
        assertEquals(TransactionSource.MANUAL_INPUT, transaction.getTransactionSource());
        assertTrue(transaction.getVoidReason() == null);
        assertTrue(transaction.getVoidedAt() == null);
    }
}
