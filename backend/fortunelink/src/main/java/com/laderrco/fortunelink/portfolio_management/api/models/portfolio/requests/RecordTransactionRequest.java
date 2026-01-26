package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecordTransactionRequest {

    @NotBlank(message = "Transaction type is required")
    private String transactionType; // BUY, SELL, DEPOSIT, WITHDRAWAL, DIVIDEND, INTEREST, FEE

    // Asset-specific fields
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal price;
    private String priceCurrency;
    private List<FeeRequest> fees;

    // Always required
    @NotNull(message = "Transaction date is required")
    private LocalDateTime transactionDate;

    // Optional fields
    private String notes;
    private Boolean isDrip;
    private BigDecimal sharesReceived;

    public RecordTransactionRequest() {
    }

    public RecordTransactionRequest(String transactionType,
            String symbol,
            BigDecimal quantity,
            BigDecimal price,
            String priceCurrency,
            List<FeeRequest> fees,
            LocalDateTime transactionDate,
            String notes,
            Boolean isDrip,
            BigDecimal sharesReceived) {
        this.transactionType = transactionType;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.priceCurrency = priceCurrency;
        this.fees = fees;
        this.transactionDate = transactionDate;
        this.notes = notes;
        this.isDrip = isDrip;
        this.sharesReceived = sharesReceived;
    }

    /**
     * Helper to determine if this transaction involves an asset
     */
    public boolean isAssetTransaction() {
        return transactionType != null &&
                (transactionType.equalsIgnoreCase("BUY") ||
                        transactionType.equalsIgnoreCase("SELL") ||
                        transactionType.equalsIgnoreCase("DIVIDEND") ||
                        transactionType.equalsIgnoreCase("INTEREST") ||
                        transactionType.equalsIgnoreCase("FEE"));
    }

    /**
     * Helper to determine if this is cash-only (deposit/withdrawal)
     */
    public boolean isCashTransaction() {
        return transactionType != null &&
                (transactionType.equalsIgnoreCase("DEPOSIT") ||
                        transactionType.equalsIgnoreCase("WITHDRAWAL"));
    }

    /**
     * Optional: validate fields dynamically in assembler/service
     */
    public void validateFields() {
        if (isAssetTransaction()) {
            Objects.requireNonNull(symbol, "Asset symbol is required for asset transactions");
            Objects.requireNonNull(quantity, "Quantity is required for asset transactions");
            Objects.requireNonNull(price, "Price is required for asset transactions");
            Objects.requireNonNull(priceCurrency, "Price currency is required for asset transactions");
        }

        if (isCashTransaction()) {
            Objects.requireNonNull(quantity, "Amount is required for cash transactions");
            Objects.requireNonNull(priceCurrency, "Currency is required for cash transactions");
        }

        Objects.requireNonNull(transactionDate, "Transaction date is required");
    }
}
