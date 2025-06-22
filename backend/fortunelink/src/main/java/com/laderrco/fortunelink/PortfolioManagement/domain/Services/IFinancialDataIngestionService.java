package com.laderrco.fortunelink.PortfolioManagement.domain.Services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.AssetHolding;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;

public interface IFinancialDataIngestionService {
    // handles the processing of manual user input for transaction and ensures this data is correctly integrated inth the Portfolio aggregate struct
    // depends on Portoflio, account - supabase, transaction, and asset value object

    public AssetHolding recordAssetPurchase(UUID portfolioId, AssetIdentifier assetIdentifier,BigDecimal quantity, Money costBasisPerUnit, LocalDate acquisitionDate);
    public void recordAssetSale(UUID portfolioId, AssetIdentifier assetIdentifier, BigDecimal quantityToSell, Money salePricePerUnit, Instant transactionDate);
}
