package com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.mapper;

import java.util.Currency;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.AssetHolding;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities.AssetHoldingEntity;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities.PortfolioEntity;

@Component
public class AssetHoldingMapper {
    public AssetHoldingEntity toEntity(AssetHolding domain, PortfolioEntity portfolioEntity) {
        if (domain == null) return null;

        return new AssetHoldingEntity(
            domain.getAssetId(),
            portfolioEntity,
            domain.getAssetIdentifier().symbol(),
            domain.getAssetIdentifier().assetCommonName(),
            domain.getAssetIdentifier().assetType(),
            domain.getAssetIdentifier().industrySector(),
            domain.getTotalQuantity(),
            domain.getAverageACBPerUnit().amount(),
            domain.getAverageACBPerUnit().currency().getCurrencyCode(),
            domain.getCreatedAt(),
            0
        );
    }

    public AssetHolding toDomain(AssetHoldingEntity entity, String ISIN, String exchangeInfo, String description) {
        if (entity == null) return null;

        Money averageCostBasis = new Money(entity.getAverageCostBasisAmount(), Currency.getInstance(entity.getAverageCostBasisCurrencyCode()));
        AssetIdentifier assetIdentifier = new AssetIdentifier(
            entity.getAssetSymbol(),
            entity.getAssetType(),
            ISIN,
            entity.getAssetCommonName(),
            exchangeInfo,
            description,
            entity.getIndustrySector()
        );

        return new AssetHolding(
            entity.getId(),
            entity.getPortfolio().getId(),
            assetIdentifier,
            entity.getTotalQuantity(),
            averageCostBasis,
            entity.getPurchaseDate()
        );
    }
}
