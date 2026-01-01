package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CryptoIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionFeeEntity;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionEntityMapper {

    public Transaction toDomain(TransactionEntity entity) {
        AssetIdentifier identifier = null;
        if (entity.getAssetType() != null) {
            identifier = mapSnapshotToIdentifier(entity);
        }

        // 1. Reconstruct Value Objects
        Money price = new Money(entity.getPriceAmount(), ValidatedCurrency.of(entity.getPriceCurrency()));
        
        // Handle nullable dividend amount
        Money dividend = entity.getDividendAmount() != null 
            ? new Money(entity.getDividendAmount(), ValidatedCurrency.of(entity.getDividendCurrency())) 
            : null;

        // Assuming Fee mapping logic exists
        List<Fee> fees = new ArrayList<>();
        for (TransactionFeeEntity fee : entity.getFees()) {
            fees.add(mapFeeToDomain(fee));
        }

        // 2. Use the reconstitution factory
        return Transaction.reconstitute(
            new TransactionId(entity.getId()),
            new AccountId(entity.getAccountId()),
            TransactionType.valueOf(entity.getTransactionType()),
            identifier,
            entity.getQuantity(),
            price,
            dividend,
            fees,
            entity.getTransactionDate(),
            entity.getNotes(),
            entity.getIsDrip() // Ensure this exists in your Entity
        );
    }

    public TransactionEntity toEntity(Transaction domain) {
        TransactionEntity entity = new TransactionEntity();
        entity.setId(domain.getTransactionId().transactionId());
        // ... map other fields ...

        List<TransactionFeeEntity> feeEntities = domain.getFees().stream().map(f -> {
            TransactionFeeEntity feeEntity = new TransactionFeeEntity();
            feeEntity.setFeeType(f.feeType());
            feeEntity.setAmount(f.amountInNativeCurrency().amount());
            feeEntity.setCurrency(f.amountInNativeCurrency().currency().getCode());
            
            if (f.exchangeRate() != null) {
                feeEntity.setRate(f.exchangeRate().rate());
                feeEntity.setFromCurrency(f.exchangeRate().from().getCode());
                feeEntity.setToCurrency(f.exchangeRate().to().getCode());
            }
            
            feeEntity.setMetadata(f.metadata());
            feeEntity.setFeeDate(f.feeDate());
            return feeEntity;
        }).toList();

        entity.setFees(feeEntities);
        return entity;
    }

    private AssetIdentifier mapSnapshotToIdentifier(TransactionEntity entity) {
        String type = entity.getAssetType();
        if (type == null) return null;

        return switch (type.toUpperCase()) {
            case "MARKET" -> new MarketIdentifier(
                entity.getPrimaryId(), 
                entity.getSecondaryIds(),
                AssetType.valueOf(entity.getAssetType()), 
                entity.getDisplayName(),
                entity.getUnitOfTrade(), 
                entity.getMetadata()
            );
            case "CASH" -> new CashIdentifier(
                entity.getPrimaryId(), 
                ValidatedCurrency.of(entity.getPrimaryId()) // Use .of() factory if available
            );
            case "CRYPTO" -> new CryptoIdentifier(
                entity.getPrimaryId(), 
                entity.getDisplayName(),
                AssetType.valueOf(entity.getAssetType()), 
                entity.getUnitOfTrade(),
                entity.getMetadata()
            );
            default -> throw new IllegalArgumentException("Unsupported asset snapshot type: " + type);
        };
    }

    private Fee mapFeeToDomain(TransactionFeeEntity f) {
        ExchangeRate rate = null;
        
        // Only create the ExchangeRate if the data exists in DB
        if (f.getRate() != null) {
            rate = new ExchangeRate(
                ValidatedCurrency.of(f.getFromCurrency()),
                ValidatedCurrency.of(f.getToCurrency()),
                f.getRate(),
                f.getExchangeRateDate(),
                f.getRateSource()
            );
        }

        return new Fee(
            f.getFeeType(),
            new Money(f.getAmount(), ValidatedCurrency.of(f.getCurrency())),
            rate,
            f.getMetadata() != null ? f.getMetadata() : Collections.emptyMap(),
            f.getFeeDate()
        );
    }
}