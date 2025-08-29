package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.CashTransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.CorporateActionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.ExpenseType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.IncomeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TradeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.TransactionAlreadyReversedException;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.CorrelationId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.ReversalTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.TransactionDetails;



// state management and lifecycle, NOT financial math
public class Transaction {
    private final TransactionId transactionId;
    private final TransactionId parentTransactionId;
    private final CorrelationId correlationId;
    private final PortfolioId portfolioId;

    private final TransactionType type; // this is an interface/abstract
    private final TransactionCategory category;
    private TransactionStatus status;
    private final TransactionDetails details;
    private final Instant transactionDate;

    private final Money transactionNetImpact;
    private boolean hidden;
    private int version;
    private final Instant createdAt;
    private Instant updatedAt;

    private final Map<String, String> metadata; // can be null 

    private static final Set<TransactionType> REVERSAL_TYPES = getAllReversalTypes();

    public Transaction(TransactionId transactionId, TransactionId parentTransactionId, CorrelationId correlationId,
            PortfolioId portfolioId, TransactionType type, TransactionStatus status, TransactionDetails details,
            Instant transactionDate, Money transactionNetImpact, boolean hidden, int version, Instant createdAt,
            Instant updatedAt, Map<String, String> metadata, TransactionCategory category) {
        this.transactionId = transactionId;
        this.parentTransactionId = parentTransactionId;
        this.correlationId = correlationId;
        this.portfolioId = portfolioId;
        this.type = type;
        this.status = status;
        this.category = category;
        this.details = details;
        this.transactionDate = transactionDate;
        this.transactionNetImpact = transactionNetImpact;
        this.hidden = hidden;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.metadata = metadata;
    }

    public Money getNetCostInPortfolioCurrency() {
        return details.calculateNetImpact(type);        
    }

    public boolean isReversal() {
        return REVERSAL_TYPES.contains(this.type);
    }

    public boolean isReversed() {
        return this.status == TransactionStatus.REVERSED;
    }

    public boolean canBeUpdated() {
        return Set.of(TransactionStatus.CANCELLED, TransactionStatus.FINALIZED).contains(this.status);
    }

    public void reverse(Transaction reversalTransaction, Instant reversedAt) {
        if (!canBeUpdated()) {
            throw new IllegalStateException("Cannot reverse a finalized or cancelled transaction.");
        }
        if (isReversed()) {
            throw new TransactionAlreadyReversedException("Transaction already reversed.");
        }

        Objects.requireNonNull(reversalTransaction, "Reversal transaction cannot be null.");
        Objects.requireNonNull(reversedAt, "Reversed at cannot be null.");

        if (!(reversalTransaction.getDetails() instanceof ReversalTransactionDetails reversalDetails)) {
            throw new IllegalArgumentException("Reversal transaction must carry ReversalTransactionDetails.");
        }

        if (!reversalDetails.getOriginalTransactionId().equals(this.transactionId)) {
            throw new IllegalArgumentException("Reversal details must point back to this transaction.");
        }

        this.status = TransactionStatus.REVERSED;
        updateVisibility(hidden, reversedAt);
    }

    public TransactionId getTransactionId() {
        return transactionId;
    }

    public TransactionId getParentTransactionId() {
        return parentTransactionId;
    }

    public CorrelationId getCorrelationId() {
        return correlationId;
    }

    public PortfolioId getPortfolioId() {
        return portfolioId;
    }

    public TransactionType getType() {
        return type;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public TransactionDetails getDetails() {
        return details;
    }

    public Instant getTransactionDate() {
        return transactionDate;
    }

    public Money getTransactionNetImpact() {
        return transactionNetImpact;
    }

    public boolean isHidden() {
        return hidden;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public static Set<TransactionType> getReversalTypes() {
        return REVERSAL_TYPES;
    }

    private static Set<TransactionType> getAllReversalTypes() {
        return Stream.of(CashTransactionType.values(), CorporateActionType.values(), ExpenseType.values(), IncomeType.values(), TradeType.values())
        .flatMap(Arrays::stream)
        .filter(TransactionType::isReversal)
        .collect(Collectors.toUnmodifiableSet());
    }

    private void updateVisibility(boolean hidden, Instant updatedAt) {
        updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null.");
        if (updatedAt.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("UpdatedAt cannot be null.");            
        }

        this.hidden = hidden;
        this.updatedAt = updatedAt;
        updateVersion();

    }

    private void updateTransaction() {
        updateUpdatedAt();
        updateVersion();
    }

    private void updateUpdatedAt() {
        this.updatedAt = Instant.now();
    }

    private void updateVersion() {
        this.version++;
    }

}
