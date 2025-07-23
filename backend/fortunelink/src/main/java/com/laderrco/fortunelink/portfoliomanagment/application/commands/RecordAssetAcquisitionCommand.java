package com.laderrco.fortunelink.portfoliomanagment.application.commands;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;

// from the source directly, awe do the currency conversion in the PortfolioApplicationService.java  
public record RecordAssetAcquisitionCommand(
    UUID portfolioId,
    AssetIdentifier assetIdentifier,
    BigDecimal quantity,
    Money purchasePrice,
    Instant transactionDate,
    List<Money> fees,
    TransactionMetadata transactionMetadata
) {
    
}
