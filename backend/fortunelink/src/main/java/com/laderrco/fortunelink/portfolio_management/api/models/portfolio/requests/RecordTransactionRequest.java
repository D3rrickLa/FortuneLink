package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordTransactionRequest {
    @NotBlank(message = "Transaction type is required")
    String transactionType; // BUY, SELL, DEPOSIT, WITHDRAWAL, DIVIDEND, INTEREST, FEE

    @NotBlank(message = "Asset symbol is required")
    String symbol;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Quantity cannot be negative")
    BigDecimal quantity;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
    BigDecimal price;
    
    @NotNull(message = "Price currency is required")
    String priceCurrency;

    List<FeeRequest> fees;

    @NotNull(message = "Transaction date is required")
    LocalDateTime transactionDate;

    String notes; // Optional notes

    public RecordTransactionRequest(@NotBlank(message = "Transaction type is required") String transactionType,
            @NotBlank(message = "Asset symbol is required") String symbol,
            @NotNull(message = "Quantity is required") @DecimalMin(value = "0.0", inclusive = true, message = "Quantity cannot be negative") BigDecimal quantity,
            @NotNull(message = "Price is required") @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative") BigDecimal price,
            @NotNull(message = "Price currency is required") String priceCurrency, List<FeeRequest> fees,
            @NotNull(message = "Transaction date is required") LocalDateTime transactionDate, String notes) {
        this.transactionType = transactionType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.priceCurrency = priceCurrency;
        this.fees = fees;
        this.transactionDate = transactionDate;
        this.notes = notes;
    }

    
}
