package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.lang.foreign.Linker.Option;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetAllocation;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.ReversalTransactionDetails;
import com.laderrco.fortunelink.shared.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.shared.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class Portfolio {
	private final UUID portfolioId;
	private final UUID userId;
	private String portfolioName;
	private String portfolioDescription;
	private Money portfolioCashBalance;
	private Currency currencyPreference; // view everything in one currency, preference
	private List<Transaction> transactions;
	private List<AssetHolding> assetHoldings;
	private List<Liability> liabilities;

	private final ExchangeRateService exchangeRateService;

	public Portfolio(
			UUID portfolioId,
			UUID userId,
			String portfolioName,
			String portfolioDescription,
			Money portfolioCashBalance,
			Currency currencyPreference,
			ExchangeRateService exchangeRateService) {
		this.portfolioId = portfolioId;
		this.userId = userId;
		this.portfolioName = portfolioName;
		this.portfolioDescription = portfolioDescription;
		this.portfolioCashBalance = portfolioCashBalance;
		this.currencyPreference = currencyPreference;
		this.transactions = new ArrayList<>();
		this.assetHoldings = new ArrayList<>();
		this.liabilities = new ArrayList<>();

		this.exchangeRateService = exchangeRateService;
	}

	private Money getTotalFeesAmount(List<Fee> fees) {
		// TODO change this to accept FEE
		Money totalFeesAmount = Money.ZERO(this.currencyPreference);
		for (Fee fee : fees) {
			Money feeAmount = fee.amount();
			if (!feeAmount.currency().equals(this.currencyPreference)) {

				feeAmount = exchangeRateService.convert(feeAmount, this.currencyPreference);
				// NOTE: fees are not expected to be in the portfolio's currency, we will be
				// converting
				// if we did expect if to be in the currency fee amount, throw an error
			}

			totalFeesAmount = totalFeesAmount.add(feeAmount);
		}
		return totalFeesAmount;
	}

	// the parameter head might be wrong
	// do we abstract some of the Transaction methods to the TransactionDetails
	// class?
	// we are trusting 'details' for all the pre calcualted financial gigures for a
	// transaction
	public void recordCashflow(CashflowTransactionDetails details, CommonTransactionInput commonTransactionInput,
			Instant transactionDate) {

		Money totalAmount = details.getConvertedCashflowAmount();

		Money totalFeesAmount = getTotalFeesAmount(commonTransactionInput.fees());

		Transaction newCashTransaction = new Transaction(
				UUID.randomUUID(),
				this.portfolioId,
				commonTransactionInput.correlationId(),
				commonTransactionInput.parentTransactionId(),
				commonTransactionInput.transactionType(),
				totalAmount,
				transactionDate,
				details,
				commonTransactionInput.transactionMetadata(),
				commonTransactionInput.fees(),
				false,
				1);

		this.transactions.add(newCashTransaction);

		Money netCashImpact = Money.ZERO(this.currencyPreference);
		if (Set.of(TransactionType.DEPOSIT, TransactionType.INTEREST, TransactionType.DIVIDEND)
				.contains(commonTransactionInput.transactionType())) {
			netCashImpact = totalAmount.subtract(totalFeesAmount);
		} else if (Set.of(TransactionType.WITHDRAWAL).contains(commonTransactionInput.transactionType())) {
			// For withdrawal, both the withdrawn amount and fees are subtracted.
			// Assuming principalTransactionAmount is positive (amount withdrawn).
			netCashImpact = totalAmount.negate().subtract(totalFeesAmount);
		} else {
			// Handle other cashflow types or throw an exception if type is not recognized
			// for cash balance update.
			// For example, an `ACCOUNT_TRANSFER` might have a different net impact.
			throw new IllegalArgumentException("Unsupported cashflow transaction type for balance update: "
					+ commonTransactionInput.transactionType());
		}

		this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact);
	}

	public void recordAssetPurchase(
		AssetTransactionDetails details, 
		CommonTransactionInput commonTransactionInput,
		Instant transactionDate
	) {
		Objects.requireNonNull(details, "details cannot be null.");
		Objects.requireNonNull(commonTransactionInput, "commonTransactionInput cannot be null.");
		Objects.requireNonNull(transactionDate, "transactionDate cannot be null.");

		if (commonTransactionInput.transactionType() != TransactionType.BUY) {
			throw new IllegalArgumentException("Expected BUY transaction type, got: "+ commonTransactionInput.transactionType());
		}

		Optional<AssetHolding> existingHolding = assetHoldings.stream()
			.filter(ah -> ah.getAssetIdentifier().equals(details.getAssetIdentifier()))
			.findFirst();
		
		AssetHolding holding;
		
		if (existingHolding.isPresent()) {
			holding = existingHolding.get();
			holding.addToPosition(details.getQuantity(), details.getCostBasisInAssetCurrency());
		} 
		else {
			holding = new AssetHolding(
				UUID.randomUUID(),
				this.portfolioId,
				details.getAssetIdentifier(),
				details.getQuantity(),
				details.getCostBasisInAssetCurrency(),
				transactionDate
			);
			this.assetHoldings.add(holding);
		}
			
		// The costBasisInPortfolioCurrency from details *should already include* all fees.
		// So, this is the total cash outflow for the purchase.
		Money cashOutflow = details.getCostBasisInPortfolioCurrency();
		Money netCashImpact = cashOutflow.negate();
		Money newBalance = this.portfolioCashBalance.add(netCashImpact);
		if (newBalance.isNegative()) {
			throw new InsufficientFundsException("Insufficient cash for asset purchase.");
		}

		this.portfolioCashBalance = newBalance;

		Transaction newAssetTransaction = new Transaction(
			UUID.randomUUID(),
			this.portfolioId,
			commonTransactionInput.correlationId(),
			commonTransactionInput.parentTransactionId(),
			commonTransactionInput.transactionType(),
			cashOutflow,
			transactionDate,
			details,
			commonTransactionInput.transactionMetadata(),
			commonTransactionInput.fees(),
			false,
			1
		);
		this.transactions.add(newAssetTransaction);
	}

	public void recordAssetSale(AssetTransactionDetails details, CommonTransactionInput commonTransactionInput,
			Instant transactionDate) {
		Objects.requireNonNull(details, "details cannot be null.");
		Objects.requireNonNull(commonTransactionInput, "commonTransactionInput cannot be null.");
		Objects.requireNonNull(transactionDate, "transactionDate cannot be null.");

		if (commonTransactionInput.transactionType() != TransactionType.SELL) {
			throw new IllegalArgumentException("Expected SELL transaction type, got: "
					+ commonTransactionInput.transactionType());
		}

		Optional<AssetHolding> existingHolding = assetHoldings.stream()
				.filter(ah -> ah.getAssetIdentifier().equals(details.getAssetIdentifier()))
				.findFirst();

		if (existingHolding.isEmpty()) {
			throw new AssetNotFoundException("Cannot sell asset not held in portfolio: "
					+ details.getAssetIdentifier().symbol());
		}

		AssetHolding holding = existingHolding.get();
		if (holding.getTotalQuantity().compareTo(details.getQuantity()) < 0) {
			throw new IllegalArgumentException("Cannot sell more units than you have.");
		}

		Money netCashImpact = details.getAssetValueInPortfolioCurrency().subtract(details.getTotalFeesInPortfolioCurrency());
		if (!this.portfolioCashBalance.currency().equals(netCashImpact.currency())) {
			throw new IllegalArgumentException(
					"Portfolio cash balance currency does not match transaction's net cash impact currency.");
		}
		this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact); // Cash increases

		holding.removeFromPosition(details.getQuantity());
		Transaction newAssetTransaction = new Transaction(
				UUID.randomUUID(),
				this.portfolioId,
				commonTransactionInput.correlationId(),
				commonTransactionInput.parentTransactionId(),
				commonTransactionInput.transactionType(), // Should be SELL
				details.getAssetValueInPortfolioCurrency(), // Store the gross proceeds as the
										// transaction amount
				transactionDate,
				details,
				commonTransactionInput.transactionMetadata(),
				commonTransactionInput.fees(), // Store the individual fees
				false,
				1);
		this.transactions.add(newAssetTransaction);
	}

	// liabilities, you technically gain cash
	// when you take out a loan, your cash balance increases by the loan principal minus any fees
	public void recordNewLiability(LiabilityIncurrenceTransactionDetails details, CommonTransactionInput commonTransactionInput, Instant transactionDate) {
		Objects.requireNonNull(details, "details cannot be null.");
		Objects.requireNonNull(commonTransactionInput, "commonTransactionInput cannot be null.");
		Objects.requireNonNull(transactionDate, "TransactionDate cannot be null.");

		if (!Set.of(TransactionType.LIABILITY_INCURRENCE, TransactionType.DEPOSIT).contains(commonTransactionInput.transactionType())) {
			throw new IllegalArgumentException("Liability incurrence transaction type must be LIABILITY_INCURRENCE or DEPOSIT.");
		}
	
		Money netCashReceived = details.getOriginalLoanAmountInPortfolioCurrency().subtract(details.getTotalFeesInPortfolioCurrency());
		this.portfolioCashBalance = this.portfolioCashBalance.add(netCashReceived);
		
		Transaction newLiabilityIncurrenceTransaction = new Transaction(
			UUID.randomUUID(),
			this.portfolioId,
			commonTransactionInput.correlationId(),
			commonTransactionInput.parentTransactionId(),
			commonTransactionInput.transactionType(),
			netCashReceived,
			transactionDate,
			details,
			commonTransactionInput.transactionMetadata(),
			commonTransactionInput.fees(),
			false,
			1
		);
		this.transactions.add(newLiabilityIncurrenceTransaction);


		Liability liability = new Liability(
			UUID.randomUUID(),
			this.portfolioId,
			details.getLiabilityName(),
			details.getDescription(),
			details.getOriginalLoanAmount().subtract(details.getTotalFeesInLiabilityCurrency()),
			details.getAnnualInterestRate(),
			details.getMaturityDate(),
			transactionDate
		);

		this.liabilities.add(liability);
		
	}

	public void recordLiabilityPayment(LiabilityPaymentTransactionDetails details, CommonTransactionInput commonTransactionInput, Instant transactionDate) {
		Objects.requireNonNull(details, "details cannot be null.");
		Objects.requireNonNull(commonTransactionInput, "commonTransactionInput cannot be null.");
		Objects.requireNonNull(transactionDate, "TransactionDate cannot be null.");

		if (!Set.of(TransactionType.PAYMENT).contains(commonTransactionInput.transactionType())) {
			throw new IllegalArgumentException("Liability incurrence transaction type must be PAYMENT.");
		}

		Optional<Liability> exitingLiability = this.liabilities.stream()
			.filter(l -> l.getLiabilityId().equals(details.getLiabilityId()))
			.findFirst();
		
		if (exitingLiability.isEmpty()) {
			throw new IllegalArgumentException(String.format("Liability with ID %s not found in portfolio.", details.getLiabilityId()));
		}

		Liability liability = exitingLiability.get();

		Money totalCashOutflow = details.getTotalPaymentAmountInPortfolioCurrency().add(getTotalFeesAmount(commonTransactionInput.fees()));
		Money newCashBalance = this.portfolioCashBalance.subtract(totalCashOutflow);

		if (newCashBalance.isNegative()) {
			throw new InsufficientFundsException("Insufficient cash to make liability payment.");
		}
		this.portfolioCashBalance = newCashBalance;
		
		// Reduce the liability's balance by the principal portion of the payment.
        // The principal paid is the total payment in liability currency minus interest and fees (in liability currency).
        Money principalReductionAmount = details.getTotalPaymentAmountInLiabilityCurrency()
                                             .subtract(details.getInterestAmountInLiabilityCurrency())
                                             .subtract(details.getFeesAmountInLiabilityCurrency());

       
        liability.applyPayment(principalReductionAmount);

		Transaction newPaymentTransaction = new Transaction(
            UUID.randomUUID(),
            this.portfolioId,
            commonTransactionInput.correlationId(),
            commonTransactionInput.parentTransactionId(),
            commonTransactionInput.transactionType(), // Should be PAYMENT
            totalCashOutflow, // The total amount that affected cash (a positive value representing outflow)
            transactionDate,
            details, // Store the payment details
            commonTransactionInput.transactionMetadata(),
            commonTransactionInput.fees(), // Any fees associated with this transaction
            false, // Typically not a realized gain/loss event
            1
        );
        this.transactions.add(newPaymentTransaction);
	}

	public void reverseTransaction(UUID transactionId, String reason, Instant transactionDate) {
		ReversalTransactionDetails reversalTransactionDetails = new ReversalTransactionDetails(transactionId, reason);

		Optional<Transaction> reversedTransaction = this.transactions.stream()
			.filter(t -> t.getTransactionId().equals(transactionId))
			.findFirst();
		
		if (reversedTransaction.isEmpty()) {
			throw new IllegalArgumentException("Transaction to reverse does not exist");
		}

      	Transaction originalTransaction = reversedTransaction.get();

        // Prevent reversing a reversal (or apply specific logic if allowed)
        if (originalTransaction.getTransactionType() == TransactionType.REVERSAL) {
             throw new IllegalArgumentException("Cannot reverse a reversal transaction.");
        }
        
        // Prepare common input for the reversal *event* transaction record
        // Note: The specific commonInput for the *underlying cashflow reversal*
        // will be created inside the switch statement if needed.
        CommonTransactionInput reversalCommonInput = new CommonTransactionInput(
            UUID.randomUUID(), // New correlation ID for this reversal event
            originalTransaction.getParentTransactionId(), // Parent ID links to the original transaction
            TransactionType.REVERSAL, // Generic reversal type
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.SYSTEM, reason, transactionDate, transactionDate),
            null // Fees for the reversal transaction itself, if any
        );

        // This will be the net cash impact of the *entire reversal operation*
        // This variable is primarily for the REVERSAL transaction record's totalAmount
        Money netCashImpactOfReversalOperation = Money.ZERO(this.currencyPreference); 

		switch (originalTransaction.getTransactionType()) {
			case DEPOSIT:
			case WITHDRAWAL:
			case INTEREST:
			case DIVIDEND:
				if (!(originalTransaction.getTransactionDetails() instanceof CashflowTransactionDetails)) {
					throw new IllegalArgumentException("Original transaction details are not of CashflowTransactionDetails type ofr reversal.");
				}

				CashflowTransactionDetails originalCashflowDetails = (CashflowTransactionDetails) originalTransaction.getTransactionDetails();
				
				Money originalConvertedAmount = originalCashflowDetails.getConvertedCashflowAmount();
                Money originalTotalConversionFees = originalCashflowDetails.getTotalConversionFees();
             
 				List<Fee> originalOtherFeesList = originalTransaction.getFees(); // Access directly from the Transaction record
                Money originalOtherFeesFromTransaction = getTotalFeesAmount( originalOtherFeesList);                
				TransactionType inverseType;         
				if (originalTransaction.getTransactionType() == TransactionType.DEPOSIT ||
                    originalTransaction.getTransactionType() == TransactionType.INTEREST ||
                    originalTransaction.getTransactionType() == TransactionType.DIVIDEND) {
                    inverseType = TransactionType.WITHDRAWAL; // Reversing an inflow means an outflow
                } else if (originalTransaction.getTransactionType() == TransactionType.WITHDRAWAL) {
                    inverseType = TransactionType.DEPOSIT; // Reversing an outflow means an inflow
                } else {
                    // This case should ideally not be reached if the outer switch covers only cashflow types.
                    throw new IllegalStateException("Unexpected cashflow type for reversal: " + originalTransaction.getTransactionType());
                }
              	
				CashflowTransactionDetails reversalCashflowDetails = new CashflowTransactionDetails(
                    originalConvertedAmount,
                    originalConvertedAmount,
                    originalTotalConversionFees,
                    originalCashflowDetails.getExchangeRate()
                );

   				CommonTransactionInput reversalSubTransactionCommonInput = new CommonTransactionInput(
                    UUID.randomUUID(),
                    originalTransaction.getParentTransactionId(),
                    inverseType,
                    new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.SYSTEM, "Reversal of " + originalTransaction.getTransactionType() + ": " + reason, transactionDate, transactionDate),
                    originalOtherFeesList // Pass the list of fees directly from the original transaction
                );
                this.recordCashflow(reversalCashflowDetails, reversalSubTransactionCommonInput, transactionDate);
				// Calculate the net cash impact for the main REVERSAL transaction record.
                Money originalNetCashImpact;
                if (Set.of(TransactionType.DEPOSIT, TransactionType.INTEREST, TransactionType.DIVIDEND).contains(originalTransaction.getTransactionType())) {
                    originalNetCashImpact = originalConvertedAmount.subtract(originalTotalConversionFees).subtract(originalOtherFeesFromTransaction);
                } else { // WITHDRAWAL
                    originalNetCashImpact = originalConvertedAmount.negate().subtract(originalTotalConversionFees).subtract(originalOtherFeesFromTransaction);
                }
                netCashImpactOfReversalOperation = originalNetCashImpact.negate();
				break;

			case BUY:
				
				break;
		
			case SELL:
				
				break;
			
			case LIABILITY_INCURRENCE:
				break;
			
			case PAYMENT:
				break;
		
			default:
				throw new UnsupportedOperationException("Reversal not supported for transaction type: " + originalTransaction.getTransactionType());
		}
		


		Transaction reversalTransaction = new Transaction(
			UUID.randomUUID(),
			this.portfolioId,
			UUID.randomUUID(),
			originalTransaction.getTransactionId(),
			TransactionType.REVERSAL_BUY, // need to fix this, probably just pass it as a param
			netCashImpactOfReversalOperation, // we would need to add back/ remove the money
			transactionDate,
			reversalTransactionDetails,
			new TransactionMetadata(null, null, reason, transactionDate, transactionDate),
			null,
			true,
			originalTransaction.getVersion()+1
		);

		this.transactions.add(reversalTransaction);

	}

	public Money calculateTotalValue(Map<AssetIdentifier, MarketPrice> currentPrices) {
		// total value in portfolio's preference
		// this is an example, we would need an acutal service class to handle this
		// TODO switch to acutal service, currenyl using CAD -> USD
		// Money total = Money.ZERO(this.currencyPreference);
		// for (AssetHolding assetHolding : assetHoldings) {
		// // check if we need to even do it
		// MarketPrice price = currentPrices.get(assetHolding.getAssetIdentifier());

		// if (price != null) {
		// Money holdingValue = assetHolding.getCurrentValue(price);
		// Money convertedValue = exchangeRateService.convert(holdingValue,
		// this.currencyPreference);
		// total = total.add(convertedValue);
		// }
		// }
		// return total;
		return null;
	}

	public Money calculateUnrealizedGains(AssetAllocation currentPrices) {
		return null;
	}

	public AssetAllocation getAssetAllocation(Map<AssetIdentifier, MarketPrice> currentPrices) {
		return null;
		// Money totalValue = calculateTotalValue(currentPrices);
		// AssetAllocation allocation = new AssetAllocation(totalValue,
		// this.currencyPreference);

		// for (AssetHolding assetHolding : assetHoldings) {
		// MarketPrice marketPrice =
		// currentPrices.get(assetHolding.getAssetIdentifier());
		// if (marketPrice != null) {
		// Money holdingValue = assetHolding.getCurrentValue(marketPrice); // this needs
		// to return value in portfolio pref

		// Money covertedValue = exchangeRateService.convert(holdingValue,
		// this.currencyPreference);

		// Percentage percentage = new Percentage(
		// covertedValue.amount()
		// .divide(totalValue.amount(), DecimalPrecision.PERCENTAGE.getDecimalPlaces(),
		// RoundingMode.HALF_UP)
		// .multiply(BigDecimal.valueOf(100))
		// );

		// allocation.addAllocation(assetHolding.getAssetIdentifier(), covertedValue,
		// percentage);
		// }
		// }

		// return allocation;
	}

	public void accruelInterestLiabilities() {

	}

	public UUID getPortfolioId() {
		return portfolioId;
	}

	public UUID getUserId() {
		return userId;
	}

	public String getPortfolioName() {
		return portfolioName;
	}

	public String getPortfolioDescription() {
		return portfolioDescription;
	}

	public Money getPortfolioCashBalance() {
		return portfolioCashBalance;
	}

	public Currency getCurrencyPreference() {
		return currencyPreference;
	}

	public List<Transaction> getTransactions() {
		return transactions;
	}

	public List<AssetHolding> getAssetHoldings() {
		return assetHoldings;
	}

	public List<Liability> getLiabilities() {
		return liabilities;
	}

	public ExchangeRateService getExchangeRateService() {
		return exchangeRateService;
	}

}
