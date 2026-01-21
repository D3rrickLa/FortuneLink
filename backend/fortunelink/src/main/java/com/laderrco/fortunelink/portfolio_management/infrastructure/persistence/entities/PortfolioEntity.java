package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "portfolios")
public class PortfolioEntity {
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable =  false, unique = true)
    private UUID userId;
    
    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccountEntity> accounts;

    @Column(name = "portfolio_name", nullable =  false)
    private String name;
    
    @Column(name = "portfolio_currency_preference", length = 3)
    private String currencyPreference;

    @Column(name = "portfolio_description")
    private String description;
    
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Integer version;


    public PortfolioEntity(UUID id, UUID userId, String name, String currencyPref, String description) {
        this.id = id;
        this.userId = userId;
        this.accounts = new ArrayList<>();
        this.name = name;
        this.currencyPreference = currencyPref;
        this.description = description;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
