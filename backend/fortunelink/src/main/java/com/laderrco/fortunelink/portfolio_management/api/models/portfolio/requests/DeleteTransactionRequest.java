package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeleteTransactionRequest {
    private String notes;
}
