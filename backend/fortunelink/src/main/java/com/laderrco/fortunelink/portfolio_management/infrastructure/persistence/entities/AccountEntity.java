package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "accounts")
@ToString(exclude = "portfolio") // issue with testing shouldAddNewAccount(), circular DP
public class AccountEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private PortfolioEntity portfolio;

    private String name;

    @Enumerated(EnumType.STRING)
    private AccountType accountType;
    private String baseCurrency;

    private BigDecimal cashBalanceAmount;
    private String cashBalanceCurrency;

    private boolean isActive;

    @Column(name = "closed_date")
    private Instant closedDate;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssetEntity> assets = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionEntity> transactions = new ArrayList<>();

    @Column(name = "created_date")
    private Instant createdDate;

    @Column(name = "last_system_interaction")
    private Instant lastUpdated;

    @Version
    private Integer version;
}
