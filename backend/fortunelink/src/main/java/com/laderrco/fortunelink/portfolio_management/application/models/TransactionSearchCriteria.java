package com.laderrco.fortunelink.portfolio_management.application.models;

import java.time.LocalDateTime;
import java.util.Set;

import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;

import lombok.Builder;

@Builder
public record TransactionSearchCriteria(
    PortfolioId portfolioId,
    AccountId accountId,
    TransactionType transactionType,
    LocalDateTime startDate,
    LocalDateTime endDate,
    Set<String> assetSymbols
) {

}