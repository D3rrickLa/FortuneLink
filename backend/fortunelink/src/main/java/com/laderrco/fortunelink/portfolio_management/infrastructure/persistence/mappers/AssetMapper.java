package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AccountEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AssetEntity;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;

@Component
public class AssetMapper {

    /**
     * Maps Domain -> Entity (for creating new records)
     */
    public AssetEntity toEntity(Asset domain, AccountEntity accountEntity) {
        AssetEntity entity = new AssetEntity();
        entity.setId(domain.getAssetId().assetId());
        entity.setAccount(accountEntity); // Essential for JPA relationship
        
        updateEntityFromDomain(domain, entity);
        
        // Version is usually managed by JPA @Version, 
        // but we set it here for the initial state if needed
        entity.setVersion(domain.getVersion());
        
        return entity;
    }

    /**
     * Updates an existing Entity with Domain state (for updates)
     */
    public void updateEntityFromDomain(Asset domain, AssetEntity entity) {
        AssetIdentifier iden = domain.getAssetIdentifier();
        
        // Flatten Identifier to Entity columns
        entity.setIdentifierType(determineIdentifierType(iden));
        entity.setPrimaryId(iden.getPrimaryId());
        entity.setName(iden.displayName());
        entity.setAssetType(iden.getAssetType().name());

        // Fill polymorphic "bucket" columns based on implementation
        if (iden instanceof MarketIdentifier m) {
            entity.setSecondaryIds(m.secondaryIds());
            entity.setUnitOfTrade(m.unitOfTrade());
            entity.setMetadata(m.metadata());
        } else if (iden instanceof CryptoIdentifier c) {
            entity.setUnitOfTrade(c.unitOfTrade());
            entity.setMetadata(c.metadata());
        } else if (iden instanceof CashIdentifier) {
            // Cash typically only uses primaryId (USD, EUR)
            entity.setSecondaryIds(null);
            entity.setUnitOfTrade(null);
            entity.setMetadata(null);
        }
        
        // Financials
        entity.setQuantity(domain.getQuantity());
        entity.setCostBasisAmount(domain.getCostBasis().amount());
        entity.setCostBasisCurrency(domain.getCostBasis().currency().getCode());
        entity.setAcquiredDate(domain.getAcquiredOn());
        entity.setLastInteraction(domain.getLastSystemInteraction());
    }

    /**
     * Maps Entity -> Domain (Reconstitution)
     */
    public Asset toDomain(AssetEntity entity) {
        return Asset.reconstitute(
            new AssetId(entity.getId()),
            toIdentifier(entity),
            entity.getCurrency(),
            entity.getQuantity(),
            entity.getCostBasisAmount(),
            entity.getCostBasisCurrency(),
            entity.getAcquiredDate(),
            entity.getLastInteraction(),
            entity.getVersion() != null ? entity.getVersion().intValue() : 0
        );
    }

    public AssetIdentifier toIdentifier(AssetEntity entity) {
        return switch (entity.getIdentifierType()) {
            case "MARKET" -> new MarketIdentifier(
                entity.getPrimaryId(),
                entity.getSecondaryIds(),
                AssetType.valueOf(entity.getAssetType()),
                entity.getName(),
                entity.getUnitOfTrade(),
                entity.getMetadata()
            );
            case "CASH" -> new CashIdentifier(
                entity.getPrimaryId(),
                ValidatedCurrency.of(entity.getPrimaryId())
            );
            case "CRYPTO" -> new CryptoIdentifier(
                entity.getPrimaryId(),
                entity.getName(),
                AssetType.valueOf(entity.getAssetType()),
                entity.getUnitOfTrade(),
                entity.getMetadata()
            );
            default -> throw new IllegalStateException("Unknown identifier type: " + entity.getIdentifierType());
        };
    }

    /**
     * Determines the String discriminator for the database column
     */
    private String determineIdentifierType(AssetIdentifier identifier) {
        if (identifier instanceof MarketIdentifier) return "MARKET";
        if (identifier instanceof CashIdentifier) return "CASH";
        if (identifier instanceof CryptoIdentifier) return "CRYPTO";
        throw new IllegalArgumentException("Unknown identifier implementation: " + identifier.getClass());
    }
}