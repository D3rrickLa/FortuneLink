package com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.AssetHolding;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Liability;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Transaction;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;

import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "portfolios")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class PortfolioEntity {
    @Id
    @Embedded
    private UUID id;

    @Nonnull
    private UUID userId;

    @Nonnull
    private String name;

    private String description;

    // NOTE: we are using 2 fields for the cash amount because will need to be a POJO for Money
    @Nonnull
    private BigDecimal cashBalanceAmount;

    @Nonnull
    private String cashBalanceCurrencyCode;

    @Nonnull
    private String currencyPreferenceCode;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AssetHoldingEntity> assetHoldings = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LiabilityEntity> liabilities = new ArrayList<>();

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TransactionEntity> transactions = new ArrayList<>();


    @Version
    private int version;

    public PortfolioEntity(
        UUID portfolioId, 
        UUID userId, 
        String name, 
        String description,
        Money initialCashBalance, 
        Currency currencyPreference
    ) {
        // Domain invariants check
        if (portfolioId == null || userId == null || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Portfolio ID, User ID, and name cannot be null or empty.");
        }
        if (initialCashBalance == null || initialCashBalance.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Initial cash balance cannot be null or negative.");
        }
        if (currencyPreference == null) {
            throw new IllegalArgumentException("Currency preference cannot be null.");
        }

        this.id = portfolioId;
        this.userId = userId;   
        this.name = name;
        this.description = description;
        this.cashBalanceAmount = initialCashBalance.amount();
        this.cashBalanceCurrencyCode = initialCashBalance.currency().getCurrencyCode();
        this.currencyPreferenceCode = currencyPreference.getCurrencyCode();
        this.assetHoldings = new ArrayList<>();
        this.liabilities = new ArrayList<>();
        this.transactions = new ArrayList<>();
        this.version = 0; // Initial version
    }

    public Money getPortfolioCashBalance() {
        return new Money(this.cashBalanceAmount, Currency.getInstance(this.cashBalanceCurrencyCode));
    }
    
    public Currency getCurrencyPreference() {
        return Currency.getInstance(this.currencyPreferenceCode);
    }
    
    public List<AssetHolding> getAssetHoldings() {
        return Collections.unmodifiableList(assetHoldings); // Return unmodifiable list
    }

    public List<Liability> getLiabilities() {
        return Collections.unmodifiableList(liabilities);
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }
}
