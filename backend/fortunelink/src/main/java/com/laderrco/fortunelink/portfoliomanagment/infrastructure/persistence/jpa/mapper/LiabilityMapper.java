package com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.mapper;

import java.util.Currency;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Liability;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities.LiabilityEntity;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities.PortfolioEntity;

@Component
public class LiabilityMapper {
    public LiabilityEntity toEntity(Liability domain, PortfolioEntity portfolioEntity) {
        if (domain == null) return null;

        return new LiabilityEntity(
            domain.getLiabilityId(),
            portfolioEntity,
            domain.getDescription(),
            domain.getLiabilityType(),
            domain.getPrincipalAmount().amount(),
            domain.getPrincipalAmount().currency().getCurrencyCode(),
            domain.getRemainingBalance().amount(),
            domain.getRemainingBalance().currency().getCurrencyCode(),
            domain.getAnnualInterestRate(),
            domain.getIncurrenceDate(),
            domain.getMaturityDate()
        );
    }

    public Liability toDomain(LiabilityEntity entity, UUID portfolioId) {
        if (entity == null) return null;

        Money principalAmount = new Money(entity.getPrincipalAmount(), Currency.getInstance(entity.getPrincipalCurrencyCode()));
        Money remainingBalance = new Money(entity.getRemainingBalanceAmount(), Currency.getInstance(entity.getRemainingBalanceCurrencyCode()));

        return new Liability(
            entity.getId(),
            portfolioId,
            entity.getDescription(),
            entity.getLiabilityType(),
            principalAmount,
            remainingBalance,
            entity.getInterestRate(),
            entity.getIncurrenceDate(),
            entity.getDueDate()
        );
    }
}
