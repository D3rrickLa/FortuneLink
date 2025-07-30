package com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "liabilities")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LiabilityEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @Column(nullable = false)
    private String description;

    @Column(name = "liability_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType liabilityType;

    @Column(name = "principal_amount", nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "principal_currency", nullable = false)
    private String principalCurrencyCode;

    @Column(name = "remaining_balance_amount", nullable = false)
    private BigDecimal remainingBalanceAmount;

    @Column(name = "remaining_balance_currency", nullable = false)
    private String remainingBalanceCurrencyCode;

    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @Column(name = "incurrence_date", nullable = false)
    private Instant incurrenceDate;

    @Column(name = "due_date")
    private Instant dueDate;

    @Version
    private int version;

}
