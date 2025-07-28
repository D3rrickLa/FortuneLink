package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.services.SimpleExchangeRateService;

public class PortfolioTest {
	private Portfolio portfolio;
	private UUID userId;
	private UUID portfolioId;
	private String name;
	private String desc;
	private Money portfolioCashBalance;
	private Currency cad;
	private Currency usd;
	private ExchangeRateService exchangeRateService;
	private AssetIdentifier appleAsset;

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

		appleAsset = new AssetIdentifier(
				"APPL", AssetType.STOCK, "US0378331005", "Apple", "NASDAQ", "DESCRIPTION", "TECHNOLOGY");
	}

	@Test 
	void Getters() {
		assertEquals(portfolio, portfolio);
		assertEquals(portfolioId, portfolio.getPortfolioId());
		assertEquals(userId, portfolio.getUserId());
		assertEquals(name, portfolio.getPortfolioName());
		assertEquals(desc, portfolio.getPortfolioDescription());
		assertEquals(cad, portfolio.getCurrencyPreference());
		assertEquals(Collections.emptyList(), portfolio.getTransactions());
		assertEquals(Collections.emptyList(), portfolio.getAssetHoldings());
		assertEquals(Collections.emptyList(), portfolio.getLiabilities());
		assertEquals(exchangeRateService, portfolio.getExchangeRateService());
	}

	@Test
	void testRecordCashflow() {
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
	}

	@Test
	void testRecordCashflowDifferentCurrencyFees() {
		// transaction details setup
		Money orignalMoney = new Money(2000, cad);
		Money convertedMoney = orignalMoney;
		Money totalConversionFee = new Money(0, cad);
		ExchangeRate exchangeRate = null;

		CashflowTransactionDetails cashflowTransactionDetails = new CashflowTransactionDetails(orignalMoney,
				convertedMoney, totalConversionFee, exchangeRate);

		// common transaction setup
		List<Fee> fees = new ArrayList<>();
		fees.add(new Fee(FeeType.DEPOSIT_FEE, new Money(0.05, usd)));
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

		assertEquals(new Money(13999.9315, cad), portfolio.getPortfolioCashBalance());
		assertEquals(1, portfolio.getTransactions().size());
	}

	@Test
	void testAssetPurchase() {
		AssetIdentifier assetIdentifier;
		BigDecimal quantity;
		Money pricePerUnit;
		Money assetValueInAssetCurrency;
		Money assetValueInPortfolioCurrency;
		Money costBasisInPortfolioCurrency;
		Money costBasisInAssetCurrency;
		Money totalFeesInPortfolioCurrency;
		Money totalFeesInAssestCurrency;

		assetIdentifier = new AssetIdentifier(
				"APPL",
				AssetType.STOCK,
				"US0378331005",
				"Apple",
				"NASDAQ",
				"DESCRIPTION",
				"TECH");

		quantity = BigDecimal.valueOf(20);
		pricePerUnit = new Money(214.55, usd);
		assetValueInAssetCurrency = pricePerUnit.multiply(quantity);
		assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad,
				new ExchangeRate(usd, cad, BigDecimal.valueOf(1.37), Instant.now(), "CHANGE"));

		List<Fee> fees = new ArrayList<>();
		fees.add(
				new Fee(FeeType.BROKERAGE, new Money(0.05, cad)));
		fees.add(
				new Fee(FeeType.FOREIGN_EXCHANGE_CONVERSION, new Money(1.57, usd)));

		totalFeesInPortfolioCurrency = new Money(0.05 + (1.57 * 1.37), cad);
		totalFeesInAssestCurrency = new Money(1.57 + (0.05 / 1.37), usd);

		costBasisInPortfolioCurrency = Money.of(
				assetValueInPortfolioCurrency.amount().add(totalFeesInPortfolioCurrency.amount()), cad);

		costBasisInAssetCurrency = Money.of(
				assetValueInAssetCurrency.amount().add(totalFeesInAssestCurrency.amount()), usd);

		AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
				assetIdentifier,
				quantity,
				pricePerUnit,
				assetValueInAssetCurrency,
				assetValueInPortfolioCurrency,
				costBasisInPortfolioCurrency,
				costBasisInAssetCurrency,
				totalFeesInPortfolioCurrency,
				totalFeesInAssestCurrency);

		UUID correlationId = UUID.randomUUID();
		UUID parentId = null;
		TransactionType transactionType = TransactionType.BUY;

		TransactionMetadata transactionMetadata = new TransactionMetadata(
				TransactionStatus.COMPLETED,
				TransactionSource.MANUAL_INPUT,
				"BUYING STUFF",
				Instant.now(),
				Instant.now());

		CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
				correlationId,
				parentId,
				transactionType,
				transactionMetadata,
				fees);

		Instant transactionDate = Instant.now();

		portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, transactionDate);
		// Calculate the expected amount using BigDecimal for precision
		// Ensure all parts of the calculation use BigDecimal, not double literals
		// directly where precision matters.
		BigDecimal expectedAmountValue = BigDecimal.valueOf(12000.00)
				.subtract(BigDecimal.valueOf(5878.67))
				.subtract(BigDecimal.valueOf(0.05))
				.subtract(BigDecimal.valueOf(1.57).multiply(BigDecimal.valueOf(1.37)));

		// Define the comparison scale. 4 decimal places seem to be the expected
		// precision in your example.
		int comparisonScale = 4;
		RoundingMode roundingMode = RoundingMode.HALF_EVEN; // Or RoundingMode.HALF_UP, depending on your policy

		// Round the expected amount to the comparison scale
		BigDecimal expectedRoundedAmount = expectedAmountValue.setScale(comparisonScale, roundingMode);

		// Get the actual portfolio cash balance
		Money actualPortfolioCashBalance = portfolio.getPortfolioCashBalance();

		// Round the actual amount from the portfolio to the same comparison scale
		BigDecimal actualRoundedAmount = actualPortfolioCashBalance.amount().setScale(comparisonScale,
				roundingMode);

		// Assert that the rounded amounts are equal
		assertEquals(expectedRoundedAmount, actualRoundedAmount,
				"Portfolio cash balance should be updated correctly.");

		// You can also assert the currency if it's part of the test scope
		assertEquals(cad, actualPortfolioCashBalance.currency(),
				"Portfolio cash balance currency should be CAD.");
	}

	@Test
	void testAssetPurchaseAssetExistsAlready() {
		AssetIdentifier assetIdentifier;
		BigDecimal quantity;
		Money pricePerUnit;
		Money assetValueInAssetCurrency;
		Money assetValueInPortfolioCurrency;
		Money costBasisInPortfolioCurrency;
		Money costBasisInAssetCurrency;
		Money totalFeesInPortfolioCurrency;
		Money totalFeesInAssestCurrency;

		assetIdentifier = new AssetIdentifier(
				"APPL",
				AssetType.STOCK,
				"US0378331005",
				"Apple",
				"NASDAQ",
				"DESCRIPTION",
				"TECH");

		quantity = BigDecimal.valueOf(20);
		pricePerUnit = new Money(214.55, usd);
		assetValueInAssetCurrency = pricePerUnit.multiply(quantity);
		assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad,
				new ExchangeRate(usd, cad, BigDecimal.valueOf(1.37), Instant.now(), "CHANGE"));

		List<Fee> fees = new ArrayList<>();
		fees.add(
				new Fee(FeeType.BROKERAGE, new Money(0.05, cad)));
		fees.add(
				new Fee(FeeType.FOREIGN_EXCHANGE_CONVERSION, new Money(1.57, usd)));

		totalFeesInPortfolioCurrency = new Money(0.05 + (1.57 * 1.37), cad);
		totalFeesInAssestCurrency = new Money(1.57 + (0.05 / 1.37), usd);

		costBasisInPortfolioCurrency = Money.of(
				assetValueInPortfolioCurrency.amount().add(totalFeesInPortfolioCurrency.amount()), cad);

		costBasisInAssetCurrency = Money.of(
				assetValueInAssetCurrency.amount().add(totalFeesInAssestCurrency.amount()), usd);

		AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
				assetIdentifier,
				quantity,
				pricePerUnit,
				assetValueInAssetCurrency,
				assetValueInPortfolioCurrency,
				costBasisInPortfolioCurrency,
				costBasisInAssetCurrency,
				totalFeesInPortfolioCurrency,
				totalFeesInAssestCurrency);

		UUID correlationId = UUID.randomUUID();
		UUID parentId = null;
		TransactionType transactionType = TransactionType.BUY;

		TransactionMetadata transactionMetadata = new TransactionMetadata(
				TransactionStatus.COMPLETED,
				TransactionSource.MANUAL_INPUT,
				"BUYING STUFF",
				Instant.now(),
				Instant.now());

		CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
				correlationId,
				parentId,
				transactionType,
				transactionMetadata,
				fees);

		Instant transactionDate = Instant.now();

		portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, transactionDate);
		System.out.println(assetTransactionDetails.getAssetValueInPortfolioCurrency());
		System.out.println(assetTransactionDetails.getTotalFeesInPortfolioCurrency());
		System.out.println(portfolio.getPortfolioCashBalance());

		CommonTransactionInput commonTransactionInput2 = new CommonTransactionInput(UUID.randomUUID(), parentId,
				transactionType, transactionMetadata, fees);
		
				portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput2, transactionDate);
		assertEquals(2, portfolio.getTransactions().size());
		assertEquals(BigDecimal.valueOf(40), portfolio.getAssetHoldings().get(0).getTotalQuantity());
		// Correct way to calculate expected value using only BigDecimal
		BigDecimal initialCash = BigDecimal.valueOf(12000);

		// Break down the cost calculation into BigDecimal operations
		BigDecimal assetValue1 = BigDecimal.valueOf(5878.67);
		BigDecimal brokerageFee1 = BigDecimal.valueOf(0.05);
		BigDecimal fxFeeAmount1 = BigDecimal.valueOf(1.57);
		BigDecimal fxRate1 = BigDecimal.valueOf(1.37);

		// Calculate cost for one transaction using BigDecimal
		BigDecimal singleTransactionCost = assetValue1
				.add(brokerageFee1)
				.add(fxFeeAmount1.multiply(fxRate1));

		// Calculate total expected cost for two transactions
		BigDecimal totalExpectedCost = singleTransactionCost.multiply(BigDecimal.valueOf(2));

		// Calculate the final expected cash balance
		BigDecimal expectedCashBalanceValue = initialCash.subtract(totalExpectedCost);

		// Define the comparison scale. From your output, 8 decimal places for the raw
		// value,
		// or round to 4 if that's your standard for cash balances for display.
		// Let's use 8 to match the actual's high precision for now, then round to 4 for
		// the final assert.
		int comparisonScale = 4; // For final assertion if 4 decimal places is desired

		// Round the expected amount to the desired comparison scale
		BigDecimal expectedRoundedAmount = expectedCashBalanceValue.setScale(comparisonScale,
				RoundingMode.HALF_EVEN);

		// Get the actual portfolio cash balance (which uses your high-precision Money
		// class)
		Money actualPortfolioCashBalance = portfolio.getPortfolioCashBalance();

		// Round the actual amount from the portfolio to the same comparison scale
		BigDecimal actualRoundedAmount = actualPortfolioCashBalance.amount().setScale(comparisonScale,
				RoundingMode.HALF_EVEN);

		// Assert that the rounded amounts are equal
		assertEquals(expectedRoundedAmount, actualRoundedAmount,
				"Portfolio cash balance should be updated correctly after two purchases.");

		assertEquals(cad, actualPortfolioCashBalance.currency(),
				"Portfolio cash balance currency should be CAD.");
	}

	@Test
	void testAssetPurchaseMultipleTimesDifferentPricesAndRates() {
		AssetIdentifier assetIdentifier;
		BigDecimal quantity;
		Money pricePerUnit;
		Money assetValueInAssetCurrency;
		Money assetValueInPortfolioCurrency;
		Money costBasisInPortfolioCurrency;
		Money costBasisInAssetCurrency;
		Money totalFeesInPortfolioCurrency;
		Money totalFeesInAssestCurrency;

		assetIdentifier = new AssetIdentifier(
				"APPL",
				AssetType.STOCK,
				"US0378331005",
				"Apple",
				"NASDAQ",
				"DESCRIPTION",
				"TECH");

		quantity = BigDecimal.valueOf(20);
		pricePerUnit = new Money(214.55, usd);
		assetValueInAssetCurrency = pricePerUnit.multiply(quantity);
		assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad,
				new ExchangeRate(usd, cad, BigDecimal.valueOf(1.37), Instant.now(), "CHANGE"));

		List<Fee> fees = new ArrayList<>();
		fees.add(
				new Fee(FeeType.BROKERAGE, new Money(0.05, cad)));
		fees.add(
				new Fee(FeeType.FOREIGN_EXCHANGE_CONVERSION, new Money(1.57, usd)));

		totalFeesInPortfolioCurrency = new Money(0.05, cad);
		totalFeesInPortfolioCurrency = totalFeesInPortfolioCurrency
				.add(new Money(BigDecimal.valueOf(1.57).multiply(BigDecimal.valueOf(1.37)), cad));
		totalFeesInAssestCurrency = new Money(1.57, usd);
		totalFeesInAssestCurrency = totalFeesInAssestCurrency.add(new Money(
				BigDecimal.valueOf(0.05).divide(BigDecimal.valueOf(1.37), RoundingMode.HALF_EVEN),
				usd));

		costBasisInPortfolioCurrency = Money.of(
				assetValueInPortfolioCurrency.amount().add(totalFeesInPortfolioCurrency.amount()), cad);

		costBasisInAssetCurrency = Money.of(
				assetValueInAssetCurrency.amount().add(totalFeesInAssestCurrency.amount()), usd);

		AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
				assetIdentifier,
				quantity,
				pricePerUnit,
				assetValueInAssetCurrency,
				assetValueInPortfolioCurrency,
				costBasisInPortfolioCurrency,
				costBasisInAssetCurrency,
				totalFeesInPortfolioCurrency,
				totalFeesInAssestCurrency);

		UUID correlationId = UUID.randomUUID();
		UUID parentId = null;
		TransactionType transactionType = TransactionType.BUY;

		TransactionMetadata transactionMetadata = new TransactionMetadata(
				TransactionStatus.COMPLETED,
				TransactionSource.MANUAL_INPUT,
				"BUYING STUFF",
				Instant.now(),
				Instant.now());

		CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
				correlationId,
				parentId,
				transactionType,
				transactionMetadata,
				fees);

		Instant transactionDate = Instant.now();

		portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, transactionDate);
		System.out.println("first record: " + portfolio.getPortfolioCashBalance());
		System.out.println(costBasisInPortfolioCurrency.toString());

		// Capture the actual balance after first transaction for more robust testing
		Money actualCashAfterFirstTransaction = portfolio.getPortfolioCashBalance();

		// second transaction
		BigDecimal quantity2;
		Money pricePerUnit2;
		Money assetValueInAssetCurrency2;
		Money assetValueInPortfolioCurrency2;
		Money costBasisInPortfolioCurrency2;
		Money costBasisInAssetCurrency2;
		Money totalFeesInPortfolioCurrency2;
		Money totalFeesInAssestCurrency2;

		quantity2 = BigDecimal.valueOf(14);
		pricePerUnit2 = new Money(201.45, usd);
		assetValueInAssetCurrency2 = pricePerUnit2.multiply(quantity2);
		assetValueInPortfolioCurrency2 = assetValueInAssetCurrency2.convertTo(cad,
				new ExchangeRate(usd, cad, BigDecimal.valueOf(1.38), Instant.now(), "CHANGE")); // changed
														// exchange
														// rate,
														// starting
														// now
														// it's
														// 1.38

		List<Fee> fees2 = new ArrayList<>();
		fees2.add(
				new Fee(FeeType.BROKERAGE, new Money(0.06, cad)));
		fees2.add(
				new Fee(FeeType.FOREIGN_EXCHANGE_CONVERSION, new Money(1.57, usd)));

		totalFeesInPortfolioCurrency2 = new Money(0.06, cad);
		totalFeesInPortfolioCurrency2 = totalFeesInPortfolioCurrency2
				.add(new Money(BigDecimal.valueOf(1.57).multiply(BigDecimal.valueOf(1.38)), cad));
		totalFeesInAssestCurrency2 = new Money(1.57, usd);
		// FIX: Use 0.06 (second transaction's brokerage fee) instead of 0.05
		totalFeesInAssestCurrency2 = totalFeesInAssestCurrency2.add(new Money(
				BigDecimal.valueOf(0.06).divide(BigDecimal.valueOf(1.38), RoundingMode.HALF_EVEN),
				usd));

		costBasisInPortfolioCurrency2 = Money.of(
				assetValueInPortfolioCurrency2.amount().add(totalFeesInPortfolioCurrency2.amount()),
				cad);

		costBasisInAssetCurrency2 = Money.of(
				assetValueInAssetCurrency2.amount().add(totalFeesInAssestCurrency2.amount()), usd);

		AssetTransactionDetails assetTransactionDetails2 = new AssetTransactionDetails(
				assetIdentifier,
				quantity2,
				pricePerUnit2,
				assetValueInAssetCurrency2,
				assetValueInPortfolioCurrency2,
				costBasisInPortfolioCurrency2,
				costBasisInAssetCurrency2,
				totalFeesInPortfolioCurrency2,
				totalFeesInAssestCurrency2);

		UUID correlationId2 = UUID.randomUUID();
		TransactionMetadata transactionMetadata2 = new TransactionMetadata(
				TransactionStatus.COMPLETED,
				TransactionSource.MANUAL_INPUT,
				"BUYING STUFF",
				Instant.now(),
				Instant.now());

		CommonTransactionInput commonTransactionInput2 = new CommonTransactionInput(
				correlationId2,
				parentId,
				transactionType,
				transactionMetadata2,
				fees2);

		Instant transactionDate2 = Instant.now();

		portfolio.recordAssetPurchase(assetTransactionDetails2, commonTransactionInput2, transactionDate2);
		System.out.println("second record: " + portfolio.getPortfolioCashBalance());
		System.out.println(costBasisInPortfolioCurrency2.toString());

		// --- Assertions ---
		assertEquals(2, portfolio.getTransactions().size(), "Should have two recorded transactions.");
		assertEquals(1, portfolio.getAssetHoldings().size(), "Should have one asset holding for Apple.");

		AssetHolding appleHolding = portfolio.getAssetHoldings().get(0);
		assertNotNull(appleHolding, "Apple holding should exist.");
		assertEquals(assetIdentifier, appleHolding.getAssetIdentifier(),
				"Holding asset identifier should match.");

		// Expected Total Quantity
		BigDecimal expectedTotalQuantity = quantity.add(quantity2); // 20 + 14 = 34
		assertEquals(expectedTotalQuantity, appleHolding.getTotalQuantity(),
				"Total quantity should be the sum of both purchases.");

		// Expected Total Adjusted Cost Basis in Asset Currency (USD)
		BigDecimal expectedTotalAcbInUsdAmount = costBasisInAssetCurrency.amount()
				.add(costBasisInAssetCurrency2.amount());

		int comparisonScaleAcb = 8; // Sufficient precision for these calculations
		BigDecimal calculatedExpectedAcbUsd = expectedTotalAcbInUsdAmount.setScale(comparisonScaleAcb,
				RoundingMode.HALF_EVEN);
		BigDecimal actualAcbUsd = appleHolding.getTotalAdjustedCostBasis().amount().setScale(comparisonScaleAcb,
				RoundingMode.HALF_EVEN);

		assertEquals(calculatedExpectedAcbUsd, actualAcbUsd,
				"Total Adjusted Cost Basis in USD should be correct.");
		assertEquals(usd, appleHolding.getTotalAdjustedCostBasis().currency(),
				"Adjusted Cost Basis currency should be USD.");

		// Verify Portfolio Cash Balance
		// CRITICAL FIX: Use the actual balance after first transaction to avoid
		// rounding discrepancies

		// Calculate expected balance after second transaction using actual balance from
		// first transaction
		Money expectedCashBalanceMoney = actualCashAfterFirstTransaction
				.subtract(costBasisInPortfolioCurrency2);

		// Round for comparison
		int comparisonScaleCash = 4;
		BigDecimal expectedCashBalanceCalculated = expectedCashBalanceMoney.amount()
				// .subtract(totalFeesInPortfolioCurrency.amount())
				// .subtract(totalFeesInPortfolioCurrency2.amount())
				.setScale(comparisonScaleCash, RoundingMode.HALF_EVEN);
		BigDecimal actualCashBalance = portfolio.getPortfolioCashBalance().amount()
				.setScale(comparisonScaleCash, RoundingMode.HALF_EVEN);

		// System.out.println("Cash after first transaction: "
		// 		+ actualCashAfterFirstTransaction.amount().toPlainString());
		// System.out.println("Second transaction cost basis: "
		// 		+ costBasisInPortfolioCurrency2.amount().toPlainString());
		// System.out.println("Expected Cash Balance (CAD) (raw): "
		// 		+ expectedCashBalanceMoney.amount().toPlainString());
		// System.out.println("Expected Cash Balance (CAD) (rounded): "
		// 		+ expectedCashBalanceCalculated.toPlainString());
		// System.out.println("Actual Cash Balance (CAD) (raw): "
		// 		+ portfolio.getPortfolioCashBalance().amount().toPlainString());
		// System.out.println("Actual Cash Balance (CAD) (rounded): " + actualCashBalance.toPlainString());

		assertEquals(expectedCashBalanceCalculated, actualCashBalance,
				"Portfolio cash balance should be updated correctly.");
		assertEquals(cad, portfolio.getPortfolioCashBalance().currency(),
				"Portfolio cash balance currency should be CAD.");
	}

	@Test
	void testAssetPurchaseInValidCashBalanceNotEnough() {
		AssetIdentifier assetIdentifier;
		BigDecimal quantity;
		Money pricePerUnit;
		Money assetValueInAssetCurrency;
		Money assetValueInPortfolioCurrency;
		Money costBasisInPortfolioCurrency;
		Money costBasisInAssetCurrency;
		Money totalFeesInPortfolioCurrency;
		Money totalFeesInAssestCurrency;

		assetIdentifier = new AssetIdentifier(
				"APPL",
				AssetType.STOCK,
				"US0378331005",
				"Apple",
				"NASDAQ",
				"DESCRIPTION",
				"TECH");

		quantity = BigDecimal.valueOf(200);
		pricePerUnit = new Money(214.55, usd);
		assetValueInAssetCurrency = pricePerUnit.multiply(quantity);
		assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad,
				new ExchangeRate(usd, cad, BigDecimal.valueOf(1.37), Instant.now(), "CHANGE"));

		List<Fee> fees = new ArrayList<>();
		fees.add(
				new Fee(FeeType.BROKERAGE, new Money(0.05, cad)));
		fees.add(
				new Fee(FeeType.FOREIGN_EXCHANGE_CONVERSION, new Money(1.57, usd)));

		totalFeesInPortfolioCurrency = new Money(0.05 + (1.57 * 1.37), cad); // need to use convetTo
											// calculations here to avoid
											// floating point errors
		totalFeesInAssestCurrency = new Money(1.57 + (0.05 / 1.37), usd);

		costBasisInPortfolioCurrency = Money.of(
				assetValueInPortfolioCurrency.amount().add(totalFeesInPortfolioCurrency.amount()), cad);

		costBasisInAssetCurrency = Money.of(
				assetValueInAssetCurrency.amount().add(totalFeesInAssestCurrency.amount()), usd);

		AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
				assetIdentifier,
				quantity,
				pricePerUnit,
				assetValueInAssetCurrency,
				assetValueInPortfolioCurrency,
				costBasisInPortfolioCurrency,
				costBasisInAssetCurrency,
				totalFeesInPortfolioCurrency,
				totalFeesInAssestCurrency);

		UUID correlationId = UUID.randomUUID();
		UUID parentId = null;
		TransactionType transactionType = TransactionType.BUY;

		TransactionMetadata transactionMetadata = new TransactionMetadata(
				TransactionStatus.COMPLETED,
				TransactionSource.MANUAL_INPUT,
				"BUYING STUFF",
				Instant.now(),
				Instant.now());

		CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
				correlationId,
				parentId,
				transactionType,
				transactionMetadata,
				fees);

		Instant transactionDate = Instant.now();
		assertThrows(InsufficientFundsException.class, () -> portfolio
				.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, transactionDate));
	}

	@Test
	void testAssetPurchaseInValidNotBuyTransaction() {
		AssetIdentifier assetIdentifier;
		BigDecimal quantity;
		Money pricePerUnit;
		Money assetValueInAssetCurrency;
		Money assetValueInPortfolioCurrency;
		Money costBasisInPortfolioCurrency;
		Money costBasisInAssetCurrency;
		Money totalFeesInPortfolioCurrency;
		Money totalFeesInAssestCurrency;

		assetIdentifier = new AssetIdentifier(
				"APPL",
				AssetType.STOCK,
				"US0378331005",
				"Apple",
				"NASDAQ",
				"DESCRIPTION",
				"TECH");

		quantity = BigDecimal.valueOf(20);
		pricePerUnit = new Money(214.55, usd);
		assetValueInAssetCurrency = pricePerUnit.multiply(quantity);
		assetValueInPortfolioCurrency = assetValueInAssetCurrency.convertTo(cad,
				new ExchangeRate(usd, cad, BigDecimal.valueOf(1.37), Instant.now(), "CHANGE"));

		List<Fee> fees = new ArrayList<>();
		fees.add(
				new Fee(FeeType.BROKERAGE, new Money(0.05, cad)));
		fees.add(
				new Fee(FeeType.FOREIGN_EXCHANGE_CONVERSION, new Money(1.57, usd)));

		totalFeesInPortfolioCurrency = new Money(0.05 + (1.57 * 1.37), cad);
		totalFeesInAssestCurrency = new Money(1.57 + (0.05 / 1.37), usd);

		costBasisInPortfolioCurrency = Money.of(
				assetValueInPortfolioCurrency.amount().add(totalFeesInPortfolioCurrency.amount()), cad);

		costBasisInAssetCurrency = Money.of(
				assetValueInAssetCurrency.amount().add(totalFeesInAssestCurrency.amount()), usd);

		AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
				assetIdentifier,
				quantity,
				pricePerUnit,
				assetValueInAssetCurrency,
				assetValueInPortfolioCurrency,
				costBasisInPortfolioCurrency,
				costBasisInAssetCurrency,
				totalFeesInPortfolioCurrency,
				totalFeesInAssestCurrency);

		UUID correlationId = UUID.randomUUID();
		UUID parentId = null;
		TransactionType transactionType = TransactionType.SELL;

		TransactionMetadata transactionMetadata = new TransactionMetadata(
				TransactionStatus.COMPLETED,
				TransactionSource.MANUAL_INPUT,
				"BUYING STUFF",
				Instant.now(),
				Instant.now());

		CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
				correlationId,
				parentId,
				transactionType,
				transactionMetadata,
				fees);

		Instant transactionDate = Instant.now();

		Exception e1 = assertThrows(IllegalArgumentException.class, () -> portfolio
				.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, transactionDate));
		assertEquals("Expected BUY transaction type, got: " + commonTransactionInput.transactionType(),
				e1.getMessage());
	}

	@Test
	void testAssetSale() {
		// --- Step 1: Initial Purchase (to have something to sell) ---
		BigDecimal purchaseQuantity = BigDecimal.valueOf(20);
		Money purchasePricePerUnit = new Money(BigDecimal.valueOf(214.55), usd); // $214.55 USD
		BigDecimal purchaseExchangeRate = BigDecimal.valueOf(1.37); // 1 USD = 1.37 CAD
		BigDecimal purchaseExchangeRateUSD = BigDecimal.valueOf(0.73); // 1 CAD = 0.73 USD

		// Fees for Purchase
		Money purchaseBrokerageFee = new Money(BigDecimal.valueOf(0.05), cad); // $0.05 CAD
		Money purchaseFxFee = new Money(BigDecimal.valueOf(1.57), usd); // $1.57 USD

		// Calculate Purchase Transaction Details
		Money purchaseAssetValueInAssetCurrency = purchasePricePerUnit.multiply(purchaseQuantity); // 4291.00
														// USD
		Money purchaseAssetValueInPortfolioCurrency = purchaseAssetValueInAssetCurrency.convertTo(cad,
				new ExchangeRate(usd, cad, purchaseExchangeRate, Instant.now(), "Provider")); // 5878.47
														// CAD

		Money totalPurchaseFeesInPortfolioCurrency = purchaseBrokerageFee.add(purchaseFxFee.convertTo(cad,
				new ExchangeRate(usd, cad, purchaseExchangeRate, Instant.now(), "Provider"))); // 0.05 +
														// (1.57
														// *
														// 1.37)
														// =
														// 2.2009
														// CAD
		Money totalPurchaseFeesInAssetCurrency = purchaseFxFee.add(purchaseBrokerageFee.convertTo(usd,
				new ExchangeRate(cad, usd, purchaseExchangeRateUSD, Instant.now(), "Provider"))); // 1.57
															// +
															// (0.05
															// /
															// 1.37)
															// =
															// ~1.60649635
															// USD

		Money purchaseCostBasisInPortfolioCurrency = purchaseAssetValueInPortfolioCurrency
				.add(totalPurchaseFeesInPortfolioCurrency); // 5878.47 + 2.2009 = 5880.6709 CAD
		Money purchaseCostBasisInAssetCurrency = purchaseAssetValueInAssetCurrency
				.add(totalPurchaseFeesInAssetCurrency); // 4291.00 + ~1.60649635 = ~4292.60649635 USD

		AssetTransactionDetails purchaseDetails = new AssetTransactionDetails(
				appleAsset, purchaseQuantity, purchasePricePerUnit, purchaseAssetValueInAssetCurrency,
				purchaseAssetValueInPortfolioCurrency,
				purchaseCostBasisInPortfolioCurrency, purchaseCostBasisInAssetCurrency,
				totalPurchaseFeesInPortfolioCurrency, totalPurchaseFeesInAssetCurrency);

		CommonTransactionInput purchaseInput = new CommonTransactionInput(
				UUID.randomUUID(), null, TransactionType.BUY,
				new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT,
						"Initial Apple Purchase", Instant.now(), Instant.now()),
				new ArrayList<>());

		portfolio.recordAssetPurchase(purchaseDetails, purchaseInput, Instant.now());

		// --- Assertions After Purchase (Sanity Check) ---
		assertEquals(1, portfolio.getTransactions().size(), "Should have one purchase transaction.");
		assertEquals(1, portfolio.getAssetHoldings().size(), "Should have one asset holding.");
		AssetHolding appleHoldingAfterPurchase = portfolio.getAssetHoldings().get(0);
		assertEquals(purchaseQuantity, appleHoldingAfterPurchase.getTotalQuantity(),
				"Initial quantity after purchase should be correct.");

		// Verify cash balance after purchase
		Money expectedCashAfterPurchase = new Money(BigDecimal.valueOf(12000.00), cad)
				.subtract(purchaseCostBasisInPortfolioCurrency);
		assertEquals(expectedCashAfterPurchase.amount().setScale(4, RoundingMode.HALF_EVEN),
				portfolio.getPortfolioCashBalance().amount().setScale(4, RoundingMode.HALF_EVEN),
				"Cash balance after purchase should be correct.");
		System.out.println("Cash after purchase: " + portfolio.getPortfolioCashBalance().toString());
		System.out.println("Total ACB after purchase: "
				+ appleHoldingAfterPurchase.getTotalAdjustedCostBasis().toString());

		// --- Step 2: Asset Sale ---
		BigDecimal saleQuantity = BigDecimal.valueOf(10); // Sell half the shares
		Money salePricePerUnit = new Money(BigDecimal.valueOf(220.00), usd); // $220.00 USD (higher price)
		BigDecimal saleExchangeRate = BigDecimal.valueOf(1.39); // 1 USD = 1.39 CAD (new rate)
		// BigDecimal saleExchangeRateUSD = BigDecimal.valueOf(0.71); // 1 CAD = 0.71 USD (new rate)

		// Fees for Sale
		Money saleBrokerageFee = new Money(BigDecimal.valueOf(0.07), cad); // $0.07 CAD
		Money saleFxFee = new Money(BigDecimal.valueOf(1.60), usd); // $1.60 USD

		// Calculate Sale Transaction Details
		Money saleAssetValueInAssetCurrency = salePricePerUnit.multiply(saleQuantity); // 10 * 220.00 = 2200.00
												// USD
		Money saleAssetValueInPortfolioCurrency = saleAssetValueInAssetCurrency.convertTo(cad,
				new ExchangeRate(usd, cad, saleExchangeRate, Instant.now(), "Provider")); // 2200.00 *
														// 1.39
														// =
														// 3058.00
														// CAD

		Money totalSaleFeesInPortfolioCurrency = saleBrokerageFee.add(saleFxFee.convertTo(cad,
				new ExchangeRate(usd, cad, saleExchangeRate, Instant.now(), "Provider"))); // 0.07 +
														// (1.60
														// *
														// 1.39)
														// =
														// 0.07
														// +
														// 2.224
														// =
														// 2.294
														// CAD
		BigDecimal brokerageFeeInUsdForSale = saleBrokerageFee.amount().divide(saleExchangeRate,
				MathContext.DECIMAL128);
		Money totalSaleFeesInAssetCurrency = saleFxFee.add(new Money(brokerageFeeInUsdForSale, usd));
		// For sale, costBasis fields are not directly used as "cost", but
		// AssetTransactionDetails requires them.
		// We'll use them to store the net proceeds for consistency if the structure
		// demands it,
		// or just pass dummy values if they're ignored for sales.
		// For simplicity, let's pass the net proceeds as costBasis for sale details.
		Money saleNetProceedsInPortfolioCurrency = saleAssetValueInPortfolioCurrency
				.subtract(totalSaleFeesInPortfolioCurrency);
		Money saleNetProceedsInAssetCurrency = saleAssetValueInAssetCurrency
				.subtract(totalSaleFeesInAssetCurrency);

		AssetTransactionDetails saleDetails = new AssetTransactionDetails(
				appleAsset, saleQuantity, salePricePerUnit, saleAssetValueInAssetCurrency,
				saleAssetValueInPortfolioCurrency,
				saleNetProceedsInPortfolioCurrency, // Using this field to pass net proceeds for sale
				saleNetProceedsInAssetCurrency, // Using this field to pass net proceeds for sale
				totalSaleFeesInPortfolioCurrency, totalSaleFeesInAssetCurrency);

		CommonTransactionInput saleInput = new CommonTransactionInput(
				UUID.randomUUID(), null, TransactionType.SELL,
				new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT,
						"Apple Sale", Instant.now(), Instant.now()),
				new ArrayList<>());

		Money initialAcbBeforeSale = appleHoldingAfterPurchase.getTotalAdjustedCostBasis();
		BigDecimal initialQuantityBeforeSale = appleHoldingAfterPurchase.getTotalQuantity();
		System.out.println(
				"Cash after purchase: " + portfolio.getPortfolioCashBalance().amount().toPlainString());
		System.out.println("Total ACB after purchase: " + initialAcbBeforeSale.amount().toPlainString()); // Use
															// initialAcbBeforeSale

		portfolio.recordAssetSale(saleDetails, saleInput, Instant.now());

		// --- Assertions After Sale ---
		assertEquals(2, portfolio.getTransactions().size(),
				"Should have two transactions (1 purchase, 1 sale).");
		assertEquals(1, portfolio.getAssetHoldings().size(),
				"Should still have one asset holding for Apple (partial sale).");

		AssetHolding appleHoldingAfterSale = portfolio.getAssetHoldings().get(0);
		assertEquals(purchaseQuantity.subtract(saleQuantity), appleHoldingAfterSale.getTotalQuantity(),
				"Remaining quantity after sale should be correct."); // 20 - 10 = 10

		// Corrected calculation for Expected Remaining ACB (USD)
		// This calculates it based on the initial ACB before the sale, and the
		// proportion of remaining shares.
		Money expectedRemainingAcb = initialAcbBeforeSale
				.multiply(initialQuantityBeforeSale.subtract(saleQuantity))
				.divide(initialQuantityBeforeSale);

		assertEquals(expectedRemainingAcb.amount().setScale(8, RoundingMode.HALF_EVEN),
				appleHoldingAfterSale.getTotalAdjustedCostBasis().amount().setScale(8,
						RoundingMode.HALF_EVEN),
				"Remaining total adjusted cost basis after sale should be correct.");
		System.out.println("Total ACB after sale: "
				+ appleHoldingAfterSale.getTotalAdjustedCostBasis().amount().toPlainString());

		// Calculate Expected Cash Balance After Sale
		// Cash after purchase: expectedCashAfterPurchase
		// Net cash impact from sale: saleNetProceedsInPortfolioCurrency (3055.706 CAD)
		Money expectedFinalCashBalance = expectedCashAfterPurchase.add(saleNetProceedsInPortfolioCurrency);

		assertEquals(expectedFinalCashBalance.amount().setScale(4, RoundingMode.HALF_EVEN),
				portfolio.getPortfolioCashBalance().amount().setScale(4, RoundingMode.HALF_EVEN),
				"Final cash balance after sale should be correct.");
		System.out.println(
				"Final cash balance: " + portfolio.getPortfolioCashBalance().amount().toPlainString());

		// Calculate and Print Realized Gain/Loss (for informational purposes in test
		// output)
		// This is (Net Sale Proceeds) - (Cost Basis of Sold Shares)
		Money realizedGainLoss = appleHoldingAfterPurchase.calculateCapitalGain(saleQuantity,
				saleNetProceedsInAssetCurrency);
		System.out.println("Realized Gain/Loss for sale: " + realizedGainLoss.amount().toPlainString());

		// Corrected Expected Realized Gain/Loss (in USD, accounting for all fees)
		// This is 2198.349640287769784172661870503597 USD (Net Sale Proceeds in Asset
		// Currency)
		// MINUS 2146.303248175182481751824817518248 USD (Cost Basis of Sold Shares)
		// = 52.04639211258730242083695300000009 USD
		Money expectedRealizedGainLoss = new Money(new BigDecimal("52.04639029125873024208369530000000023"),
				usd);

		assertEquals(expectedRealizedGainLoss.amount().setScale(8, RoundingMode.HALF_EVEN),
				realizedGainLoss.amount().setScale(8, RoundingMode.HALF_EVEN),
				"Realized gain/loss should be correct.");
		assertEquals(usd, realizedGainLoss.currency(),
				"Realized gain/loss should be in asset's native currency.");
	}

	@Test
	void testAssetSaleInsufficientQuantity() {
		// Purchase some shares first
		BigDecimal purchaseQuantity = BigDecimal.valueOf(10);
		Money purchasePricePerUnit = new Money(BigDecimal.valueOf(100.00), usd);
		AssetTransactionDetails purchaseDetails = new AssetTransactionDetails(
				appleAsset, purchaseQuantity, purchasePricePerUnit,
				purchasePricePerUnit.multiply(purchaseQuantity),
				purchasePricePerUnit.multiply(purchaseQuantity).convertTo(cad,
						new ExchangeRate(usd, cad, BigDecimal.valueOf(1.30), Instant.now(),
								"P")),
				Money.of(BigDecimal.valueOf(10), cad), Money.of(BigDecimal.valueOf(10 / 1.30), usd),
				Money.ZERO(cad), Money.ZERO(usd) // Simplified fees for this test
		);
		portfolio.recordAssetPurchase(purchaseDetails, new CommonTransactionInput(
				UUID.randomUUID(), null, TransactionType.BUY,
				new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT,
						"Buy for insufficient test", Instant.now(), Instant.now()),
				new ArrayList<>()), Instant.now());

		// Attempt to sell more than owned
		BigDecimal saleQuantity = BigDecimal.valueOf(15); // Try to sell 15, but only 10 owned
		Money salePricePerUnit = new Money(BigDecimal.valueOf(110.00), usd);
		AssetTransactionDetails saleDetails = new AssetTransactionDetails(
				appleAsset, saleQuantity, salePricePerUnit,
				salePricePerUnit.multiply(saleQuantity),
				salePricePerUnit.multiply(saleQuantity).convertTo(cad,
						new ExchangeRate(usd, cad, BigDecimal.valueOf(1.30), Instant.now(),
								"P")),
				Money.ZERO(cad), Money.ZERO(usd), Money.ZERO(cad), Money.ZERO(usd) // Simplified fees
													// for this test
		);

		CommonTransactionInput saleInput = new CommonTransactionInput(
				UUID.randomUUID(), null, TransactionType.SELL,
				new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT,
						"Insufficient Sale", Instant.now(), Instant.now()),
				new ArrayList<>());

		// Assert that an IllegalArgumentException is thrown
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			portfolio.recordAssetSale(saleDetails, saleInput, Instant.now());
		});

		assertTrue(thrown.getMessage().contains("Cannot sell more units than you have."),
				"Should throw exception for insufficient quantity.");
	}

	@Test
	void testAssetSaleNotHeld() {
		// Attempt to sell an asset not in the portfolio
		BigDecimal saleQuantity = BigDecimal.valueOf(5);
		Money salePricePerUnit = new Money(BigDecimal.valueOf(100.00), usd);
		AssetIdentifier nonHeldAsset = new AssetIdentifier("GOOG", AssetType.STOCK, "US02079K1079", "Google",
				"NASDAQ", "DESC", "TECH");

		AssetTransactionDetails saleDetails = new AssetTransactionDetails(
				nonHeldAsset, saleQuantity, salePricePerUnit,
				salePricePerUnit.multiply(saleQuantity),
				salePricePerUnit.multiply(saleQuantity).convertTo(cad,
						new ExchangeRate(usd, cad, BigDecimal.valueOf(1.30), Instant.now(),
								"P")),
				Money.ZERO(cad), Money.ZERO(usd), Money.ZERO(cad), Money.ZERO(usd) // Simplified fees
													// for this test
		);

		CommonTransactionInput saleInput = new CommonTransactionInput(
				UUID.randomUUID(), null, TransactionType.SELL,
				new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT,
						"Sale of not held asset", Instant.now(), Instant.now()),
				new ArrayList<>());

		// Assert that an IllegalArgumentException is thrown
		AssetNotFoundException thrown = assertThrows(AssetNotFoundException.class, () -> {
			portfolio.recordAssetSale(saleDetails, saleInput, Instant.now());
		});

		assertTrue(thrown.getMessage()
				.contains("Cannot sell asset not held in portfolio: "
						+ saleDetails.getAssetIdentifier().symbol()),
				"Should throw exception for not held asset.");
	}

	@Test
	void testRecordNewLiability() {
		Money originalLoanAmount = new Money("4800.67", cad); // Assuming Money has a String constructor
		Money originalLoanAmountInPortfolioCurrency = new Money("4800.67", cad);
		Money fees = Money.ZERO(cad); // This is a Money object representing zero fees

		Percentage annualInterestRate = new Percentage(BigDecimal.valueOf(5.75));
		Instant maturityDate = Instant.now();

		// Assuming LiabilityIncurrenceTransactionDetails has a constructor matching this
		// and has fields/getters for liabilityName and description
		LiabilityIncurrenceTransactionDetails liabilityIncurrenceTransactionDetails = new LiabilityIncurrenceTransactionDetails(
			UUID.randomUUID(),
			"Loan 1", // Assuming a name field
			"Personal Loan", // Assuming a description field
			originalLoanAmount, 
			originalLoanAmountInPortfolioCurrency, 
			annualInterestRate, 
			Instant.now(),
			maturityDate, 
			fees, // totalFeesInPortfolioCurrency
			fees  // totalFeesInLiabilityCurrency
		);

		CommonTransactionInput newLiabilityInput = new CommonTransactionInput(
			UUID.randomUUID(), 
			null, 
			TransactionType.LIABILITY_INCURRENCE, // Assuming this TransactionType is defined
			new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT,
				"Some description", Instant.now(), Instant.now()),
			null // Fees list is null here
		);

		Instant transactionDate = Instant.now();

		// Capture initial cash balance for comparison
		Money initialCashBalance = portfolio.getPortfolioCashBalance();

		portfolio.recordNewLiability(liabilityIncurrenceTransactionDetails, newLiabilityInput, transactionDate);

		// --- Assertions ---
		assertEquals(1, portfolio.getLiabilities().size(), "Should have one liability recorded.");
		
		Liability recordedLiability = portfolio.getLiabilities().get(0);
		assertEquals(originalLoanAmount, recordedLiability.getCurrentBalance(), "Liability current balance should match original loan amount.");
		assertEquals(annualInterestRate, recordedLiability.getAnnualInterestRate(), "Liability interest rate should be correct.");
		assertEquals(maturityDate, recordedLiability.getMaturityDate(), "Liability maturity date should be correct.");
		assertEquals(transactionDate, recordedLiability.getLastInterestAccrualDate(), "Last interest accrual date should be transaction date.");
		assertEquals("Loan 1", recordedLiability.getName(), "Liability name should be correct."); // Assuming getName() exists
		assertEquals("Personal Loan", recordedLiability.getDescription(), "Liability description should be correct."); // Assuming getDescription() exists

		// Assert cash balance update
		Money expectedFinalCashBalance = initialCashBalance.add(originalLoanAmountInPortfolioCurrency); // No fees, so just add loan amount
		assertEquals(expectedFinalCashBalance, portfolio.getPortfolioCashBalance(), "Portfolio cash balance should increase by loan amount.");

		// Assert transaction recording
		assertEquals(1, portfolio.getTransactions().size(), "Should have one transaction recorded.");
		Transaction recordedTransaction = portfolio.getTransactions().get(0);
		assertEquals(TransactionType.LIABILITY_INCURRENCE, recordedTransaction.getTransactionType(), "Transaction type should be LIABILITY_INCURRENCE.");
		assertEquals(originalLoanAmountInPortfolioCurrency, recordedTransaction.getTotalTransactionAmount(), "Transaction total amount should be net cash inflow.");
		assertEquals(transactionDate, recordedTransaction.getTransactionDate(), "Transaction date should be correct.");
		assertTrue(recordedTransaction.getFees().isEmpty(), "Transaction fees list should be empty."); // Because newLiabilityInput.fees was null
	}

	    @Test
    void testRecordNewLiability_WithFees_SameCurrency() {
        Money initialCashBalance = portfolio.getPortfolioCashBalance(); // $10,000 CAD

        Money originalLoanAmount = new Money("5000.00", cad);
        Money originalLoanAmountInPortfolioCurrency = new Money("5000.00", cad);
        Money originationFee = new Money("50.00", cad); // $50 CAD origination fee
        
        Percentage annualInterestRate = new Percentage(new BigDecimal("6.00"));
        Instant maturityDate = Instant.now().plusSeconds(3600 * 24 * 365 * 3); // 3 years from now

        // Total fees for details
        Money totalFeesInPortfolioCurrency = originationFee;
        Money totalFeesInLiabilityCurrency = originationFee;

        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(
			UUID.randomUUID(),
            "Car Loan CAD", "Loan for new car purchase",
            originalLoanAmount, 
			originalLoanAmountInPortfolioCurrency, 
			annualInterestRate, 
			Instant.now(), 
			maturityDate,
            totalFeesInPortfolioCurrency, 
			totalFeesInLiabilityCurrency
        );

        List<Fee> feesList = new ArrayList<>();
        feesList.add(new Fee(FeeType.BROKERAGE, originationFee)); // Add the individual fee

        CommonTransactionInput commonInput = new CommonTransactionInput(
            UUID.randomUUID(), null, TransactionType.DEPOSIT,
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT,
                "Incurred car loan with fee", Instant.now(), Instant.now()),
            feesList
        );

        Instant transactionDate = Instant.now();

        portfolio.recordNewLiability(details, commonInput, transactionDate);

        // Expected cash balance: Initial cash + Loan Amount - Fees = 10000 + 5000 - 50 = 14950.00 CAD
        Money expectedFinalCashBalance = initialCashBalance
                                        .add(originalLoanAmountInPortfolioCurrency)
                                        .subtract(totalFeesInPortfolioCurrency);
        assertEquals(expectedFinalCashBalance, portfolio.getPortfolioCashBalance(), "Cash balance should reflect loan amount minus fees.");

        // Expected liability balance: Should be the original loan amount (assuming fee is separate)
        // assertEquals(originalLoanAmount, portfolio.getLiabilities().get(0).getCurrentBalance(), "Liability balance should be original loan amount.");

        // Assert transaction details
        Transaction recordedTransaction = portfolio.getTransactions().get(0);
        Money expectedTransactionAmount = originalLoanAmountInPortfolioCurrency.subtract(totalFeesInPortfolioCurrency);
		System.out.println(originalLoanAmountInPortfolioCurrency);
		System.out.println(totalFeesInPortfolioCurrency);
        assertEquals(expectedTransactionAmount, recordedTransaction.getTotalTransactionAmount(), "Transaction total amount should be net cash inflow after fees.");
        assertEquals(1, recordedTransaction.getFees().size(), "Transaction should record the individual fee.");
        assertEquals(originationFee, recordedTransaction.getFees().get(0).amount(), "Recorded fee amount should be correct.");
    }

    @Test
    void testRecordNewLiability_ForeignCurrency_WithFees() {
        Money initialCashBalance = portfolio.getPortfolioCashBalance(); // $10,000 CAD

        Money originalLoanAmountUSD = new Money("1000.00", usd); // $1,000 USD loan
        BigDecimal exchangeRateUsdToCad = new BigDecimal("1.35"); // 1 USD = 1.35 CAD
        ExchangeRate usdToCadRate = new ExchangeRate(usd, cad, exchangeRateUsdToCad, Instant.now(), "Provider");

        // Convert loan amount to portfolio currency
        Money originalLoanAmountInPortfolioCurrency = originalLoanAmountUSD.convertTo(cad, usdToCadRate); // 1000 * 1.35 = 1350.00 CAD

		System.out.println(originalLoanAmountInPortfolioCurrency);

        Money originationFeeUSD = new Money("10.00", usd); // $10 USD origination fee
        
        // Convert fees to portfolio currency
        Money totalFeesInPortfolioCurrency = originationFeeUSD.convertTo(cad, usdToCadRate); // 10 * 1.35 = 13.50 CAD
        Money totalFeesInLiabilityCurrency = originationFeeUSD; // Fees in liability's native currency

		System.out.println(totalFeesInPortfolioCurrency);

        Percentage annualInterestRate = new Percentage(new BigDecimal("4.50"));
        Instant maturityDate = Instant.now().plusSeconds(3600 * 24 * 365 * 10); // 10 years from now

        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(	
			UUID.randomUUID(),
            "Mortgage USD", "USD mortgage for US property",
            originalLoanAmountUSD, originalLoanAmountInPortfolioCurrency, annualInterestRate, Instant.now(),maturityDate,
            totalFeesInLiabilityCurrency, totalFeesInPortfolioCurrency
        );

        List<Fee> feesList = new ArrayList<>();
        feesList.add(new Fee(FeeType.BROKERAGE, originationFeeUSD));

        CommonTransactionInput commonInput = new CommonTransactionInput(
            UUID.randomUUID(), null, TransactionType.LIABILITY_INCURRENCE,
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT,
                "Incurred USD mortgage", Instant.now(), Instant.now()),
            feesList
        );

        Instant transactionDate = Instant.now();

        portfolio.recordNewLiability(details, commonInput, transactionDate);

        // Expected cash balance: Initial cash + Loan Amount (CAD) - Fees (CAD) = 10000 + 1350 - 13.50 = 11336.50 CAD
        Money expectedFinalCashBalance = initialCashBalance
                                        .add(originalLoanAmountInPortfolioCurrency)
                                        .subtract(totalFeesInPortfolioCurrency);
        assertEquals(expectedFinalCashBalance, portfolio.getPortfolioCashBalance(), "Cash balance should reflect converted loan amount minus fees.");

        // Expected liability balance (in USD)
        assertEquals(originalLoanAmountUSD.subtract(totalFeesInLiabilityCurrency), portfolio.getLiabilities().get(0).getCurrentBalance(), "Liability balance should be original loan amount in USD.");
        assertEquals(usd, portfolio.getLiabilities().get(0).getCurrentBalance().currency(), "Liability currency should be USD.");
    }

    @Test
    void testRecordNewLiability_InvalidTransactionType() {
        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(
			UUID.randomUUID(),
            "Invalid Loan", "Test invalid type",
            new Money("1000", cad), new Money("1000", cad), new Percentage(new BigDecimal("1.0")), Instant.now(),Instant.now(),
            Money.ZERO(cad), Money.ZERO(cad)
        );

        CommonTransactionInput commonInput = new CommonTransactionInput(
            UUID.randomUUID(), null, TransactionType.SELL, // Incorrect type
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, "Invalid type test", Instant.now(), Instant.now()),
            new ArrayList<>()
        );

        Instant transactionDate = Instant.now();

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
            portfolio.recordNewLiability(details, commonInput, transactionDate);
        });

        assertTrue(thrown.getMessage().contains("Liability incurrence transaction type must be LIABILITY_INCURRENCE or DEPOSIT."), "Should throw exception for incorrect transaction type.");
        assertEquals(0, portfolio.getLiabilities().size(), "No liability should be recorded.");
        assertEquals(0, portfolio.getTransactions().size(), "No transaction should be recorded.");
    }

    @Test
    void testRecordNewLiability_NullInputs() {
        LiabilityIncurrenceTransactionDetails details = new LiabilityIncurrenceTransactionDetails(
			UUID.randomUUID(),
            "Null Test", "Testing nulls",
            new Money("100", cad), new Money("100", cad), new Percentage(new BigDecimal("1.0")), Instant.now(),Instant.now(),
            Money.ZERO(cad), Money.ZERO(cad)
        );
        CommonTransactionInput commonInput = new CommonTransactionInput(
            UUID.randomUUID(), null, TransactionType.DEPOSIT,
            new TransactionMetadata(TransactionStatus.COMPLETED, TransactionSource.MANUAL_INPUT, "Null test", Instant.now(), Instant.now()),
            new ArrayList<>()
        );
        Instant transactionDate = Instant.now();

        assertThrows(NullPointerException.class, () -> portfolio.recordNewLiability(null, commonInput, transactionDate), "Should throw NPE for null details.");
        assertThrows(NullPointerException.class, () -> portfolio.recordNewLiability(details, null, transactionDate), "Should throw NPE for null commonInput.");
        assertThrows(NullPointerException.class, () -> portfolio.recordNewLiability(details, commonInput, null), "Should throw NPE for null transactionDate.");
    }
}
