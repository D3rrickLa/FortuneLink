package com.laderrco.fortunelink.portfoliomanagment.application.services;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordAssetDisposalCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordAssetPurchaseCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordCashDeposit;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordCashWidthdrawalCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordLiabilityPaymentCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordNewLiabilityCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.AssetAllocationDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.MoneyDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.PortfolioDetailsDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.TransactionDto;
import com.laderrco.fortunelink.portfoliomanagment.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.application.queries.GetAssetAllocationQuery;
import com.laderrco.fortunelink.portfoliomanagment.application.queries.GetPortfolioDetailsQuery;
import com.laderrco.fortunelink.portfoliomanagment.application.queries.GetTotalPortfolioValueQuery;
import com.laderrco.fortunelink.portfoliomanagment.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfoliomanagment.application.queries.GetUnrealizedGainsQuery;
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
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.InterestExpenseDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.InterestIncomeDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.SimpleTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TransactionDetails;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
// Write opetaions (commands) -> UUID or String
// read, use DTOS
// we should handle any domain errors that might be thrown in here with a generic 'Application Error'
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
        @Nonnull RecordAssetPurchaseCommand command
    ) {

        Portfolio portfolio = getPortfolio(command.portfolioId());
        Money purchasePriceInPortfolioCurrency = exchangeRateService.convert(command.purchasePrice(), portfolio.getCurrencyPreference());

        // fees can be a mixture
        Money totalFeesInPortfolioCurrency = command.fees().stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, portfolio.getCurrencyPreference()))
            .reduce(Money.ZERO(portfolio.getCurrencyPreference()), Money::add);
            
            
            Money totalFeesInAssetCurrency = command.fees().stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, command.purchasePrice().currency()))
            .reduce(Money.ZERO(command.purchasePrice().currency()), Money::add);


        Money assetValueInAssetCurrency = command.purchasePrice().multiply(command.quantity());
        Money assetValueInPortfolioCurrency = purchasePriceInPortfolioCurrency.multiply(command.quantity());

        Money costBasisInAssetCurrency = assetValueInAssetCurrency.add(totalFeesInAssetCurrency);
        Money costBasisInPortfolioCurrency = assetValueInPortfolioCurrency.add(totalFeesInPortfolioCurrency);


        // --- Construct Domain Detail Objects ---
        // These constructors would be defined in your Domain Layer
        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
            command.assetIdentifier(),
            command.quantity(),
            command.purchasePrice(),
            assetValueInAssetCurrency,
            assetValueInPortfolioCurrency,
            costBasisInPortfolioCurrency,
            costBasisInAssetCurrency,
            totalFeesInPortfolioCurrency,
            totalFeesInAssetCurrency,
            null
        );


        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), // parent Transaction Id
            UUID.randomUUID(), // correlation Transaction Id
            TransactionType.BUY,
            command.transactionMetadata(),
            command.fees() // list of fees
        );

        // --- Delegate to Domain Aggregate ---
        portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, command.transactionDate());

        portfolioRepository.save(portfolio);
        return command.portfolioId(); // As per your example, returning portfolioId

    }

    @Transactional
    public UUID recordAssetDisposal(
        @Nonnull RecordAssetDisposalCommand command
    ) {
        Portfolio portfolio = getPortfolio(command.portfolioId());

        Money salePriceInPortfolioCurrency = exchangeRateService.convert(command.salePrice(), portfolio.getCurrencyPreference());
       
        // --- Calculate total fees in portfolio's currency ---
        Money totalFeesInPortfolioCurrency = command.fees().stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, portfolio.getCurrencyPreference()))
            .reduce(Money.ZERO(portfolio.getCurrencyPreference()), Money::add);

        // --- Calculate total fees in the asset's original currency ---
        Money totalFeesInAssetCurrency = command.fees().stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, command.salePrice().currency())) // Convert to asset's original currency
            .reduce(Money.ZERO(command.salePrice().currency()), Money::add);

        // --- Calculate all derived values for AssetTransactionDetails (for disposal) ---
        Money assetValueInAssetCurrency = command.salePrice().multiply(command.quantity());
        Money assetValueInPortfolioCurrency = salePriceInPortfolioCurrency.multiply(command.quantity());

        // For disposal, cost basis related values might represent net proceeds or similar,
        // depending on your domain's exact definition for disposal 'costBasis' fields in AssetTransactionDetails.
        // I'll assume it's sale value minus fees here as a common interpretation for "cost basis" on a sale.
        Money costBasisInAssetCurrency = assetValueInAssetCurrency.subtract(totalFeesInAssetCurrency);
        Money costBasisInPortfolioCurrency = assetValueInPortfolioCurrency.subtract(totalFeesInPortfolioCurrency);

        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
            command.assetIdentifier(),
            command.quantity(),
            command.salePrice(), // pricePerUnit (original salePrice)
            assetValueInAssetCurrency,
            assetValueInPortfolioCurrency,
            costBasisInPortfolioCurrency,
            costBasisInAssetCurrency,
            totalFeesInPortfolioCurrency,
            totalFeesInAssetCurrency,
            null
        );

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(),
            UUID.randomUUID(),
            TransactionType.SELL,
            command.transactionMetadata(),
            command.fees()
        );

        portfolio.recordAssetSale(assetTransactionDetails, commonTransactionInput, command.transactionDate());

        portfolioRepository.save(portfolio);
        return command.portfolioId();

    }

    @Transactional
    public UUID recordCashDeposit(
        @Nonnull RecordCashDeposit command
    ) {
        Portfolio portfolio = getPortfolio(command.portfolioId());

        Money depositAmountInPortoflioCurrency = exchangeRateService.convert(command.amount(), portfolio.getCurrencyPreference());

        ExchangeRate exchangeRate = new ExchangeRate(
            command.amount().currency(), 
            portfolio.getCurrencyPreference(), 
            exchangeRateService.getExchangeRate(command.amount().currency(),portfolio.getCurrencyPreference()), 
            command.transactionDate(), 
            "SYSTEM"
        );

        CashflowTransactionDetails cashflowTransactionDetails = new CashflowTransactionDetails(
            command.amount(), depositAmountInPortoflioCurrency, Money.ZERO(command.amount().currency()), exchangeRate)
        ;
        
        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), 
            UUID.randomUUID(), TransactionType.DEPOSIT, 
            command.metadata(), 
            null
        );
        portfolio.recordCashflow(cashflowTransactionDetails, commonTransactionInput, command.transactionDate());

        portfolioRepository.save(portfolio);
        return command.portfolioId();
    }

    @Transactional
    public UUID recordCashWithdrawal(
        @Nonnull RecordCashWidthdrawalCommand command
    ) {
        Portfolio portfolio = getPortfolio(command.portfolioId());

        Money depositAmountInPortoflioCurrency = exchangeRateService.convert(command.amount(), portfolio.getCurrencyPreference());

        ExchangeRate exchangeRate = new ExchangeRate(
            command.amount().currency(), 
            portfolio.getCurrencyPreference(), 
            exchangeRateService.getExchangeRate(command.amount().currency(),portfolio.getCurrencyPreference()), 
            command.transactionDate(), 
            "SYSTEM"
        );

        CashflowTransactionDetails cashflowTransactionDetails = new CashflowTransactionDetails(
            command.amount(), depositAmountInPortoflioCurrency, Money.ZERO(command.amount().currency()), exchangeRate)
        ;
        
        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), 
            UUID.randomUUID(), TransactionType.WITHDRAWAL, 
            command.metadata(), 
            null
        );
        portfolio.recordCashflow(cashflowTransactionDetails, commonTransactionInput, command.transactionDate());

        portfolioRepository.save(portfolio);
        return command.portfolioId();
    }

    @Transactional
    public UUID recordNewLiability(
        @Nonnull RecordNewLiabilityCommand command
    ) {
        Portfolio portfolio = getPortfolio(command.portfolioId());
        Money loanAmountInPortfolioCurrency = exchangeRateService.convert(command.originalLoanAmount(), portfolio.getCurrencyPreference());

      // --- Calculate total fees in portfolio's currency ---
        Money totalFeesInPortfolioCurrency = command.fees().stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, portfolio.getCurrencyPreference()))
            .reduce(Money.ZERO(portfolio.getCurrencyPreference()), Money::add);

        // --- Calculate total fees in the asset's original currency ---
        Money totalFeesInAssetCurrency = command.fees().stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, command.originalLoanAmount().currency())) // Convert to asset's original currency
            .reduce(Money.ZERO(command.originalLoanAmount().currency()), Money::add);


        LiabilityIncurrenceTransactionDetails liabilityIncurrenceTransactionDetails = new LiabilityIncurrenceTransactionDetails(
            UUID.randomUUID(), 
            "NAME", 
            command.description(), 
            command.originalLoanAmount(), 
            loanAmountInPortfolioCurrency, 
            command.annualInterestRate(), 
            command.incurrenceDate(),
            command.maturityDate(), 
            totalFeesInAssetCurrency, 
            totalFeesInPortfolioCurrency
        );

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), UUID.randomUUID(), TransactionType.LIABILITY_INCURRENCE, command.transactionMetadata(), command.fees());

        Liability newLiability = portfolio.recordNewLiability(liabilityIncurrenceTransactionDetails, commonTransactionInput, command.incurrenceDate());

        portfolioRepository.save(portfolio);
        return newLiability.getLiabilityId();

    }

    @Transactional
    public UUID recordLiabilityPayment(
        @Nonnull RecordLiabilityPaymentCommand command
    ) {
        Portfolio portfolio = getPortfolio(command.portfolioId());
        
        Liability selectedLiability = portfolio.getLiabilities().stream()
            .filter(l -> l.getLiabilityId().equals(command.liabilityId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Liability ID " + command.liabilityId() + " does not exist in this portfolio."));


        // making payment to liability
        PaymentAllocationResult paymentResult = selectedLiability.applyPayment(command.totalPaymentAmountInLiabilityCurrency(), command.paymentDate());

        
       // calculate total cash outflow from the portfolio's prespective
        Money totalFeesInLiabilityCurrency = command.fees().stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, command.totalPaymentAmountInLiabilityCurrency().currency())) // Convert to asset's original currency
            .reduce(Money.ZERO(command.totalPaymentAmountInLiabilityCurrency().currency()), Money::add);
            
        Money totalFeesInPortfolioCurrency = command.fees().stream()
            .map(Fee::amount)
            .map(feeAmount -> exchangeRateService.convert(feeAmount, portfolio.getCurrencyPreference()))
            .reduce(Money.ZERO(portfolio.getCurrencyPreference()), Money::add);
            
        
        Money totalCashOutflowInLiabilityCurrency = command.totalPaymentAmountInLiabilityCurrency().add(totalFeesInLiabilityCurrency);
        Money totalCashOutflowInPortfolioCurrency = exchangeRateService.convert(totalCashOutflowInLiabilityCurrency, portfolio.getCurrencyPreference());

        Money interestAmountInPortfolioCurrency = exchangeRateService.convert(paymentResult.interestPaid(), portfolio.getCurrencyPreference());

        LiabilityPaymentTransactionDetails liabilityPaymentTransactionDetails = new LiabilityPaymentTransactionDetails(
            selectedLiability.getLiabilityId(),
            command.totalPaymentAmountInLiabilityCurrency(),
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
            command.transactionMetadata(),
            command.fees()
        );
        
        portfolio.recordLiabilityPayment(
            liabilityPaymentTransactionDetails,
            commonTransactionInput,
            command.paymentDate()
        );

        portfolioRepository.save(portfolio);

        return command.portfolioId();
    }

    @Transactional(readOnly = true)
    public PortfolioDetailsDto getPortfolioDetails(
        @Nonnull GetPortfolioDetailsQuery query
    ) {
        Portfolio portfolio = getPortfolio(query.portfolioId());
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
        @Nonnull GetAssetAllocationQuery query
    ) {
        Portfolio portfolio = getPortfolio(query.portfolioId());
        
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
        @Nonnull GetTotalPortfolioValueQuery query
    ) {
        Portfolio portfolio = getPortfolio(query.portfolioId());

        Set<AssetIdentifier> uniqueAssetsInPortfolio = portfolio.getAssetHoldings().stream()
            .map(ah -> ah.getAssetIdentifier())
            .collect(Collectors.toSet());
        Map<AssetIdentifier, MarketPrice> currentPrices = pricingService.getCurrentPrices(uniqueAssetsInPortfolio);

        Money totalValue = portfolio.calculateTotalValue(currentPrices);
        return new MoneyDto(totalValue.amount(), totalValue.currency().getCurrencyCode());
    }

    @Transactional(readOnly = true)
    public MoneyDto getUnrealizedGains(
        @Nonnull GetUnrealizedGainsQuery query
    ) {
        Portfolio portfolio = getPortfolio(query.portfolioId());

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
    public List<TransactionDto> getTransactionHistory(
        @Nonnull GetTransactionHistoryQuery query
    ) {
        
        Portfolio portfolio = getPortfolio(query.portfolioId());


        return portfolio.getTransactions().stream()
            .filter(tx -> !tx.getTransactionDate().isBefore(query.startDate()) && !tx.getTransactionDate().isAfter(query.endDate()))
            .sorted(Comparator.comparing(Transaction::getTransactionDate))
            .map(tx -> {
                String description;
                BigDecimal primaryAmount; // will be the DTO's amount field
                String relatedEntityName = null;
                UUID relatedEntityId = null;

                TransactionDetails details = tx.getTransactionDetails();
                TransactionType type = tx.getTransactionType();

                switch (type) {
                    case BUY:
                        if (details instanceof AssetTransactionDetails assetDetails) {
                            primaryAmount = assetDetails.getAssetValueInAssetCurrency().amount();
                            description = String.format("Bought %s shares of %s for %s %s",
                                assetDetails.getQuantity().stripTrailingZeros().toPlainString(),
                                assetDetails.getAssetIdentifier().symbol(),
                                assetDetails.getAssetValueInAssetCurrency().amount().stripTrailingZeros().toPlainString(),
                                assetDetails.getAssetValueInAssetCurrency().currency().getCurrencyCode()
                            );

                            relatedEntityName = assetDetails.getAssetIdentifier().symbol();
                            relatedEntityId = assetDetails.getAssetHoldingId();
                        }
                        else {
                            primaryAmount = tx.getTotalTransactionAmount().amount();
                            description = "BUY transaction with unexpected details";
                        }
                        break;
                
                    case SELL:
                        if (details instanceof AssetTransactionDetails assetDetails) {
                            Money netProceedsFromSale = assetDetails.getAssetValueInAssetCurrency().subtract(assetDetails.getTotalFeesInAssetCurrency());

                            primaryAmount = netProceedsFromSale.amount();
                            description = String.format("Sold %s shares of %s for %s %s (Fees: %s %s)",
                                assetDetails.getQuantity().stripTrailingZeros().toPlainString(),
                                assetDetails.getAssetIdentifier().symbol(),
                                netProceedsFromSale.amount().stripTrailingZeros().toPlainString(),
                                netProceedsFromSale.currency().getCurrencyCode(),
                                assetDetails.getTotalFeesInAssetCurrency().amount().stripTrailingZeros().toPlainString(),
                                assetDetails.getTotalFeesInAssetCurrency().currency().getCurrencyCode()
                            );

                            relatedEntityName = assetDetails.getAssetIdentifier().symbol();
                            relatedEntityId = assetDetails.getAssetHoldingId();
                        }   
                        else {
                            primaryAmount = tx.getTotalTransactionAmount().amount();
                            description = "SELL transaction with unexpected details.";
                        }

                        break;

                    case DEPOSIT:
                        if(details instanceof CashflowTransactionDetails cashflowDetails) { // Assuming Deposit uses CashflowTransactionDetails
                            primaryAmount = cashflowDetails.getOriginalCashflowAmount().amount();
                            description = String.format("Cash Deposit: %s %s",
                                cashflowDetails.getOriginalCashflowAmount().amount().stripTrailingZeros().toPlainString(),
                                cashflowDetails.getOriginalCashflowAmount().currency().getCurrencyCode()
                            );
                        } 
                        else {
                            primaryAmount = tx.getTotalTransactionAmount().amount();
                            description = "DEPOSIT transaction with unexpected details.";
                        }
                        break;

                    case WITHDRAWAL:
                        if (details instanceof CashflowTransactionDetails cashflowDetails) { // Assuming Withdrawal uses CashflowTransactionDetails
                            primaryAmount = cashflowDetails.getOriginalCashflowAmount().amount();
                            description = String.format("Cash Withdrawal: %s %s",
                                cashflowDetails.getOriginalCashflowAmount().amount().stripTrailingZeros().toPlainString(),
                                cashflowDetails.getOriginalCashflowAmount().currency().getCurrencyCode()
                            );
                        } 
                        else {
                            primaryAmount = tx.getTotalTransactionAmount().amount();
                            description = "WITHDRAWAL transaction with unexpected details.";
                        }
                        break;

                    case PAYMENT: // This refers to Liability Payment
                        if (details instanceof LiabilityPaymentTransactionDetails paymentDetails) {
                            primaryAmount = paymentDetails.getTotalPaymentAmountInLiabilityCurrency().amount();
                            description = String.format("Liability Payment: %s %s (Interest: %s %s)",
                                paymentDetails.getTotalPaymentAmountInLiabilityCurrency().amount().stripTrailingZeros().toPlainString(),
                                paymentDetails.getTotalPaymentAmountInLiabilityCurrency().currency().getCurrencyCode(),
                                paymentDetails.getInterestAmountInLiabilityCurrency().amount().stripTrailingZeros().toPlainString(),
                                paymentDetails.getInterestAmountInLiabilityCurrency().currency().getCurrencyCode()
                            );
                            relatedEntityId = paymentDetails.getLiabilityId(); // ASSUMPTION: liabilityId is stored in details

                            // OPTIONAL: Fetch actual liability name if portfolio provides a lookup method
                            // portfolio.getLiabilityById(relatedEntityId).map(Liability::getName).orElse(...)
                            relatedEntityName = "Liability " + paymentDetails.getLiabilityId().toString().substring(0, 8) + "..."; // Fallback
                        } 
                        else {
                             primaryAmount = tx.getTotalTransactionAmount().amount();
                             description = "PAYMENT transaction with unexpected details.";
                        }
                        break;
                    case LIABILITY_INCURRENCE:
                        if (details instanceof LiabilityIncurrenceTransactionDetails incurrenceDetails) {
                            primaryAmount = incurrenceDetails.getOriginalLoanAmountInLiabilityCurrency().amount();
                            description = String.format("New Loan Incurred: %s %s",
                                incurrenceDetails.getOriginalLoanAmountInLiabilityCurrency().amount().stripTrailingZeros().toPlainString(),
                                incurrenceDetails.getOriginalLoanAmountInLiabilityCurrency().currency().getCurrencyCode()
                            );
                            relatedEntityId = incurrenceDetails.getLiabilityId(); // ASSUMPTION: liabilityId is stored in details

                            // OPTIONAL: Fetch actual liability name
                            // relatedEntityName = portfolio.getLiabilityById(relatedEntityId).map(Liability::getName).orElse(...)
                            relatedEntityName = "Loan " + incurrenceDetails.getLiabilityId().toString().substring(0, 8) + "..."; // Fallback
                        }
                        else {
                            primaryAmount = tx.getTotalTransactionAmount().amount();
                            description = "LIABILITY_INCURRENCE transaction with unexpected details.";
                        }
                        break;
                    
                    case INTEREST_INCOME:
                        if (details instanceof InterestIncomeDetails incomeDetails) {
                            primaryAmount = incomeDetails.getAmountEarned().amount();
                            description = String.format("Interest Income: %s %s from %s",
                                incomeDetails.getAmountEarned().amount().stripTrailingZeros().toPlainString(),
                                incomeDetails.getAmountEarned().currency().getCurrencyCode(),
                                incomeDetails.getSourceDescription()
                            );
                            relatedEntityId = incomeDetails.getRealtedAccountId(); // Can be null
                            relatedEntityName = incomeDetails.getSourceDescription(); // Use source description as name
                        } else {
                            primaryAmount = tx.getTotalTransactionAmount().amount();
                            description = "INTEREST_INCOME transaction with unexpected details.";
                        }
                        break;

                    case INTEREST_EXPENSE:
                        if (details instanceof InterestExpenseDetails expenseDetails) {
                            primaryAmount = expenseDetails.getAmountAccruedOrPaid().amount();
                            description = String.format("Interest Expense: %s %s on Liability",
                                expenseDetails.getAmountAccruedOrPaid().amount().stripTrailingZeros().toPlainString(),
                                expenseDetails.getAmountAccruedOrPaid().currency().getCurrencyCode()
                            );
                            relatedEntityId = expenseDetails.getLiabilityId();
                            // You might want to fetch the liability name here:
                            // relatedEntityName = portfolio.getLiabilityById(relatedEntityId).map(Liability::getName).orElse(...)
                            relatedEntityName = "Liability " + expenseDetails.getLiabilityId().toString().substring(0, 8) + "..."; // Fallback
                        } else {
                            primaryAmount = tx.getTotalTransactionAmount().amount();
                            description = "INTEREST_EXPENSE transaction with unexpected details.";
                        }
                        break;

                    case SIMPLE: // For generic messages, assuming SimpleTransactionDetails
                        if (details instanceof SimpleTransactionDetails simpleDetails) {
                            primaryAmount = tx.getTotalTransactionAmount().amount(); // Use totalTransactionAmount as primary
                            description = simpleDetails.getDescription();
                        } 
                        else {
                             primaryAmount = tx.getTotalTransactionAmount().amount();
                             description = "SIMPLE transaction with unexpected details.";
                        }
                        break;
                   

                    default:
                        primaryAmount = tx.getTotalTransactionAmount().amount();
                        description = "Unknown Transaction Type: " + type.name();
                        break;
                }
                 // The totalTransactionAmount on the Transaction domain object is your net cash impact
                BigDecimal cashImpact = tx.getTotalTransactionAmount().amount();

                return new TransactionDto(
                    tx.getTransactionId(),
                    tx.getTransactionDate(),
                    tx.getTransactionType().name(),
                    description,
                    tx.getTotalTransactionAmount().currency().getCurrencyCode(), // Currency of the cash impact
                    primaryAmount,
                    cashImpact,
                    relatedEntityName,
                    relatedEntityId
                );
                
                
            })
            .collect(Collectors.toList());
    }

    
}