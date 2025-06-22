package com.laderrco.fortunelink.PortfolioManagement.domain.Services.interfaces;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.AssetHolding;
import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Liability;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Percentage;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.TransactionType;

public interface IFinancialDataIngestionService {
    // handles the processing of manual user input for transaction and ensures this
    // data is correctly integrated inth the Portfolio aggregate struct
    // depends on Portoflio, account - supabase, transaction, and asset value object

    public AssetHolding recordAssetPurchase(UUID portfolioId, AssetIdentifier assetIdentifier, BigDecimal quantity,
            Money costBasisPerUnit, LocalDate acquisitionDate);

    public void recordAssetSale(UUID portfolioId, AssetIdentifier assetIdentifier, BigDecimal quantityToSell,
            Money salePricePerUnit, Instant transactionDate);

    public Liability addNewLiability(UUID portfolioId, String name, String description,
            Money initialAmount, Percentage interestRate, LocalDate maturityDate);

    public void recordLiabilityPayment(UUID portfolioId, UUID liabilityId, Money paymentAmount,
            Instant transactionDate);

    public void removeLiability(UUID portfolioId, UUID liabilityId);

    public void recordCashFlowTransaction(UUID portfolioId, TransactionType type, Money amount, String description,
            Instant transactionDate);

    public void voidExistingTransaction(UUID portfolioId, UUID transactionId, String reason);


}
