package com.laderrco.fortunelink.PortfolioManagement.domain.Services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.AssetHolding;
import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Liability;
import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Portfolio;
import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Transaction;
import com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces.ITransactionCorrectionService;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionType;

public class TransactionCorrectionService implements ITransactionCorrectionService {

    @Override
    public void applyCompensationForVoidedTransaction(Portfolio portfolio, Transaction originalTransaction,String reason) {
        // NOTE: The following is a lot of horrendous AI logic, basically the code below
        // is saying that depending on the type of transaction it was before
        // we need to create a compensating transaction and revert financial impact to
        // ensure financial integrity
        // Example for a BUY transaction:
        if (originalTransaction.getTransactionType() == TransactionType.BUY) {
            // Find the associated AssetHolding
            Optional<AssetHolding> optionalHolding = portfolio.getAssets().stream()
                    .filter(ah -> ah.getAssetHoldingId().equals(originalTransaction.getAssetHoldingId()))
                    .findFirst();

            if (optionalHolding.isEmpty()) {
                // This is an error state: A BUY transaction should always have an AssetHolding
                throw new IllegalStateException("AssetHolding for voided BUY transaction not found.");
            }

            AssetHolding holding = optionalHolding.get();

            // Create a compensating SALE transaction to reduce the asset holding
            // You might need a specific 'VOID_BUY' TransactionType if you want to
            // distinguish
            // or just use a SELL type with negative values for amount/quantity.
            Money originalCostPerUnit = new Money(originalTransaction.getPricePerUnit(),
                    originalTransaction.getAmount().currencyCode());
            Money compensatingSaleProceeds = originalCostPerUnit.multiply(originalTransaction.getQuantity());

            // Simulate the 'sale' of the asset that was bought to void it
            holding.recordSale(originalTransaction.getQuantity(), originalCostPerUnit); // This will decrease quantity and
                                                                                      // adjust cost basis

            // Create a new compensating transaction for the audit trail
            Transaction compensatingTransaction = new Transaction(
                    UUID.randomUUID(),
                    portfolio.getPortfolioId(),
                    TransactionType.VOID_BUY, // Or a specific VOID_BUY type if you have one
                    compensatingSaleProceeds,
                    Instant.now(), // Date of voiding, not original transaction
                    "VOID: Reversed purchase of " + originalTransaction.getQuantity() + " units for "
                            + originalTransaction.getDescription() + ". Reason: " + reason,
                    originalTransaction.getQuantity(),
                    originalCostPerUnit.amount(),
                    originalTransaction.getAssetHoldingId(),
                    null

            );
            portfolio.addInternalTransaction(compensatingTransaction);

            // If the holding quantity drops to 0, remove it (similar to recordAssetSale)
            if (holding.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                portfolio.getAssets().remove(holding);
            }

        }
        // You would need similar 'else if' blocks for other TransactionTypes
        // (e.g., voiding a SELL might create a BUY, voiding a DEPOSIT might create a
        // WITHDRAWAL)
        else if (originalTransaction.getTransactionType() == TransactionType.DEPOSIT) {
            // Create a compensating WITHDRAWAL transaction
            Money compensatingWithdrawal = originalTransaction.getAmount().negate(); // Assuming Money can handle
                                                                                   // negation, NOTE: this will throw an
                                                                                   // error most likely
            Transaction compensatingTransaction = new Transaction(
                    UUID.randomUUID(),
                    portfolio.getPortfolioId(),
                    TransactionType.WITHDRAWAL, // Create a withdrawal
                    compensatingWithdrawal,
                    Instant.now(),
                    "VOID: Reversed deposit of " + originalTransaction.getAmount() + ". Reason: " + reason,
                    null, null, null, null);
            portfolio.addInternalTransaction(compensatingTransaction);
        }
        // ... handle other types like WITHDRAWAL, LOAN_PAYMENT, DIVIDEND, etc.
        else if (originalTransaction.getTransactionType() == TransactionType.WITHDRAWAL) {
            // Create a compensating DEPOSIT transaction
            Money compensatingDeposit = originalTransaction.getAmount(); // Original amount of withdrawal
            Transaction compensatingTransaction = new Transaction(
                    UUID.randomUUID(),
                    portfolio.getPortfolioId(),
                    TransactionType.VOID_WITHDRAWAL, // Use VOID_WITHDRAWAL type
                    compensatingDeposit.negate(), // A negative withdrawal is a positive deposit
                    Instant.now(),
                    "VOID: Reversed withdrawal of " + originalTransaction.getAmount() + ". Reason: " + reason,
                    null, null, null, null);
            portfolio.addInternalTransaction(compensatingTransaction);
        } else if (originalTransaction.getTransactionType() == TransactionType.LOAN_PAYMENT) {
            // Revert the payment on the liability
            Optional<Liability> optionalLiability = portfolio.getLiabilities().stream()
                    .filter(l -> l.getLiabilityId().equals(originalTransaction.getLiabilityId()))
                    .findFirst();

            if (optionalLiability.isEmpty()) {
                throw new IllegalStateException("Liability for voided LOAN_PAYMENT transaction not found.");
            }
            Liability liability = optionalLiability.get();
            // Assuming Liability has a method to "reverse" a payment or increase balance
            // For simplicity, we'll manually add back the amount for this mock
            liability.increaseBalance(originalTransaction.getAmount()); // Use the original positive payment amount

            Transaction compensatingTransaction = new Transaction(
                    UUID.randomUUID(),
                    portfolio.getPortfolioId(),
                    TransactionType.OTHER, // Or a specific VOID_LOAN_PAYMENT type
                    originalTransaction.getAmount().negate(), // Reversing a payment
                    Instant.now(),
                    "VOID: Reversed loan payment for liability '" + liability.getName() + "'. Reason: " + reason,
                    null, null, null, liability.getLiabilityId());
            portfolio.addInternalTransaction(compensatingTransaction);
        }

        // ---
    }

}
