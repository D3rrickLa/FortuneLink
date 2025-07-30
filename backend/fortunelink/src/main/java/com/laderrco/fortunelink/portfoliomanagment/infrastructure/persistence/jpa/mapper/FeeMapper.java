package com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.mapper;

import java.util.Currency;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities.FeeEntity;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities.TransactionEntity;

@Component
public class FeeMapper {
    public FeeEntity toEntity(Fee domain, TransactionEntity transactionEntity) {
        if (domain == null) return null;
        return new FeeEntity(
            UUID.randomUUID(),
            transactionEntity,
            domain.feeType().toString(),
            domain.amount().amount(),
            domain.amount().currency().getCurrencyCode()
        );
    }

    public Fee toDomain(FeeEntity entity) {
        if (entity == null) return null;
        Money amount = new Money(entity.getAmount(), Currency.getInstance(entity.getCurrencyCode()));
        return new Fee(FeeType.valueOf(entity.getCategory()), amount);
    }
}
