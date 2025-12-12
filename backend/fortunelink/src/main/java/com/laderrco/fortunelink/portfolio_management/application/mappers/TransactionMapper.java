package com.laderrco.fortunelink.portfolio_management.application.mappers;

import java.util.List;

import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;

public class TransactionMapper {
    public static TransactionResponse toResponse(Transaction transaction, MarketAssetInfo assetInfo) {
        return null;
    }

    public List<TransactionResponse> toResponseList(List<Transaction> transactions) {
        return null;
    }
}
