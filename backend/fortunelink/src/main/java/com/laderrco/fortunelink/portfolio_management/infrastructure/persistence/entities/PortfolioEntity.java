package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities;

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
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "portfolios")
public class PortfolioEntity {
    @Id
    private UUID id;
    
    @Column(name = "user_id", nullable =  false, unique = true)
    private UUID userId;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AccountEntity> accounts;

    @Version
    private Integer version;
}
