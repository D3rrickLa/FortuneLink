package com.laderrco.fortunelink.PortfolioManagement.domain.Services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.AssetHolding;
import com.laderrco.fortunelink.PortfolioManagement.domain.Repositories.IPortfolioRepository;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Money;

public class FinancialDataIngestionService implements IFinancialDataIngestionService {

    private final IPortfolioRepository portfolioRepository;
    private final IAssetPriceService assetPriceService;

    public FinancialDataIngestionService(IPortfolioRepository portfolioRepository, IAssetPriceService assetPriceService) {
        this.portfolioRepository = Objects.requireNonNull(portfolioRepository, "Portfolio Repository cannot be null.");
        this.assetPriceService = Objects.requireNonNull(assetPriceService, "Asset Price Service cannot be null.");
    }

    @Override
    public AssetHolding recordAssetPurchase(UUID portfolioId, AssetIdentifier assetIdentifier, BigDecimal quantity, Money costBasisPerUnit, LocalDate acquisitionDate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordAssetPurchase'");
    }

}
