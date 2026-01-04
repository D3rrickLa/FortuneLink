package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AccountEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AssetEntity;

public interface AssetEntityMapper {
    public AssetEntity toEntity(Asset domain, AccountEntity accountEntity);
    public void updateEntityFromDomain(Asset domain, AssetEntity entity);
    public Asset toDomain(AssetEntity entity);
    public AssetIdentifier toIdentifier(AssetEntity entity);
} 