package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transaction_fees")
@Getter @Setter
public class TransactionFeeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private FeeType feeType;

    private BigDecimal amount;
    private String currency;

    // Exchange Rate Components
    private BigDecimal rate;
    private String fromCurrency;
    private String toCurrency;
    private Instant exchangeRateDate; 
    private String rateSource; // renamed from 'source' to avoid SQL keywords
    
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> metadata;

    private Instant feeDate;
}