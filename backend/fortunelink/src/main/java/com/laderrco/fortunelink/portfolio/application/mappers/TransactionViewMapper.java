package com.laderrco.fortunelink.portfolio.application.mappers;

import java.util.List;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;

@Component
public class TransactionViewMapper {

    public TransactionView toView(Transaction transaction) {
        return TransactionView.create(transaction);
    }

    public List<TransactionView> toViewList(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            return List.of();
        }

        return transactions.stream()
                .map(this::toView)
                .toList();
    }
}