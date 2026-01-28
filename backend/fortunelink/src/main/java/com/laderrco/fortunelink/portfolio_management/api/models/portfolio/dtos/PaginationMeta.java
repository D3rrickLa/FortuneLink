package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.dtos;

public record PaginationMeta(
        int pageNumber, // 1-based
        int pageSize,
        int totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {

}
