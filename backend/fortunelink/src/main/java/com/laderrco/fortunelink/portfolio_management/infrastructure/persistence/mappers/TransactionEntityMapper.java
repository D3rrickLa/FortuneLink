package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AccountEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;

public interface TransactionEntityMapper {
    public Transaction toDomain(TransactionEntity entity);
    public TransactionEntity toEntity(Transaction domain, AccountEntity accountEntity);
}
