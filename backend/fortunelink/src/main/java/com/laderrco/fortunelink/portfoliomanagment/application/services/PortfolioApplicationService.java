package com.laderrco.fortunelink.portfoliomanagment.application.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfoliomanagment.application.dtos.AssetAllocationDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.MoneyDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.PortfolioDetailsDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.TransactionDto;
import com.laderrco.fortunelink.portfoliomanagment.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Liability;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Transaction;
import com.laderrco.fortunelink.portfoliomanagment.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.PriceService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.MarketPrice;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.PaymentAllocationResult;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.SimpleTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TransactionDetails;

import lombok.AllArgsConstructor;
// Write opetaions (commands) -> UUID or String
// read, use DTOS
@Service
@AllArgsConstructor
public class PortfolioApplicationService {
    private final PortfolioRepository portfolioRepository;
    private final ExchangeRateService exchangeRateService;
    private final PriceService pricingService;

    private Portfolio getPortfolio(UUID portfolioId) {
        return portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new PortfolioNotFoundException("Cannot find portfolio with id: " + portfolioId));
    }   

    @Transactional
    public UUID createPortfolio(
        UUID userId,
        String name,
        String description,
        Money initialCashBalance,
        Currency currencyPreference
    ) {

        Portfolio portfolio = new Portfolio (
            UUID.randomUUID(),
            userId,
            name,
            description,
            initialCashBalance,
            currencyPreference,
            exchangeRateService
        );

        portfolioRepository.save(portfolio);

        return portfolio.getPortfolioId();
    }

    @Transactional
    public UUID recordAssetAcquisition(
        UUID portfolioId,
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money purchasePrice, // price per unit in asset's native currency
        Instant transactionDate,
        List<Fee> fees,
        TransactionMetadata transactionMetadata
    ) {

        Portfolio portfolio = getPortfolio(portfolioId);
        Money purchasePriceInPortfolioCurrency = exchangeRateService.convert(purchasePrice, portfolio.getCurrencyPreference());

        // fees can be a mixture
        Money totalFeesInPortfolioCurrency = fees.stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, portfolio.getCurrencyPreference()))
            .reduce(Money.ZERO(portfolio.getCurrencyPreference()), Money::add);
            
            
            Money totalFeesInAssetCurrency = fees.stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, purchasePrice.currency()))
            .reduce(Money.ZERO(purchasePrice.currency()), Money::add);


        Money assetValueInAssetCurrency = purchasePrice.multiply(quantity);
        Money assetValueInPortfolioCurrency = purchasePriceInPortfolioCurrency.multiply(quantity);

        Money costBasisInAssetCurrency = assetValueInAssetCurrency.add(totalFeesInAssetCurrency);
        Money costBasisInPortfolioCurrency = assetValueInPortfolioCurrency.add(totalFeesInPortfolioCurrency);


        // --- Construct Domain Detail Objects ---
        // These constructors would be defined in your Domain Layer
        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
            assetIdentifier,
            quantity,
            purchasePrice,
            assetValueInAssetCurrency,
            assetValueInPortfolioCurrency,
            costBasisInPortfolioCurrency,
            costBasisInAssetCurrency,
            totalFeesInPortfolioCurrency,
            totalFeesInAssetCurrency
        );


        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), // parent Transaction Id
            UUID.randomUUID(), // correlation Transaction Id
            TransactionType.BUY,
            transactionMetadata,
            fees // list of fees
        );

        // --- Delegate to Domain Aggregate ---
        portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, transactionDate);

        portfolioRepository.save(portfolio);
        return portfolioId; // As per your example, returning portfolioId

    }

    @Transactional
    public UUID recordAssetDisposal(
        UUID portfolioId,
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money salePrice, // in native currency
        Instant transactionDate,
        List<Fee> fees,
        TransactionMetadata transactionMetadata
    ) {
        Portfolio portfolio = getPortfolio(portfolioId);

        Money salePriceInPortfolioCurrency = exchangeRateService.convert(salePrice, portfolio.getCurrencyPreference());
       
        // --- Calculate total fees in portfolio's currency ---
        Money totalFeesInPortfolioCurrency = fees.stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, portfolio.getCurrencyPreference()))
            .reduce(Money.ZERO(portfolio.getCurrencyPreference()), Money::add);

        // --- Calculate total fees in the asset's original currency ---
        Money totalFeesInAssetCurrency = fees.stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, salePrice.currency())) // Convert to asset's original currency
            .reduce(Money.ZERO(salePrice.currency()), Money::add);

        // --- Calculate all derived values for AssetTransactionDetails (for disposal) ---
        Money assetValueInAssetCurrency = salePrice.multiply(quantity);
        Money assetValueInPortfolioCurrency = salePriceInPortfolioCurrency.multiply(quantity);

        // For disposal, cost basis related values might represent net proceeds or similar,
        // depending on your domain's exact definition for disposal 'costBasis' fields in AssetTransactionDetails.
        // I'll assume it's sale value minus fees here as a common interpretation for "cost basis" on a sale.
        Money costBasisInAssetCurrency = assetValueInAssetCurrency.subtract(totalFeesInAssetCurrency);
        Money costBasisInPortfolioCurrency = assetValueInPortfolioCurrency.subtract(totalFeesInPortfolioCurrency);

        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
            assetIdentifier,
            quantity,
            salePrice, // pricePerUnit (original salePrice)
            assetValueInAssetCurrency,
            assetValueInPortfolioCurrency,
            costBasisInPortfolioCurrency,
            costBasisInAssetCurrency,
            totalFeesInPortfolioCurrency,
            totalFeesInAssetCurrency
        );

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TransactionType.SELL,
            transactionMetadata,
            fees
        );

        portfolio.recordAssetSale(assetTransactionDetails, commonTransactionInput, transactionDate);

        portfolioRepository.save(portfolio);
        return portfolioId;

    }

    @Transactional
    public UUID recordCashDeposit(
        UUID portfolioId,
        Money amount, 
        Instant transactionDate,
        TransactionMetadata metadata
    ) {
        Portfolio portfolio = getPortfolio(portfolioId);

        Money depositAmountInPortoflioCurrency = exchangeRateService.convert(amount, portfolio.getCurrencyPreference());

        ExchangeRate exchangeRate = new ExchangeRate(
            amount.currency(), 
            portfolio.getCurrencyPreference(), 
            exchangeRateService.getExchangeRate(amount.currency(),portfolio.getCurrencyPreference()), 
            transactionDate, 
            "SYSTEM"
        );

        CashflowTransactionDetails cashflowTransactionDetails = new CashflowTransactionDetails(
            amount, depositAmountInPortoflioCurrency, Money.ZERO(amount.currency()), exchangeRate)
        ;
        
        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), 
            UUID.randomUUID(), TransactionType.DEPOSIT, 
            metadata, 
            null
        );
        portfolio.recordCashflow(cashflowTransactionDetails, commonTransactionInput, transactionDate);

        portfolioRepository.save(portfolio);
        return portfolioId;
    }

    @Transactional
    public UUID recordCashWithdrawal(
        UUID portfolioId,
        Money amount, 
        Instant transactionDate,
        TransactionMetadata metadata
    ) {
        Portfolio portfolio = getPortfolio(portfolioId);

        Money depositAmountInPortoflioCurrency = exchangeRateService.convert(amount, portfolio.getCurrencyPreference());

        ExchangeRate exchangeRate = new ExchangeRate(
            amount.currency(), 
            portfolio.getCurrencyPreference(), 
            exchangeRateService.getExchangeRate(amount.currency(),portfolio.getCurrencyPreference()), 
            transactionDate, 
            "SYSTEM"
        );

        CashflowTransactionDetails cashflowTransactionDetails = new CashflowTransactionDetails(
            amount, depositAmountInPortoflioCurrency, Money.ZERO(amount.currency()), exchangeRate)
        ;
        
        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), 
            UUID.randomUUID(), TransactionType.WITHDRAWAL, 
            metadata, 
            null
        );
        portfolio.recordCashflow(cashflowTransactionDetails, commonTransactionInput, transactionDate);

        portfolioRepository.save(portfolio);
        return portfolioId;
    }

    @Transactional
    public UUID recordNewLiability(
        UUID portfolioId,
        Money originalLoanAmount, // Original amount of the loan/liability (in its native currency)
        Percentage annualInterestRate,
        Instant incurrenceDate,
        Instant maturityDate,
        String description,
        List<Fee> fees,
        TransactionMetadata transactionMetadata
    ) {
        Portfolio portfolio = getPortfolio(portfolioId);
        Money loanAmountInPortfolioCurrency = exchangeRateService.convert(originalLoanAmount, portfolio.getCurrencyPreference());

      // --- Calculate total fees in portfolio's currency ---
        Money totalFeesInPortfolioCurrency = fees.stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, portfolio.getCurrencyPreference()))
            .reduce(Money.ZERO(portfolio.getCurrencyPreference()), Money::add);

        // --- Calculate total fees in the asset's original currency ---
        Money totalFeesInAssetCurrency = fees.stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, originalLoanAmount.currency())) // Convert to asset's original currency
            .reduce(Money.ZERO(originalLoanAmount.currency()), Money::add);


        LiabilityIncurrenceTransactionDetails liabilityIncurrenceTransactionDetails = new LiabilityIncurrenceTransactionDetails(
            UUID.randomUUID(), 
            "NAME", 
            description, 
            originalLoanAmount, 
            loanAmountInPortfolioCurrency, 
            annualInterestRate, 
            incurrenceDate,
            maturityDate, 
            totalFeesInAssetCurrency, 
            totalFeesInPortfolioCurrency
        );

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), UUID.randomUUID(), TransactionType.LIABILITY_INCURRENCE, transactionMetadata, fees);

        Liability newLiability = portfolio.recordNewLiability(liabilityIncurrenceTransactionDetails, commonTransactionInput, incurrenceDate);

        portfolioRepository.save(portfolio);
        return newLiability.getLiabilityId();

    }

    @Transactional
    public UUID recordLiabilityPayment(
        UUID portfolioId, 
        UUID liabilityId,
        Money totalPaymentAmountInLiabilityCurrency,
        Instant paymentDate,
        List<Fee> fees,
        TransactionMetadata transactionMetadata
    ) {
        Portfolio portfolio = getPortfolio(portfolioId);
        
        Liability selectedLiability = portfolio.getLiabilities().stream()
            .filter(l -> l.getLiabilityId().equals(liabilityId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Liability ID " + liabilityId + " does not exist in this portfolio."));


        // making payment to liability
        PaymentAllocationResult paymentResult = selectedLiability.applyPayment(totalPaymentAmountInLiabilityCurrency, paymentDate);

        
       // calculate total cash outflow from the portfolio's prespective
        Money totalFeesInLiabilityCurrency = fees.stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, totalPaymentAmountInLiabilityCurrency.currency())) // Convert to asset's original currency
            .reduce(Money.ZERO(totalPaymentAmountInLiabilityCurrency.currency()), Money::add);
            
        Money totalFeesInPortfolioCurrency = fees.stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, portfolio.getCurrencyPreference()))
            .reduce(Money.ZERO(portfolio.getCurrencyPreference()), Money::add);
            
        
        Money totalCashOutflowInLiabilityCurrency = totalPaymentAmountInLiabilityCurrency.add(totalFeesInLiabilityCurrency);
        Money totalCashOutflowInPortfolioCurrency = exchangeRateService.convert(totalCashOutflowInLiabilityCurrency, portfolio.getCurrencyPreference());

        Money interestAmountInPortfolioCurrency = exchangeRateService.convert(paymentResult.interestPaid(), portfolio.getCurrencyPreference());

        LiabilityPaymentTransactionDetails liabilityPaymentTransactionDetails = new LiabilityPaymentTransactionDetails(
            selectedLiability.getLiabilityId(),
            totalPaymentAmountInLiabilityCurrency,
            paymentResult.interestPaid(),              // Interest portion in liability's currency (from domain)
            totalFeesInLiabilityCurrency,
            totalCashOutflowInPortfolioCurrency,
            interestAmountInPortfolioCurrency,         // Interest portion in portfolio's currency (converted)
            totalFeesInPortfolioCurrency
        );
        
        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TransactionType.PAYMENT,
            transactionMetadata,
            fees
        );
        
        portfolio.recordLiabilityPayment(
            liabilityPaymentTransactionDetails,
            commonTransactionInput,
            paymentDate
        );

        portfolioRepository.save(portfolio);

        return portfolioId;
    }

    @Transactional(readOnly = true)
    public PortfolioDetailsDto getPortfolioDetails(
        UUID portfolioId
    ) {
        Portfolio portfolio = getPortfolio(portfolioId);
        Set<AssetIdentifier> uniqueAssetsInPortfolio = portfolio.getAssetHoldings().stream()
            .map(ah -> ah.getAssetIdentifier())
            .collect(Collectors.toSet());
        
        Map<AssetIdentifier, MarketPrice> currentPrices = pricingService.getCurrentPrices(uniqueAssetsInPortfolio);
            
        Money totalMarketValue = portfolio.calculateTotalValue(currentPrices);
        Money unrealizedGains = portfolio.calculateUnrealizedGains(currentPrices);
        Money totalLiabilitiesValue = portfolio.calculateTotalLiabilitiesValue();
        Money netWorth = portfolio.netWorth(totalMarketValue, totalLiabilitiesValue);

        
        
        List<PortfolioDetailsDto.AssetHoldingDto> assetHoldingDtos = portfolio.getAssetHoldings().stream()
        .map(ah -> {
                MarketPrice assetMarketPrice = currentPrices.getOrDefault(ah.getAssetIdentifier(), MarketPrice.ZERO(ah.getAssetIdentifier(), ah.getAverageACBPerUnit().currency()));

                // Calculate unrealized gain/loss here, as it's not directly on AssetHolding
                Money currentHoldingValue = ah.getCurrentValue(assetMarketPrice);
                Money unrealizedGainLossCalculated = currentHoldingValue.subtract(ah.getTotalAdjustedCostBasis());
            
                return new PortfolioDetailsDto.AssetHoldingDto(
                    ah.getAssetId(),
                    ah.getAssetIdentifier().symbol(),
                    ah.getAssetIdentifier().assetCommonName(),
                    ah.getAssetIdentifier().assetType().name(),
                    ah.getTotalQuantity(),
                    ah.getTotalAdjustedCostBasis(),
                    assetMarketPrice.price(), // The actual price value
                    currentHoldingValue,
                    unrealizedGainLossCalculated 
                );
            })
            .collect(Collectors.toList());

        List<PortfolioDetailsDto.LiabilityDto> liabilityDtos = portfolio.getLiabilities().stream()
            .map(liab -> new PortfolioDetailsDto.LiabilityDto(
                liab.getLiabilityId(),
                liab.getDescription(),
                liab.getOriginalAmount(), // Using getTotalTransactionAmount as per your update
                liab.getCurrentBalance(),
                liab.getAnnualInterestRate(),
                liab.getIncurrenceDate(),
                liab.getMaturityDate(),
                liab.getCurrentBalance().isZero() // Assuming isPaidOff means currentBalance is zero
            ))
            .collect(Collectors.toList());

        
        List<PortfolioDetailsDto.TransactionDto> recentTransactionDtos = portfolio.getTransactions().stream()
            .limit(10)
            .map(tx -> new PortfolioDetailsDto.TransactionDto(
                tx.getTransactionId(),
                tx.getTransactionType(),
                tx.getTransactionDate(),
                tx.getTransactionMetadata().description(),
                tx.getTotalTransactionAmount(),
                tx.getTransactionDetails().toString() // Can be null for cash transactions
            ))
            .collect(Collectors.toList());

             // Construct and return the main PortfolioDetailsDto
        return new PortfolioDetailsDto(
            portfolio.getPortfolioId(),
            portfolio.getPortfolioName(),
            portfolio.getPortfolioDescription(),
            portfolio.getPortfolioCashBalance(),
            totalMarketValue,
            unrealizedGains,
            totalLiabilitiesValue,
            netWorth,
            assetHoldingDtos,
            liabilityDtos,
            recentTransactionDtos
        );
    } 

    @Transactional(readOnly = true)
    public AssetAllocationDto getAssetAllocation(
        UUID portoflioId
    ) {
        Portfolio portfolio = getPortfolio(portoflioId);
        
        Set<AssetIdentifier> uniqueAssetsInPortfolio = portfolio.getAssetHoldings().stream()
            .map(ah -> ah.getAssetIdentifier())
            .collect(Collectors.toSet());

        Map<AssetIdentifier, MarketPrice> currentPrices = pricingService.getCurrentPrices(uniqueAssetsInPortfolio);

        Map<String, Money> allocationByType = portfolio.getAssetAllocationByType(currentPrices);
        Map<String, Money> allocationBySector = portfolio.getAssetAllocationBySector(currentPrices);

        Money totalAllocatedValue = portfolio.calculateTotalValue(currentPrices);


        // Map allocation data to DTO segments
        List<AssetAllocationDto.AllocationSegment> byAssetTypeSegments = allocationByType.entrySet().stream()
            .map(entry -> new AssetAllocationDto.AllocationSegment(
                entry.getKey(),
                entry.getValue(),
                totalAllocatedValue.equals(Money.ZERO(totalAllocatedValue.currency())) ? Percentage.of(0) : entry.getValue().percentageOf(totalAllocatedValue)
            ))
            .collect(Collectors.toList());

        List<AssetAllocationDto.AllocationSegment> byIndustrySectorSegments = allocationBySector.entrySet().stream()
             .map(entry -> new AssetAllocationDto.AllocationSegment(
                entry.getKey(),
                entry.getValue(),
                totalAllocatedValue.equals(Money.ZERO(totalAllocatedValue.currency())) ? Percentage.of(0) : entry.getValue().percentageOf(totalAllocatedValue)
            ))
            .collect(Collectors.toList());

        // Construct and return the AssetAllocationDto
        return new AssetAllocationDto(
            portfolio.getPortfolioId(),
            byAssetTypeSegments,
            byIndustrySectorSegments,
            totalAllocatedValue
        );
    }

    @Transactional(readOnly = true)
    public MoneyDto getTotalPortfolioValue(
        UUID portfolioId
    ) {
        Portfolio portfolio = getPortfolio(portfolioId);

        Set<AssetIdentifier> uniqueAssetsInPortfolio = portfolio.getAssetHoldings().stream()
            .map(ah -> ah.getAssetIdentifier())
            .collect(Collectors.toSet());
        Map<AssetIdentifier, MarketPrice> currentPrices = pricingService.getCurrentPrices(uniqueAssetsInPortfolio);

        Money totalValue = portfolio.calculateTotalValue(currentPrices);
        return new MoneyDto(totalValue.amount(), totalValue.currency().getCurrencyCode());
    }

    @Transactional(readOnly = true)
    public MoneyDto getUnrealizedGains(
        UUID portfolioId
    ) {
        Portfolio portfolio = getPortfolio(portfolioId);

        // Get current market prices to calculate unrealized gains
        Set<AssetIdentifier> uniqueAssetsInPortfolio = portfolio.getAssetHoldings().stream()
            .map(ah -> ah.getAssetIdentifier())
            .collect(Collectors.toSet());
        Map<AssetIdentifier, MarketPrice> currentPrices = pricingService.getCurrentPrices(uniqueAssetsInPortfolio);

        // Calculate unrealized gains from the domain model
        Money unrealizedGains = portfolio.calculateUnrealizedGains(currentPrices);
        return new MoneyDto(unrealizedGains.amount(), unrealizedGains.currency().getCurrencyCode());
    }
    
    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionHistory(UUID portfolioId, Instant startDate, Instant endDate) {
        
        Portfolio portfolio = getPortfolio(portfolioId);


        return portfolio.getTransactions().stream()
            .filter(tx -> !tx.getTransactionDate().isBefore(startDate) && !tx.getTransactionDate().isAfter(endDate))
            .sorted(Comparator.comparing(Transaction::getTransactionDate))
            .map(tx -> {
                String description;
                BigDecimal primaryAmount; // will be the DTO's amount field
                String relatedEntityName = null;
                UUID relatedEntityId = null;

                TransactionDetails details = tx.getTransactionDetails();
                if (details instanceof AssetTransactionDetails purchaseDetails && tx.getTransactionType() == TransactionType.BUY) {
                    primaryAmount = purchaseDetails.totalPurchasePrice().amount();
                    description = String.format("Bought %s shares of %s for %s %s",
                        purchaseDetails.getQuantity().stripTrailingZeros().toPlainString(),
                        purchaseDetails.assetSymbol(),
                        purchaseDetails.getAssetValueInAssetCurrency().amount().stripTrailingZeros().toPlainString(),
                        purchaseDetails.getAssetValueInAssetCurrency().currency().getCurrencyCode()
                    );
                    relatedEntityName = purchaseDetails.assetSymbol();
                    relatedEntityId = purchaseDetails.assetHoldingId();
                } else if (details instanceof AssetTransactionDetails saleDetails && tx.getTransactionType() == TransactionType.SELL) {
                    primaryAmount = saleDetails.totalSaleProceeds().amount();
                    description = String.format("Sold %s shares of %s for %s %s",
                        saleDetails.getQuantity().stripTrailingZeros().toPlainString(),
                        saleDetails.assetSymbol(),
                        saleDetails.totalSaleProceeds().amount().stripTrailingZeros().toPlainString(),
                        saleDetails.totalSaleProceeds().currency().getCode()
                    );
                    relatedEntityName = saleDetails.assetSymbol();
                    relatedEntityId = saleDetails.assetHoldingId();
                } else if (details instanceof CashflowTransactionDetails depositDetails && tx.getTransactionType() == TransactionType.DEPOSIT) {
                    primaryAmount = depositDetails.depositAmount().amount();
                    description = String.format("Cash Deposit: %s %s",
                        depositDetails.depositAmount().amount().stripTrailingZeros().toPlainString(),
                        depositDetails.depositAmount().currency().getCode()
                    );
                    // No related entity ID for simple cash transactions
                } else if (details instanceof CashflowTransactionDetails withdrawalDetails && tx.getTransactionType() == TransactionType.WITHDRAWAL) {
                    primaryAmount = withdrawalDetails.withdrawalAmount().amount();
                    description = String.format("Cash Withdrawal: %s %s",
                        withdrawalDetails.withdrawalAmount().amount().stripTrailingZeros().toPlainString(),
                        withdrawalDetails.withdrawalAmount().currency().getCode()
                    );
                    // No related entity ID for simple cash transactions
                } else if (details instanceof LiabilityPaymentTransactionDetails paymentDetails && tx.getTransactionType() == TransactionType.PAYMENT) {
                    primaryAmount = paymentDetails.totalPaymentAmountInLiabilityCurrency().amount();
                    // You might need to fetch Liability name here or pass it into details when created
                    description = String.format("Liability Payment: %s %s (Interest: %s)",
                        paymentDetails.totalPaymentAmountInLiabilityCurrency().amount().stripTrailingZeros().toPlainString(),
                        paymentDetails.totalPaymentAmountInLiabilityCurrency().currency().getCode(),
                        paymentDetails.interestAmountInLiabilityCurrency().amount().stripTrailingZeros().toPlainString()
                    );
                    relatedEntityId = paymentDetails.liabilityId();
                    // Placeholder for name: You might need portfolio.getLiability(paymentDetails.liabilityId()).getName()
                    // or ensure the name is stored in details if Liability isn't always available from portfolio
                    relatedEntityName = "Liability " + paymentDetails.liabilityId().toString().substring(0, 8) + "..."; // Fallback
                } else if (details instanceof LiabilityIncurrenceTransactionDetails incurrenceDetails && tx.getTransactionType() == TransactionType.LIABILITY_INCURRENCE) {
                    primaryAmount = incurrenceDetails.originalLiabilityAmount().amount();
                    description = String.format("New Loan Incurred: %s %s",
                        incurrenceDetails.originalLiabilityAmount().amount().stripTrailingZeros().toPlainString(),
                        incurrenceDetails.originalLiabilityAmount().currency().getCode()
                    );
                    relatedEntityId = incurrenceDetails.liabilityId();
                    relatedEntityName = "Loan " + incurrenceDetails.liabilityId().toString().substring(0, 8) + "..."; // Fallback
                } else if (details instanceof InterestAccrualTransactionDetails interestDetails && tx.getTransactionType() == TransactionType.INTEREST) {
                    primaryAmount = interestDetails.accruedInterestAmount().amount();
                    description = String.format("Interest Accrual: %s %s on Liability",
                        interestDetails.accruedInterestAmount().amount().stripTrailingZeros().toPlainString(),
                        interestDetails.accruedInterestAmount().currency().getCode()
                    );
                    relatedEntityId = interestDetails.liabilityId();
                    relatedEntityName = "Liability " + interestDetails.liabilityId().toString().substring(0, 8) + "..."; // Fallback
                }
                else if (details instanceof SimpleTransactionDetails simpleDetails) {
                    primaryAmount = tx.getTotalTransactionAmount().amount(); // Fallback to total amount
                    description = simpleDetails.getDescription();
                }
                else {
                    // Fallback for unhandled transaction types
                    primaryAmount = tx.getTotalTransactionAmount().amount();
                    description = "Unhandled Transaction Type: " + tx.getTransactionType().name();
                }
                BigDecimal cashImpact = tx.getTotalTransactionAmount().amount();

                return new TransactionDto(
                    tx.getTransactionId(),
                    tx.getTransactionDate(),
                    tx.getTransactionType().name(), // Use enum name for String representation
                    description,
                    tx.getTotalTransactionAmount().currency().getCurrencyCode(), 
                    primaryAmount,
                    cashImpact,
                    relatedEntityName,
                    relatedEntityId
                );
                
            })
            .collect(Collectors.toList());
    }

    
}