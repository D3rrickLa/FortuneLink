package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import java.util.Collections;

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
import com.laderrco.fortunelink.shared.valueobjects.ClassValidation;

// TODO: NUll checks
@Component
public class AssetMapper implements ClassValidation {

    /**
     * Maps Domain -> Entity (for creating new records)
     */
    public AssetEntity toEntity(Asset domain, AccountEntity accountEntity) {
        ClassValidation.validateParameter(domain, "Domain asset cannot be null");
        ClassValidation.validateParameter(accountEntity, "Account entity cannot be null");
        
        AssetEntity entity = new AssetEntity();
        entity.setId(domain.getAssetId().assetId());
        entity.setAccount(accountEntity);
        
        updateEntityFromDomain(domain, entity);
        
        // Version is managed by JPA @Version - DO NOT set it manually
        // JPA will initialize it to 0 on first persist
        
        return entity;
    }

    /**
     * Updates an existing Entity with Domain state (for updates)
     */
    public void updateEntityFromDomain(Asset domain, AssetEntity entity) {
        ClassValidation.validateParameter(domain, "Domain asset cannot be null");
        ClassValidation.validateParameter(entity, "Entity cannot be null");
        AssetIdentifier iden = domain.getAssetIdentifier();

        // Flatten Identifier to Entity columns
        entity.setIdentifierType(determineIdentifierType(iden));
        entity.setPrimaryId(iden.getPrimaryId());
        entity.setName(iden.displayName());
        entity.setAssetType(iden.getAssetType().name());

        // Fill polymorphic "bucket" columns based on implementation
        switch (iden) {
            case MarketIdentifier m -> {
                entity.setSecondaryIds(m.secondaryIds() != null ? m.secondaryIds() : Collections.emptyMap());
                entity.setUnitOfTrade(m.unitOfTrade());
                entity.setMetadata(m.metadata() != null ? m.metadata() : Collections.emptyMap());
            }
            case CryptoIdentifier c -> {
                entity.setSecondaryIds(Collections.emptyMap());
                entity.setUnitOfTrade(c.unitOfTrade());
                entity.setMetadata(c.metadata() != null ? c.metadata() : Collections.emptyMap());
            }
            case CashIdentifier _ -> {
                // Cash only uses primaryId (USD, EUR)
                entity.setSecondaryIds(Collections.emptyMap());
                entity.setUnitOfTrade(null);
                entity.setMetadata(Collections.emptyMap());
            }

            default -> {
                throw new IllegalArgumentException("Provided unknown Identifier");
            }

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
        ClassValidation.validateParameter(entity, "Entity cannot be null");
        return Asset.reconstitute(
                new AssetId(entity.getId()),
                toIdentifier(entity),
                entity.getCostBasisCurrency(),
                entity.getQuantity(),
                entity.getCostBasisAmount(),
                entity.getCostBasisCurrency(),
                entity.getAcquiredDate(),
                entity.getLastInteraction());
        }

    public AssetIdentifier toIdentifier(AssetEntity entity) {
        ClassValidation.validateParameter(entity, "Entity cannot be null");
        String identifierType = entity.getIdentifierType();
        if (identifierType == null) {
            throw new IllegalStateException(
                    "Asset " + entity.getId() + " has null identifier type");
        }
        return switch (entity.getIdentifierType()) {
            case "MARKET" -> new MarketIdentifier(
                    entity.getPrimaryId(),
                    entity.getSecondaryIds(),
                    AssetType.valueOf(entity.getAssetType()),
                    entity.getName(),
                    entity.getUnitOfTrade(),
                    entity.getMetadata());
            case "CASH" -> new CashIdentifier(
                    entity.getPrimaryId(),
                    ValidatedCurrency.of(entity.getPrimaryId()));
            case "CRYPTO" -> new CryptoIdentifier(
                    entity.getPrimaryId(),
                    entity.getName(),
                    AssetType.valueOf(entity.getAssetType()),
                    entity.getUnitOfTrade(),
                    entity.getMetadata());
            default -> throw new IllegalStateException(String.format("Unknown identifier type '%s'",  entity.getIdentifierType().toString()));
        };
    }

    /**
     * Determines the String discriminator for the database column
     */
    protected String determineIdentifierType(AssetIdentifier identifier) {
        return switch (identifier) {
            case MarketIdentifier _ -> "MARKET";
            case CashIdentifier _ -> "CASH";
            case CryptoIdentifier _ -> "CRYPTO";
            default -> throw new IllegalArgumentException(
                    "Unknown identifier implementation: " + identifier.getClass().getName());
        };
    }
}