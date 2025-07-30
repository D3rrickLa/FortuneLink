package com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @Column(name = "parent_transaction_id")
    private UUID parentTransactionId;

    @Column(name = "transaction_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "net_cash_impact_amount", nullable = false)
    private BigDecimal netCashImpactAmount;

    @Column(name = "net_cash_impact_currency", nullable = false)
    private String netCashImpactCurrencyCode;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private TransactionDetailBaseEntity details;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadataJson;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FeeEntity> fees = new ArrayList<>();

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden;

    @Version
    private int version;
}
