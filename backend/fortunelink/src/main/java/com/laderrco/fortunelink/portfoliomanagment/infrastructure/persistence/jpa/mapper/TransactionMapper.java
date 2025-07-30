package com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.mapper;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Transaction;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.TransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities.PortfolioEntity;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities.TransactionEntity;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.valueobjects.TransactionDetailBaseEntity;

@Component
public class TransactionMapper {
    private final ObjectMapper objectMapper;
    private final FeeMapper feeMapper;

    public TransactionMapper(ObjectMapper objectMapper, FeeMapper feeMapper) {
        this.objectMapper = objectMapper;
        this.feeMapper = feeMapper;
    }

    public TransactionEntity toEntity(Transaction domain, PortfolioEntity portfolioEntity) {
        if (domain == null) return null;

        TransactionEntity entity = new TransactionEntity(
            domain.getTransactionId().value(),
            portfolioEntity,
            domain.getCorrelationId().orElse(null),
            domain.getParentTransactionId().orElse(null),
            domain.getTransactionType(),
            domain.getNetCashImpact().amount(),
            domain.getNetCashImpact().currency().getCurrencyCode(),
            domain.getTransactionDate(),
            mapToJson(domain.getMetadata()),
            domain.isHidden()
        );

        entity.setDetails(toDetailsEntity(domain.getTransactionDetails(), entity));
        domain.getFees().forEach(fee -> entity.addFee(feeMapper.toEntity(fee, entity)));

        return entity;
    }

    public Transaction toDomain(TransactionEntity entity) {
        if (entity == null) return null;

        Money netCashImpact = new Money(entity.getNetCashImpactAmount(), Currency.getInstance(entity.getNetCashImpactCurrencyCode()));
        Map<String, String> metadata = mapFromJson(entity.getMetadataJson());

        return new Transaction(
            entity.getId(),
            entity.getPortfolio().getId(),
            entity.getTransactionType(),
            netCashImpact,
            entity.getTransactionDate(),
            toDetailsDomain(entity.getDetails()),
            entity.getCorrelationId().orElse(null),
            entity.getParentTransactionId().orElse(null),
            metadata,
            entity.getFees().stream().map(feeMapper::toDomain).collect(Collectors.toList()),
            entity.isHidden()
        );
    }

    private TransactionDetailBaseEntity toDetailsEntity(TransactionDetails domainDetails, TransactionEntity transactionEntity) {
        if (domainDetails == null) return null;

        if (domainDetails instanceof AssetTransactionDetails) {
            AssetTransactionDetails d = (AssetTransactionDetails) domainDetails;
            return new AssetTransactionDetailsEntity(
                transactionEntity.getId(), transactionEntity,
                d.getAssetIdentifier().symbol(), d.getAssetIdentifier().assetCommonName(), d.getAssetIdentifier().assetType(), d.getAssetIdentifier().industrySector(),
                d.getQuantity(), d.getPricePerUnit().amount(), d.getPricePerUnit().currency().getCurrencyCode(),
                d.getAssetValueInAssetCurrency().amount(), d.getAssetValueInAssetCurrency().currency().getCurrencyCode(),
                d.getAssetValueInPortfolioCurrency().amount(), d.getAssetValueInPortfolioCurrency().currency().getCurrencyCode(),
                d.getCostBasisInPortfolioCurrency().amount(), d.getCostBasisInPortfolioCurrency().currency().getCurrencyCode(),
                d.getCostBasisInAssetCurrency().amount(), d.getCostBasisInAssetCurrency().currency().getCurrencyCode(),
                d.getTotalFeesInPortfolioCurrency().amount(), d.getTotalFeesInPortfolioCurrency().currency().getCurrencyCode(),
                d.getTotalFeesInAssetCurrency().amount(), d.getTotalFeesInAssetCurrency().currency().getCurrencyCode(),
                d.getAssetId().value()
            );
        } else if (domainDetails instanceof CashTransactionDetails) {
            CashTransactionDetails d = (CashTransactionDetails) domainDetails;
            return new CashTransactionDetailsEntity(
                transactionEntity.getId(), transactionEntity,
                d.getCashFlow().amount(), d.getCashFlow().currency().getCurrencyCode(),
                d.getReason()
            );
        } else if (domainDetails instanceof LiabilityIncurrenceTransactionDetails) {
            LiabilityIncurrenceTransactionDetails d = (LiabilityIncurrenceTransactionDetails) domainDetails;
            return new LiabilityIncurrenceTransactionDetailsEntity(
                transactionEntity.getId(), transactionEntity,
                d.getLiabilityId().value(), d.getLiabilityDescription(), d.getLiabilityType(),
                d.getPrincipalAmountIncurred().amount(), d.getPrincipalAmountIncurred().currency().getCurrencyCode(),
                d.getInterestRate()
            );
        } else if (domainDetails instanceof LiabilityPaymentTransactionDetails) {
            LiabilityPaymentTransactionDetails d = (LiabilityPaymentTransactionDetails) domainDetails;
            return new LiabilityPaymentTransactionDetailsEntity(
                transactionEntity.getId(), transactionEntity,
                d.getLiabilityId().value(),
                d.getPaymentAmount().amount(), d.getPaymentAmount().currency().getCurrencyCode()
            );
        } else if (domainDetails instanceof InterestIncomeTransactionDetails) {
            InterestIncomeTransactionDetails d = (InterestIncomeTransactionDetails) domainDetails;
            return new InterestIncomeTransactionDetailsEntity(
                transactionEntity.getId(), transactionEntity,
                d.getSourceDescription(),
                d.getIncomeAmount().amount(), d.getIncomeAmount().currency().getCurrencyCode()
            );
        } else if (domainDetails instanceof InterestExpenseTransactionDetails) {
            InterestExpenseTransactionDetails d = (InterestExpenseTransactionDetails) domainDetails;
            return new InterestExpenseTransactionDetailsEntity(
                transactionEntity.getId(), transactionEntity,
                d.getSourceDescription(),
                d.getExpenseAmount().amount(), d.getExpenseAmount().currency().getCurrencyCode(),
                d.getLiabilityId().orElse(null)
            );
        }
        throw new IllegalArgumentException("Unknown TransactionDetails type: " + domainDetails.getClass().getName());
    }

    private TransactionDetails toDetailsDomain(TransactionDetailBaseEntity entityDetails) {
        if (entityDetails == null) return null;

        if (entityDetails instanceof AssetTransactionDetailsEntity) {
            AssetTransactionDetailsEntity e = (AssetTransactionDetailsEntity) entityDetails;
            return new AssetTransactionDetails(
                new AssetId(e.getAssetHoldingId()),
                new AssetIdentifier(e.getAssetSymbol(), e.getAssetCommonName(), e.getAssetType(), e.getIndustrySector()),
                e.getQuantity(),
                new Money(e.getPricePerUnitAmount(), Currency.getInstance(e.getPricePerUnitCurrencyCode())),
                new Money(e.getAssetValueInAssetCurrencyAmount(), Currency.getInstance(e.getAssetValueInAssetCurrencyCode())),
                new Money(e.getAssetValueInPortfolioCurrencyAmount(), Currency.getInstance(e.getAssetValueInPortfolioCurrencyCode())),
                new Money(e.getCostBasisInPortfolioCurrencyAmount(), Currency.getInstance(e.getCostBasisInPortfolioCurrencyCode())),
                new Money(e.getCostBasisInAssetCurrencyAmount(), Currency.getInstance(e.getCostBasisInAssetCurrencyCode())),
                new Money(e.getTotalFeesInPortfolioCurrencyAmount(), Currency.getInstance(e.getTotalFeesInPortfolioCurrencyCode())),
                new Money(e.getTotalFeesInAssetCurrencyAmount(), Currency.getInstance(e.getTotalFeesInAssetCurrencyCode()))
            );
        } else if (entityDetails instanceof CashTransactionDetailsEntity) {
            CashTransactionDetailsEntity e = (CashTransactionDetailsEntity) entityDetails;
            return new CashTransactionDetails(
                new Money(e.getCashFlowAmount(), Currency.getInstance(e.getCashFlowCurrencyCode())),
                e.getReason()
            );
        } else if (entityDetails instanceof LiabilityIncurrenceTransactionDetailsEntity) {
            LiabilityIncurrenceTransactionDetailsEntity e = (LiabilityIncurrenceTransactionDetailsEntity) entityDetails;
            return new LiabilityIncurrenceTransactionDetails(
                new LiabilityId(e.getLiabilityId()), e.getLiabilityDescription(), e.getLiabilityType(),
                new Money(e.getPrincipalAmountIncurred(), Currency.getInstance(e.getPrincipalCurrencyCodeIncurred())),
                e.getInterestRate()
            );
        } else if (entityDetails instanceof LiabilityPaymentTransactionDetailsEntity) {
            LiabilityPaymentTransactionDetailsEntity e = (LiabilityPaymentTransactionDetailsEntity) entityDetails;
            return new LiabilityPaymentTransactionDetails(
                new LiabilityId(e.getLiabilityId()),
                new Money(e.getPaymentAmount(), Currency.getInstance(e.getPaymentCurrencyCode()))
            );
        } else if (entityDetails instanceof InterestIncomeTransactionDetailsEntity) {
            InterestIncomeTransactionDetailsEntity e = (InterestIncomeTransactionDetailsEntity) entityDetails;
            return new InterestIncomeTransactionDetails(
                e.getSourceDescription(),
                new Money(e.getIncomeAmount(), Currency.getInstance(e.getIncomeCurrencyCode()))
            );
        } else if (entityDetails instanceof InterestExpenseTransactionDetailsEntity) {
            InterestExpenseTransactionDetailsEntity e = (InterestExpenseTransactionDetailsEntity) entityDetails;
            return new InterestExpenseTransactionDetails(
                e.getSourceDescription(),
                new Money(e.getExpenseAmount(), Currency.getInstance(e.getExpenseCurrencyCode())),
                e.getLiabilityId()
            );
        }
        throw new IllegalArgumentException("Unknown TransactionDetailBaseEntity type: " + entityDetails.getClass().getName());
    }

    private String mapToJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert metadata to JSON", e);
        }
    }

    private Map<String, String> mapFromJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(metadataJson, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert JSON to metadata", e);
        }
    }
}
