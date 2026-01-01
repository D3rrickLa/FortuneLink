package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
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
    // this is needed because we have asset identifier in it, which needs to be flatten
    private String primaryId; // part of ALL, NOTE: cash Id gives only the Symbol as we can build the record from there
    private String assetType;
    private String displayName;

    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> secondaryIds;
    
    private String unitOfTrade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> metadata; // part of MI, CI

    // //

    private BigDecimal quantity;
    private BigDecimal priceAmount; // price per unit
    private String priceCurrency;


    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id") // Creates the foreign key in transaction_fees table
    private List<TransactionFeeEntity> fees = new ArrayList<>();

    // Add these for DRIP support
    private BigDecimal dividendAmount; 
    private String dividendCurrency;

    private BigDecimal feeAmount;
    private String feeCurrency;

    private Instant transactionDate;
    private String notes;
    private Boolean isDrip; // Use primitive boolean or handle nulls in mapper




    @Version
    private Integer version;
}