package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    private UUID id;

    private UUID portfolioId;
    private UUID accountId;
    private String transactionType;

    // Snapshot of the asset at time of transaction
    private String identifierType;
    private String primaryId;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> secondaryIds;

    private BigDecimal quantity;
    private BigDecimal priceAmount;
    private String priceCurrency;
    private BigDecimal feeAmount;
    private String feeCurrency;

    private Instant transactionDate;
    private String notes;

    @Version
    private Integer version;
}