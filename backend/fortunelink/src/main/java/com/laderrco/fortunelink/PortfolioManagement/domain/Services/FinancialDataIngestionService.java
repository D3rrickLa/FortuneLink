package com.laderrco.fortunelink.PortfolioManagement.domain.Services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.AssetHolding;
import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Liability;
import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Portfolio;
import com.laderrco.fortunelink.PortfolioManagement.domain.Repositories.IPortfolioRepository;
import com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces.IAssetPriceService;
import com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces.IFinancialDataIngestionService;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionType;

public class FinancialDataIngestionService implements IFinancialDataIngestionService {

    private final IPortfolioRepository portfolioRepository;
    private final IAssetPriceService assetPriceService;

    public FinancialDataIngestionService(IPortfolioRepository portfolioRepository,
            IAssetPriceService assetPriceService) {
        this.portfolioRepository = Objects.requireNonNull(portfolioRepository, "Portfolio Repository cannot be null.");
        this.assetPriceService = Objects.requireNonNull(assetPriceService, "Asset Price Service cannot be null.");
    }

    @Override
    public AssetHolding recordAssetPurchase(UUID portfolioId, AssetIdentifier assetIdentifier, BigDecimal quantity,
            Money costBasisPerUnit, LocalDate acquisitionDate) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
        Objects.requireNonNull(quantity, "Quantity cannot be null.");
        Objects.requireNonNull(costBasisPerUnit, "Cost Basis Per Unit cannot be null.");
        Objects.requireNonNull(acquisitionDate, "Acquisition Date cannot be null.");

        // You might fetch current market price here if it's required for the
        // transaction context
        // e.g., for reporting purposes right after purchase, though not strictly needed
        // for the purchase logic itself.
        // Optional<Money> currentMarketPrice =
        // assetPriceService.getCurrentPrice(assetIdentifier,
        // portfolio.getCurrencyPreference());

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found"));
        AssetHolding newHolding = portfolio.recordAssetPurchase(assetIdentifier, quantity, costBasisPerUnit,
                acquisitionDate, costBasisPerUnit);
        portfolioRepository.savePortfolio(portfolio);
        return newHolding;

    }

    @Override
    public void recordAssetSale(UUID portfolioId, AssetIdentifier assetIdentifier, BigDecimal quantityToSell,
            Money salePricePerUnit, Instant transactionDate) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(assetIdentifier, "Asset Identifier cannot be null.");
        Objects.requireNonNull(quantityToSell, "Quantity to Sell cannot be null.");
        Objects.requireNonNull(salePricePerUnit, "Sale Price Per Unit cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction Date cannot be null.");

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found with ID: " + portfolioId));

        // Delegate the core business logic to the Portfolio aggregate
        portfolio.recordAssetSale(assetIdentifier, quantityToSell, salePricePerUnit, transactionDate);

        portfolioRepository.savePortfolio(portfolio); // Persist the changes to the aggregate
    }

    @Override
    public Liability addNewLiability(UUID portfolioId, String name, String description, Money initialAmount,
            Percentage interestRate, LocalDate maturityDate) {

        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(name, "Name of liability cannot be null.");
        Objects.requireNonNull(initialAmount, "Amount owned cannot be null.");
        Objects.requireNonNull(interestRate, "Interest rate cannot be null.");
        Objects.requireNonNull(maturityDate, "Maturity date cannot be null.");
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found with ID: " + portfolioId));

        // Delegate to the Portfolio aggregate
        Liability newLiability = portfolio.addLiability(name, description, initialAmount, interestRate, maturityDate);

        portfolioRepository.savePortfolio(portfolio);
        return newLiability;
    }

    @Override
    public void recordLiabilityPayment(UUID portfolioId, UUID liabilityId, Money paymentAmount,
            Instant transactionDate) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found with ID: " + portfolioId));

        // Delegate to the Portfolio aggregate
        portfolio.removeLiability(liabilityId); // This method has the invariant check for zero balance

        portfolioRepository.savePortfolio(portfolio);
    }

    @Override
    public void removeLiability(UUID portfolioId, UUID liabilityId) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(liabilityId, "Liability ID cannot be null.");

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found with ID: " + portfolioId));

        // Delegate to the Portfolio aggregate
        portfolio.removeLiability(liabilityId); // This method has the invariant check for zero balance

        portfolioRepository.savePortfolio(portfolio);
    }

    @Override
    public void recordCashFlowTransaction(UUID portfolioId, TransactionType type, Money amount, String description,
            Instant transactionDate) {

        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(type, "Transaction type for cannot be null.");
        Objects.requireNonNull(amount, "Transfer amount cannot be null.");
        Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");

        // Ensure the type is a cash-only type (DEPOSIT, WITHDRAWAL, DIVIDEND,
        // INTEREST_INCOME).
        // This validation could also be inside the Portfolio.recordCashTransaction.
        // It's good to have it either at the service level (for input validation)
        // or aggregate level (for invariant enforcement).
        boolean isCashOnlyType = (type == TransactionType.DEPOSIT ||
                type == TransactionType.WITHDRAWAL ||
                type == TransactionType.DIVIDEND ||
                type == TransactionType.INTEREST_INCOME);
        if (!isCashOnlyType) {
            throw new IllegalArgumentException(
                    "Invalid transaction type for cash flow. Must be DEPOSIT, WITHDRAWAL, DIVIDEND, or INTEREST_INCOME.");
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found with ID: " + portfolioId));

        // Delegate to the Portfolio aggregate
        portfolio.recordCashTransaction(type, amount, description, transactionDate);

        portfolioRepository.savePortfolio(portfolio);
    }

    @Override
    // have to fix this
    // don't know if we pass the service or hard code it
    public void voidExistingTransaction(UUID portfolioId, UUID transactionId, String reason) {
        Objects.requireNonNull(portfolioId, "Portfolio ID cannot be null.");
        Objects.requireNonNull(transactionId, "Transaction ID cannot be null.");
        Objects.requireNonNull(reason, "Reason cannot be null.");

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new IllegalArgumentException("Portfolio not found with ID: " + portfolioId));

        // Delegate the voiding logic to the Portfolio aggregate, which in turn
        // delegates to the TransactionCorrectionService (as per our previous
        // discussion).
        portfolio.voidTransaction(transactionId, reason);

        portfolioRepository.savePortfolio(portfolio);
    }

}
