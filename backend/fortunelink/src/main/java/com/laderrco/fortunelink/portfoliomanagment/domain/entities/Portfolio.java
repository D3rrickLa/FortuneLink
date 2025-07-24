package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.ReversalTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.SimpleTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.exceptions.InsufficientFundsException;

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
		Money totalFeesAmount = Money.ZERO(this.currencyPreference);
		for (Fee fee : fees) {
			Money feeAmount = fee.amount();
			if (!feeAmount.currency().equals(this.currencyPreference)) {
				feeAmount = exchangeRateService.convert(feeAmount, this.currencyPreference);
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
	public void recordCashflow(CashflowTransactionDetails details, CommonTransactionInput commonTransactionInput, Instant transactionDate) {

		Money totalAmount = details.getConvertedCashflowAmount();
		Money totalFeesAmount = getTotalFeesAmount(commonTransactionInput.fees());

		Money netCashImpact = Money.ZERO(this.currencyPreference);
		if (Set.of(TransactionType.DEPOSIT, TransactionType.INTEREST, TransactionType.DIVIDEND)
				.contains(commonTransactionInput.transactionType())) {
			netCashImpact = totalAmount.subtract(totalFeesAmount);
		} 
		else if (Set.of(TransactionType.WITHDRAWAL).contains(commonTransactionInput.transactionType())) {
			// For withdrawal, both the withdrawn amount and fees are subtracted.
			// Assuming principalTransactionAmount is positive (amount withdrawn).
			netCashImpact = totalAmount.negate().subtract(totalFeesAmount);
		} 
		else {
			// Handle other cashflow types or throw an exception if type is not recognized
			// for cash balance update.
			// For example, an `ACCOUNT_TRANSFER` might have a different net impact.
			throw new IllegalArgumentException("Unsupported cashflow transaction type for balance update: "
					+ commonTransactionInput.transactionType());
		}

		this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact);
		Transaction newCashTransaction = new Transaction(
			UUID.randomUUID(),
			this.portfolioId,
			commonTransactionInput.correlationId(),
			commonTransactionInput.parentTransactionId(),
			commonTransactionInput.transactionType(),
			netCashImpact,
			transactionDate,
			details,
			commonTransactionInput.transactionMetadata(),
			commonTransactionInput.fees(),
			false,
			1
		);

		this.transactions.add(newCashTransaction);
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
	public void recordNewLiability(LiabilityIncurrenceTransactionDetails initialDetails, CommonTransactionInput commonTransactionInput, Instant transactionDate) {
		Objects.requireNonNull(initialDetails, "details cannot be null.");
		Objects.requireNonNull(commonTransactionInput, "commonTransactionInput cannot be null.");
		Objects.requireNonNull(transactionDate, "TransactionDate cannot be null.");

		if (!Set.of(TransactionType.LIABILITY_INCURRENCE, TransactionType.DEPOSIT).contains(commonTransactionInput.transactionType())) {
			throw new IllegalArgumentException("Liability incurrence transaction type must be LIABILITY_INCURRENCE or DEPOSIT.");
		}
	
		Liability liability = new Liability(
			UUID.randomUUID(),
			this.portfolioId,
			initialDetails.getLiabilityName(),
			initialDetails.getDescription(),
			initialDetails.getOriginalLoanAmountInLiabilityCurrency().subtract(initialDetails.getTotalFeesInLiabilityCurrency()),
			initialDetails.getAnnualInterestRate(),
			initialDetails.getMaturityDate(),
			transactionDate
		);

		this.liabilities.add(liability);
		LiabilityIncurrenceTransactionDetails incurrenceTransactionDetails = new LiabilityIncurrenceTransactionDetails(
			liability.getLiabilityId(),        
			initialDetails.getLiabilityName(),
			initialDetails.getDescription(),
			initialDetails.getOriginalLoanAmountInLiabilityCurrency(),
			initialDetails.getOriginalLoanAmountInPortfolioCurrency(),
			initialDetails.getAnnualInterestRate(),
			initialDetails.getMaturityDate(),
			initialDetails.getTotalFeesInLiabilityCurrency(),
			initialDetails.getTotalFeesInPortfolioCurrency()
		);


		Money netCashImpact = incurrenceTransactionDetails.getOriginalLoanAmountInPortfolioCurrency().subtract(incurrenceTransactionDetails.getTotalFeesInPortfolioCurrency());
		this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpact);
		
		Transaction newLiabilityIncurrenceTransaction = new Transaction(
			UUID.randomUUID(),
			this.portfolioId,
			commonTransactionInput.correlationId(),
			commonTransactionInput.parentTransactionId(),
			commonTransactionInput.transactionType(),
			netCashImpact,
			transactionDate,
			incurrenceTransactionDetails,
			commonTransactionInput.transactionMetadata(),
			commonTransactionInput.fees(),
			false,
			1
		);
		this.transactions.add(newLiabilityIncurrenceTransaction);
		
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

	public void reverseTransaction(UUID reversalTransactionId, String reason, Instant reversalDate) {
		Objects.requireNonNull(reversalTransactionId, "reversalTransactionId cannot be null.");
		Objects.requireNonNull(reason, "reason cannot be null.");
		Objects.requireNonNull(reversalDate, "reversalDate cannot be null.");

		// what to do
		/*
		 * Find if the TransactionId exists
		 * if it does exists, find out what action it was
		 * depending on the action we reverse it accordingly
		 */

		Optional<Transaction> transactionOptional = this.transactions.stream()
			.filter(t -> t.getTransactionId().equals(reversalTransactionId))
			.findFirst();
		
		if (transactionOptional.isEmpty()) {
			throw new IllegalStateException("Original transaction not found.");
		}

		Transaction originalTransaction = transactionOptional.get();
		if (originalTransaction.getTransactionType().equals(TransactionType.REVERSAL)) {
			throw new IllegalStateException("Cannot reverse a reversal transaction.");
		}


		Money netCashImpactOfReversal = Money.ZERO(this.currencyPreference);

		switch (originalTransaction.getTransactionType()) {
			case DEPOSIT:
			case WITHDRAWAL:
				netCashImpactOfReversal = originalTransaction.getTotalTransactionAmount().negate();
				break;
				
			case LIABILITY_INCURRENCE:
				LiabilityIncurrenceTransactionDetails originalIncurrenceTransactionDetails = (LiabilityIncurrenceTransactionDetails) originalTransaction.getTransactionDetails();
				netCashImpactOfReversal = originalIncurrenceTransactionDetails.getOriginalLoanAmountInPortfolioCurrency().negate();

				// finding the liability and reduce its balance
				Optional<Liability> liabilityOptional = this.liabilities.stream()
					.filter(l -> l.getLiabilityId().equals(originalIncurrenceTransactionDetails.getLiabilityId())) // we don't have the liability id here
					.findFirst();
				if (liabilityOptional.isPresent()) {
					liabilityOptional.get().reduceLiabilityBalance(originalIncurrenceTransactionDetails.getOriginalLoanAmountInLiabilityCurrency());
				}
				else {
					throw new IllegalStateException("Liability not found for incurrence reversal.");
				}
				break;
			case PAYMENT:
				LiabilityPaymentTransactionDetails originalPaymentTransactionDetails = (LiabilityPaymentTransactionDetails) originalTransaction.getTransactionDetails();
				netCashImpactOfReversal = originalPaymentTransactionDetails.getTotalPaymentAmountInPortfolioCurrency();

				Money principalPaidInLiabilityCurrency = originalPaymentTransactionDetails.getTotalPaymentAmountInLiabilityCurrency()
					.subtract(originalPaymentTransactionDetails.getInterestAmountInLiabilityCurrency())
					.subtract(originalPaymentTransactionDetails.getFeesAmountInLiabilityCurrency());

				Optional<Liability> targetLiability = this.liabilities.stream()
					.filter(l -> l.getLiabilityId().equals(originalPaymentTransactionDetails.getLiabilityId()))
					.findFirst();
				if (targetLiability.isPresent()) {
					targetLiability.get().increaseLiabilityBalance(principalPaidInLiabilityCurrency); // Add principal back to liability
            	} 
				else {
                	throw new IllegalStateException("Liability not found for payment reversal.");
            	}
				break;

			default:
				throw new UnsupportedOperationException("Reversal for transaction type: " + originalTransaction.getTransactionType() +" not yet supported.");
		}
		this.portfolioCashBalance = this.portfolioCashBalance.add(netCashImpactOfReversal);

		ReversalTransactionDetails reversalTransactionDetails = new ReversalTransactionDetails(reversalTransactionId, reason);
		TransactionMetadata reversalMetadata = new TransactionMetadata(
			TransactionStatus.COMPLETED,
			TransactionSource.SYSTEM,
			"REVERSAL of transaction: " + reversalTransactionId.toString(),
			reversalDate,
			reversalDate
		);
		Transaction reversalTransaction = new Transaction(
			UUID.randomUUID(),
			this.portfolioId,
			UUID.randomUUID(), // correlationId
			reversalTransactionId,
			TransactionType.REVERSAL,
			netCashImpactOfReversal.negate(), // total amoutn of the reversal transaction
			reversalDate,
			reversalTransactionDetails,
			reversalMetadata,
			null, // fees, usually null
			false, // not hidden
			1
		);

		this.transactions.add(reversalTransaction);    
	}

	public Money calculateTotalValue(Map<AssetIdentifier, MarketPrice> currentPrices) {
		Money total = Money.ZERO(this.currencyPreference);
		for (AssetHolding assetHolding : assetHoldings) {
		// check if we need to even do it
		MarketPrice price = currentPrices.get(assetHolding.getAssetIdentifier());

		if (price != null) {
			Money holdingValue = assetHolding.getCurrentValue(price);
			Money convertedValue = exchangeRateService.convert(holdingValue,
			this.currencyPreference);
			total = total.add(convertedValue);
			}
		}
		return total;
	}

	public Money calculateUnrealizedGains(Map<AssetIdentifier, MarketPrice> currentPrices) {
		Money totalMarketValue = this.calculateTotalValue(currentPrices); // Re-use the method above

		Money totalCostBasis = Money.ZERO(this.currencyPreference);

		for (AssetHolding assetHolding : assetHoldings) {
			// Get the cost basis of the holding in its original currency
			Money costBasisInAssetCurrency = assetHolding.getTotalAdjustedCostBasis();

			// Convert that cost basis to the portfolio's preferred currency
			Money convertedCostBasisInPortfolioCurrency = exchangeRateService.convert(
				costBasisInAssetCurrency,
				this.currencyPreference
			);
			totalCostBasis = totalCostBasis.add(convertedCostBasisInPortfolioCurrency);
		}

		// Unrealized Gain = Current Market Value - Total Cost Basis
		return totalMarketValue.subtract(totalCostBasis);
	}

	public AssetAllocation getAssetAllocation(Map<AssetIdentifier, MarketPrice> currentPrices) {
		Money totalValue = calculateTotalValue(currentPrices);

		// Handle case where totalValue is zero to prevent division by zero
		if (totalValue.amount().compareTo(BigDecimal.ZERO) == 0) {
			return new AssetAllocation(totalValue, this.currencyPreference); // Return empty allocation
		}

		AssetAllocation allocation = new AssetAllocation(totalValue, this.currencyPreference);

		for (AssetHolding assetHolding : assetHoldings) {
			MarketPrice marketPrice = currentPrices.get(assetHolding.getAssetIdentifier());

			if (marketPrice != null) {
				Money holdingValueInAssetCurrency = assetHolding.getCurrentValue(marketPrice);

				Money convertedValueInPortfolioCurrency = exchangeRateService.convert(
					holdingValueInAssetCurrency,
					this.currencyPreference
				);

				// Calculate percentage
				// Ensure proper division with BigDecimal and RoundingMode
				BigDecimal percentageAmount = convertedValueInPortfolioCurrency.amount()
					.divide(totalValue.amount(), DecimalPrecision.PERCENTAGE.getDecimalPlaces(), RoundingMode.HALF_UP)
					.multiply(BigDecimal.valueOf(100));

				Percentage percentage = new Percentage(percentageAmount);

				allocation.addAllocation(assetHolding.getAssetIdentifier(), convertedValueInPortfolioCurrency, percentage);
			}
		}
		return allocation;
	}

	public Money calculateTotalLiabilitiesValue() {
		if (this.liabilities == null || this.liabilities.isEmpty()) {
        	return Money.ZERO(this.currencyPreference); // Or portfolio's base currency
    	}
   		Money total = Money.ZERO(this.currencyPreference); // Initialize with portfolio's currency
    	for (Liability liability : this.liabilities) {
        	// Ensure currency consistency or handle conversion if liabilities can be in different currencies
			// For simplicity, assuming all liabilities are already in portfolio's currency for sum
			total = total.add(liability.getCurrentBalance());
		}
    return total;
		
	}

	public Money netWorth(Money totalMarketValue, Money totalLiabilitiesValue) {
		if (!totalMarketValue.currency().equals(this.currencyPreference) || !totalLiabilitiesValue.currency().equals(this.currencyPreference)) {
			throw new IllegalArgumentException("CUrrency mismatch for net worth calculation.");
		}
		return totalMarketValue.subtract(totalLiabilitiesValue);
	}

	public void accruelInterestLiabilities(Instant accrualDate) {
		for (Liability liability : this.liabilities) {
			// Calculate the number of days since last accrual (or loan inception)
			// This is a simplified example; real-world might use more sophisticated day count conventions.
			Instant lastAccrual = liability.getLastInterestAccrualDate() != null ? liability.getLastInterestAccrualDate() : liability.getLastInterestAccrualDate();
			long daysBetween = java.time.Duration.between(lastAccrual, accrualDate).toDays();

			if (daysBetween <= 0) {
				continue; // No days passed, no interest to accrue
			}

			Money currentPrincipalBalance = liability.getCurrentBalance(); // Assume this excludes already accrued interest if principal is separate
			Percentage annualRate = liability.getAnnualInterestRate();

			// Simplified daily interest accrual
			// Interest = Principal * (Annual Rate / 365) * Days
			BigDecimal dailyRate = annualRate.percentageValue().divide(BigDecimal.valueOf(365), DecimalPrecision.PERCENTAGE.getDecimalPlaces(), RoundingMode.HALF_UP);
			BigDecimal interestAmountValue = currentPrincipalBalance.amount().multiply(dailyRate).multiply(BigDecimal.valueOf(daysBetween));
			
			Money accruedInterest = new Money(interestAmountValue, currentPrincipalBalance.currency());

			if (accruedInterest.amount().compareTo(BigDecimal.ZERO) > 0) {
				liability.increaseLiabilityBalance(accruedInterest); // Increase the liability balance

				// Optional: Record an interest accrual transaction for auditing
				// This transaction usually doesn't affect cash directly until payment
				Transaction interestAccrualTransaction = new Transaction(
					UUID.randomUUID(),
					this.portfolioId,
					UUID.randomUUID(), // correlationId
					liability.getLiabilityId(), // Link to the liability
					TransactionType.INTEREST, // New TransactionType
					accruedInterest, // Amount of interest accrued (could be negative if representing expense)
					accrualDate,
					new SimpleTransactionDetails("Interest accrued on liability " + liability.getLiabilityId()), // Or a specific InterestAccrualTransactionDetails
					new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.SYSTEM, "Accrued interest", accrualDate, Instant.now()),
					null, // No direct fees for accrual itself
					false,
					1
				);
				this.transactions.add(interestAccrualTransaction);

				liability.setLastAccrualDate(accrualDate); // Update last accrual date
			}
		}
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

	public void addAssetHolding(AssetHolding holding) {
		Objects.requireNonNull(holding, "holding cannot be null.");
		this.assetHoldings.add(holding);
	}
	public void addLiability(Liability liability) {
		Objects.requireNonNull(liability, "liability cannot be null.");
		this.liabilities.add(liability);
	}
}
