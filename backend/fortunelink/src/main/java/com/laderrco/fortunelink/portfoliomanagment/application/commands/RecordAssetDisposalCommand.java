package com.laderrco.fortunelink.portfoliomanagment.application.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;

public record RecordAssetDisposalCommand(
    UUID portfolioId,
    AssetIdentifier assetIdentifier,
    BigDecimal quantity,
    Money salePrice, // in native currency
    Instant transactionDate,
    List<Fee> fees,
    TransactionMetadata transactionMetadata
) {
    
}
