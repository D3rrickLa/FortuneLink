package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.CashTransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TradeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionCancelledEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionCompletedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionFailedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionReversedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.IllegalStatusTransitionException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.TransactionAlreadyReversedException;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.CorrelationId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.AccountTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.ReversalTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.TradeTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects.TransactionDetails;



// state management and lifecycle, NOT financial math
/*
 * this to do
 * missing domain events
 * factory methods for buy, sell, cashflow,
 * canceling logic
 * design issue - immutable vs mutable 
 * need to determine if tanx type and status are fully immutable or what can be changed
 * validation
 * builder pattern as 14 parameters is stuipdly large for a constructor
 * category vs type confuction -> seems redundant
 * 
 * other business logic
 * canBeREversed
 * getAbsolutoAmoutn
 * isIncome
 * isExpense
 */
public class Transaction {
    private final TransactionId transactionId;
    private final TransactionId parentTransactionId;
    private final CorrelationId correlationId;
    private final PortfolioId portfolioId;
    private final TransactionType type;
    private final TransactionDetails details;
    private final Instant transactionDate;
    private final MonetaryAmount transactionAmount; // OG currency + conversion
    private final Instant createdAt;
    private final Map<String, String> metadata;

    private TransactionStatus status;
    private boolean hidden;
    private int version;
    private Instant updatedAt;

    private final List<DomainEvent> domainEvents;
    
    // valid status of a transaction
    private static final Map<TransactionStatus, Set<TransactionStatus>> VALID_TRANSITIONS = Map.of(
        TransactionStatus.PENDING, Set.of(TransactionStatus.COMPLETED, TransactionStatus.CANCELLED, TransactionStatus.PENDING),
        TransactionStatus.COMPLETED, Set.of(TransactionStatus.REVERSED),
        TransactionStatus.CANCELLED, Set.of(),
        TransactionStatus.FAILED, Set.of(),
        TransactionStatus.REVERSED, Set.of()
    );

    private Transaction(
        TransactionId transactionId,
        TransactionId parentTransactionId,
        CorrelationId correlationId,
        PortfolioId portfolioId,
        TransactionType type,
        TransactionStatus status,
        TransactionDetails details,
        Instant transactionDate,
        MonetaryAmount transactionAmount,
        boolean hidden,
        int version,
        Instant createdAt,
        Instant updatedAt,
        Map<String, String> metadata
    ) {
        this.transactionId = Objects.requireNonNull(transactionId, "Transaction id cannot be null.");
        this.portfolioId = Objects.requireNonNull(portfolioId, "Portfolio id cannot be null.");
        this.type = Objects.requireNonNull(type, "Transaction type cannot be null.");
        this.details = Objects.requireNonNull(details, "Transaction details cannot be null.");
        this.transactionDate = Objects.requireNonNull(transactionDate, "Transaction date cannot be null.");
        this.transactionAmount = Objects.requireNonNull(transactionAmount, "Transaction amount cannot be null.");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null.");
        this.status = Objects.requireNonNull(status, "Transaction status cannot be null.");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null.");

        validateTransactionDate(transactionDate);
        validateAmount(transactionAmount.nativeAmount());

        this.parentTransactionId = parentTransactionId;
        this.correlationId = correlationId;
        this.hidden = hidden;
        this.version = version;
        this.metadata = metadata != null ? Map.copyOf(metadata) : null;
        this.domainEvents = new ArrayList<>();
    }

    /*
     * reverse(String reason): void
     * isReversed(): boolean
     * canBeReversed(): boolean
     */

    // Factory Methods
    public static Transaction createTradeTransaction(PortfolioId portfolioId, TradeType tradeType, 
                                                   TradeTransactionDetails details, MonetaryAmount netImpact, Instant transactionDate) {
        return createTransaction(portfolioId, tradeType, details, netImpact, transactionDate, null);
    }

    public static Transaction createCashTransaction(PortfolioId portfolioId, CashTransactionType cashType, 
                                                  AccountTransactionDetails details, MonetaryAmount netImpact, Instant transactionDate) {
        return createTransaction(portfolioId, cashType, details, netImpact, transactionDate, null);
    }

    public static Transaction createReversalTransaction(PortfolioId portfolioId, TransactionType reversalType, 
                                                      ReversalTransactionDetails details, MonetaryAmount netImpact, 
                                                      Instant transactionDate, TransactionId parentTransactionId) {
        if (!reversalType.isReversal()) {
            throw new IllegalArgumentException("Transaction type must be a reversal type");
        }
        return createTransaction(portfolioId, reversalType, details, netImpact, transactionDate, parentTransactionId);
    }

    private static Transaction createTransaction(PortfolioId portfolioId, TransactionType type, 
                                               TransactionDetails details, MonetaryAmount netImpact, 
                                               Instant transactionDate, TransactionId parentTransactionId) {
        Instant now = Instant.now();
        return new Transaction(
            TransactionId.createRandom(),
            parentTransactionId,
            CorrelationId.createRandom(),
            portfolioId,
            type,
            TransactionStatus.COMPLETED,
            details,
            transactionDate,
            netImpact,
            false,
            0,
            Instant.now(),
            now,
            new HashMap<String, String>()
        );
    }
    // Business Logic Methods
    public Money getNetCostInPortfolioCurrency() {
        return details.calculateNetImpact(type);
    }

    public void markAsCompleted() {
        validateStatusTransition(TransactionStatus.COMPLETED);
        this.status = TransactionStatus.COMPLETED;
        addDomainEvent(new TransactionCompletedEvent(this.transactionId, this.portfolioId, this.type));
        updateTransaction();
    }

    public void cancel(String reason) {
        validateStatusTransition(TransactionStatus.CANCELLED);
        this.status = TransactionStatus.CANCELLED;
        addDomainEvent(new TransactionCancelledEvent(this.transactionId, this.portfolioId, reason));
        updateTransaction();
    }

    public void fail(String reason) {
        validateStatusTransition(TransactionStatus.FAILED);
        this.status = TransactionStatus.FAILED;
        addDomainEvent(new TransactionFailedEvent(this.transactionId, this.portfolioId, reason));
        updateTransaction();
    }

    public void reverse(Transaction reversalTransaction, Instant reversedAt) {
        if (!canBeReversed()) {
            throw new IllegalStateException("Transaction cannot be reversed in current state");
        }
        if (isReversed()) {
            throw new TransactionAlreadyReversedException("Transaction already reversed");
        }

        Objects.requireNonNull(reversalTransaction, "Reversal transaction cannot be null");
        Objects.requireNonNull(reversedAt, "Reversed at cannot be null");

        if (!(reversalTransaction.getDetails() instanceof ReversalTransactionDetails reversalDetails)) {
            throw new IllegalArgumentException("Reversal transaction must carry ReversalTransactionDetails");
        }

        if (!reversalDetails.getOriginalTransactionId().equals(this.transactionId)) {
            throw new IllegalArgumentException("Reversal details must point back to this transaction");
        }

        if (!reversalTransaction.getType().equals(this.type.getReversalType())) {
            throw new IllegalArgumentException("Reversal transaction type must match expected reversal type");
        }

        this.status = TransactionStatus.REVERSED;
        addDomainEvent(new TransactionReversedEvent(this.transactionId, this.portfolioId, reversalTransaction.getTransactionId()));
        updateVisibility(hidden, reversedAt);
    }

    // Query Methods
    public boolean isReversal() {
        return type.isReversal();
    }

    public boolean isReversed() {
        return this.status == TransactionStatus.REVERSED;
    }

    public boolean canBeReversed() {
        return status == TransactionStatus.COMPLETED && 
               !isReversal() && 
               !isReversed();
    }

    public boolean canBeUpdated() {
        return !Set.of(TransactionStatus.CANCELLED, TransactionStatus.FINALIZED).contains(this.status);
    }

    public boolean isIncome() {
        return type.getCategory() == TransactionCategory.INCOME;
    }

    public boolean isExpense() {
        return type.getCategory() == TransactionCategory.EXPENSE;
    }

    public Money getAbsoluteAmount() {
        return transactionAmount.nativeAmount().abs();
    }

    public TransactionCategory getCategory() {
        return type.getCategory();
    }

    // Domain Events
    public void addDomainEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void markEventsAsCommitted() {
        this.domainEvents.clear();
    }

    // Getters
    public TransactionId getTransactionId() { return transactionId; }
    public TransactionId getParentTransactionId() { return parentTransactionId; }
    public CorrelationId getCorrelationId() { return correlationId; }
    public PortfolioId getPortfolioId() { return portfolioId; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public TransactionDetails getDetails() { return details; }
    public Instant getTransactionDate() { return transactionDate; }
    public MonetaryAmount getTransactionAmount() { return transactionAmount; }
    public boolean isHidden() { return hidden; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Map<String, String> getMetadata() { return metadata; }
    

    private void validateStatusTransition(TransactionStatus newStatus) {
        Set<TransactionStatus> allowedTransitions = VALID_TRANSITIONS.get(this.status);
        if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
            throw new IllegalStatusTransitionException(String.format("%s %s", this.status, newStatus));
        }
    }

    private void validateTransactionDate(Instant transactionDate) {
        Instant now = Instant.now();
        if (transactionDate.isAfter(now.plusSeconds(300))) { // Allow 5 minute future tolerance
            throw new IllegalArgumentException("Transaction date cannot be significantly in the future");
        }
    }

    private void validateAmount(Money amount) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("Transaction amount cannot be zero");
        }
    }

    private void updateVisibility(boolean hidden, Instant updatedAt) {
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");
        if (updatedAt.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("UpdatedAt cannot be before current updatedAt");
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
