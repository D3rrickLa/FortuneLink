package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.TransactionResponse;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.shared.valueobjects.Money;

/**
 * Maps between Transaction domain model and API DTOs.
 */
@Component
public class TransactionDtoMapper {
    
    public TransactionResponse toResponse(String accountId, TransactionView transactionView) {
        BigDecimal fee = transactionView.fees() == null
        ? BigDecimal.ZERO
        : transactionView.fees().stream()
            .map(f -> f.toBaseCurrency(transactionView.price().currency()))
            .map(Money::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionResponse(
            transactionView.transactionId().transactionId().toString(),
            accountId,
            transactionView.type().name(),
            transactionView.symbol(),
            transactionView.quantity(),
            transactionView.price().amount(),
            transactionView.price().currency().getCode(),
            fee,
            transactionView.totalCost().amount(),
            transactionView.calculateNetAmount().amount(),
            toLocalDateTime(transactionView.date()),
            transactionView.notes(),
            LocalDateTime.now()
        );
    }

    private LocalDateTime toLocalDateTime(Instant date) {
        return LocalDateTime.ofInstant(date, ZoneId.systemDefault());
    }
}