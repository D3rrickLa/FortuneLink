package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
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

    // Bidirectional relationship to Account (the owner)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    // Denormalized portfolio reference for query performance
    @Column(name = "portfolio_id", nullable = false)
    private UUID portfolioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType; // TODO: find out if this should be TransactionType or String

    // Asset snapshot fields
    @Column(name = "primary_id", nullable = false)
    private String primaryId;
    
    @Column(name = "asset_type", nullable = false)
    private String assetType;
    
    @Column(name = "display_name")
    private String displayName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "secondary_ids")
    private Map<String, String> secondaryIds;
    
    @Column(name = "unit_of_trade")
    private String unitOfTrade;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata") // columnDefinition = "JSONB"
    private Map<String, String> metadata;

    // Transaction details
    @Column(nullable = false)
    private BigDecimal quantity;
    
    @Column(name = "price_amount", nullable = false)
    private BigDecimal priceAmount;
    
    @Column(name = "price_currency", nullable = false)
    private String priceCurrency;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private List<TransactionFeeEntity> fees = new ArrayList<>();

    // DRIP support
    @Column(name = "dividend_amount")
    private BigDecimal dividendAmount;
    
    @Column(name = "dividend_currency")
    private String dividendCurrency;
    
    @Column(name = "is_drip")
    private Boolean isDrip;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Integer version;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}