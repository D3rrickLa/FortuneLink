package com.laderrco.fortunelink.portfoliomanagment.infrastructure.persistence.jpa.valueobjects;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
public class AssetIdentifierPOJO {
    @Nonnull
    private String symbol;

    @Nonnull
    private String assetCommonName;

    @Nonnull
    @Enumerated(EnumType.STRING)
    private AssetType assetType;

    @Column(name = "industry_sector")
    private String industrySector;
}
