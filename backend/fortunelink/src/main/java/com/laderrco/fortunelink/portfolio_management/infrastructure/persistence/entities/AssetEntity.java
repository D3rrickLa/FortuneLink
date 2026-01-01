package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// This is where we flatten our AssetIdentifier interface, we don't use inheritance here; we use a single table with a discriminator column becasie
// it matches our SQL

/*
    A bit confusing but basically we are NOT serializing the whole Java object into a single blobk, but we are flattening AssetIdentifier into the AssetEntity

    THe 'Mapper' will spread the identifier's data acorss several columns
    AssetEntity Column	    If it's a MarketIdentifier	    If it's a CashIdentifier
    identifierType	        "MARKET"	                    "CASH"
    primaryId	            "AAPL" (Ticker)	                "USD" (Currency Code)
    assetType	            "STOCK"	                        null
    name	                "Apple Inc."	                null
    secondaryIds	        {"isin": "...", "cusip": "..."}	{} (Empty)
    metadata	            {"sector": "Tech"}	            {} (Empty)


    The default in the AssetIdentifier, primaryId and assetType will be in separate columns. But with MarketrIdentifier it has a secondaryIds of Map,
    We use JSONB for that

    Mapper will pull a row form the DB and performs a simple logic check to rebuild the proper sub class of AssetIdentifier.

    Now new thing, quick, because of this Single table approach, this table will need to include all the 'unique' variables form the
    AssetIdentifier subclasses
*/

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "assets")
public class AssetEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    // Discriminator for Reconstitution
    @Column(name = "identifier_type")
    private String identifierType; 

    // Identifier Data
    private String primaryId; // part of ALL, NOTE: cash Id gives only the Symbol as we can build the record from there

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> secondaryIds; // part of MI

    private String assetType; // part of AI
    private String name; // part of AI
    private String unitOfTrade; // part of MI, CI

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> metadata; // part of MI, CI

    // Financials
    private BigDecimal quantity;
    private BigDecimal costBasisAmount;
    private String costBasisCurrency;

    private Instant acquiredDate;
    
    @Version
    private Integer version;
}
