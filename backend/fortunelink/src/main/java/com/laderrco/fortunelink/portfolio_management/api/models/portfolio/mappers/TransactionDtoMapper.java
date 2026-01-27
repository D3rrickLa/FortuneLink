package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.dtos.DateRangeResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.dtos.PaginationMeta;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PagedTransactionHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.TransactionHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionHistoryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.shared.valueobjects.Money;

/**
 * Maps between Transaction domain model and API DTOs.
 */
@Component
public class TransactionDtoMapper {

    public TransactionHttpResponse toResponse(String accountId, TransactionView transactionView) {
        BigDecimal fee = transactionView.fees() == null
                ? BigDecimal.ZERO
                : transactionView.fees().stream()
                        .map(f -> f.toBaseCurrency(transactionView.price().currency()))
                        .map(Money::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionHttpResponse(
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
                LocalDateTime.now());
    }

    public PagedTransactionHttpResponse toPagedResponse(String accountId, TransactionHistoryView transactionHistoryView) {
        PaginationMeta meta = new PaginationMeta(
                transactionHistoryView.pageNumber(),
                transactionHistoryView.pageSize(),
                transactionHistoryView.totalElements(),
                transactionHistoryView.totalPages(),
                transactionHistoryView.hasNext(),
                transactionHistoryView.hasPrevious());

        DateRangeResponse dateRange = new DateRangeResponse(
                transactionHistoryView.dateRange().startDate(),
                transactionHistoryView.dateRange().endDate());

        return new PagedTransactionHttpResponse(
                transactionHistoryView.transactions().stream()
                        .map(t -> toResponse(accountId, t))
                        .toList(),
                meta,
                dateRange);
    }

    private LocalDateTime toLocalDateTime(Instant date) {
        return LocalDateTime.ofInstant(date, ZoneId.systemDefault());
    }
}