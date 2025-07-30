package com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.valueobjects.AssetIdentifierPOJO;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
@Table(name = "asset_holdings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetHoldingEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @Column(name = "asset_symbol", nullable = false)
    private String assetSymbol;

    @Column(name = "asset_common_name")
    private String assetCommonName;

    @Column(name = "asset_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AssetType assetType;

    @Column(name = "industry_sector")
    private String industrySector;

    @Column(name = "total_quantity", nullable = false)
    private BigDecimal totalQuantity;

    @Column(name = "average_acb_amount", nullable = false)
    private BigDecimal averageCostBasisAmount;

    @Column(name = "average_acb_currency", nullable = false)
    private String averageCostBasisCurrencyCode;

    @Column(name = "purchase_date", nullable = false)
    private Instant purchaseDate;

    @Version
    private int version;
}
