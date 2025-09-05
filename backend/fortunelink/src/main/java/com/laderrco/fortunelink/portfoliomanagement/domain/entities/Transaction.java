package com.laderrco.fortunelink.portfoliomanagement.domain.entities;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionCategory;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.CashTransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.ExpenseType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.IncomeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TradeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.type.TransactionType;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.DomainEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionCancelledEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionCompletedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionFailedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.events.TransactionReversedEvent;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidReversalTransactionException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidTransactionAmountException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidTransactionDateException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidTransactionStateException;
import com.laderrco.fortunelink.portfoliomanagement.domain.exceptions.InvalidTransactionTypeException;
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
 * getAbsolutoAmount
 * isIncome
 * isExpense
 */
public class Transaction {
    // IMMUTABLE CORE ATTRIBUTES
    private final TransactionId transactionId;
    private final TransactionId parentTransactionId;
    private final CorrelationId correlationId;
    private final PortfolioId portfolioId;
    private final TransactionType type;
    private final TransactionDetails details;
    private final Instant transactionDate;
    private final MonetaryAmount transactionNetImpact;
    private final Instant createdAt;
    private final Map<String, String> metadata;

    // MUTABLE LIFECYCLE STATE //
    private TransactionStatus status;
    private boolean hidden;
    private int version;
    private Instant updatedAt;

    // DOMAIN EVENTS //
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // VALID STATE TRANSITIONS //
    private static final Map<TransactionStatus, Set<TransactionStatus>> VALID_TRANSITIONS = Map.of(
        TransactionStatus.PENDING, Set.of(TransactionStatus.COMPLETED, TransactionStatus.CANCELLED, TransactionStatus.FAILED),
        TransactionStatus.COMPLETED, Set.of(TransactionStatus.REVERSED),
        TransactionStatus.CANCELLED, Set.of(),
        TransactionStatus.FAILED, Set.of(),
        TransactionStatus.REVERSED, Set.of()
    );

    // CONSTRUCTOR (Private - use factory methods)
    
    private Transaction(Builder builder) {
        // Validate required fields
        this.transactionId = Objects.requireNonNull(builder.transactionId, "Transaction ID cannot be null");
        this.portfolioId = Objects.requireNonNull(builder.portfolioId, "Portfolio ID cannot be null");
        this.type = Objects.requireNonNull(builder.type, "Transaction type cannot be null");
        this.status = Objects.requireNonNull(builder.status, "Transaction status cannot be null");
        this.details = Objects.requireNonNull(builder.details, "Transaction details cannot be null");
        this.transactionDate = Objects.requireNonNull(builder.transactionDate, "Transaction date cannot be null");
        this.transactionNetImpact = Objects.requireNonNull(builder.transactionNetImpact, "Net impact cannot be null");
        this.createdAt = Objects.requireNonNull(builder.createdAt, "Created at cannot be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "Updated at cannot be null");
        
        // Validate business rules
        TransactionValidator.validateDate(builder.transactionDate);
        TransactionValidator.validateAmount(builder.transactionNetImpact.nativeAmount());
        
        // Set optional fields
        this.parentTransactionId = builder.parentTransactionId;
        this.correlationId = builder.correlationId;
        this.hidden = builder.hidden;
        this.version = builder.version;
        this.metadata = builder.metadata != null ? Map.copyOf(builder.metadata) : null;
    }

    // FACTORY METHODS //

    public static Transaction createTradeTransaction(
        PortfolioId portfolioId,
        TradeType tradeType,
        TradeTransactionDetails details,
        MonetaryAmount netImpact,
        Instant transactionDate
    ) {
        return new Builder()
            .portfolioId(portfolioId)
            .type(tradeType)
            .details(details)
            .netImpact(netImpact)
            .transactionDate(transactionDate)
            .build();
    }

    public static Transaction createCashTransaction(
        PortfolioId portfolioId,
        CashTransactionType cashType,
        AccountTransactionDetails details,
        MonetaryAmount netImpact,
        Instant transactionDate
    ) {
        return new Builder()
            .portfolioId(portfolioId)
            .type(cashType)
            .details(details)
            .netImpact(netImpact)
            .transactionDate(transactionDate)
            .build();
    }

    public static Transaction createIncomeTransaction(
        PortfolioId portfolioId,
        IncomeType incomeType,
        AccountTransactionDetails details,
        MonetaryAmount netImpact,
        Instant transactionDate
    ) {
        return new Builder()
            .portfolioId(portfolioId)
            .type(incomeType)
            .details(details)
            .netImpact(netImpact)
            .transactionDate(transactionDate)
            .build();
    }

    public static Transaction createExpenseTransaction(
        PortfolioId portfolioId,
        ExpenseType expenseType,
        AccountTransactionDetails details,
        MonetaryAmount netImpact,
        Instant transactionDate
    ) {
        return new Builder()
            .portfolioId(portfolioId)
            .type(expenseType)
            .details(details)
            .netImpact(netImpact)
            .transactionDate(transactionDate)
            .build();
    }

    public static Transaction createReversalTransaction(
        PortfolioId portfolioId,
        TransactionId parentTransactionId,
        TransactionType reversalType,
        ReversalTransactionDetails details,
        MonetaryAmount netImpact,
        Instant transactionDate
    ) {
        if (!reversalType.isReversal()) {
            throw new InvalidTransactionTypeException("Transaction type must be a reversal type: " + reversalType);
        }
        
        return new Builder()
            .portfolioId(portfolioId)
            .parentTransactionId(parentTransactionId)
            .type(reversalType)
            .details(details)
            .netImpact(netImpact)
            .transactionDate(transactionDate)
            .build();
    }

    // =============================================
    // BUSINESS OPERATIONS
    // =============================================

    public void markAsCompleted() {
        validateStatusTransition(TransactionStatus.COMPLETED);
        this.status = TransactionStatus.COMPLETED;
        addDomainEvent(new TransactionCompletedEvent(this.transactionId, this.portfolioId, this.type));
        updateTransaction();
    }

    public void cancel(String reason) {
        Objects.requireNonNull(reason, "Cancellation reason cannot be null");
        validateStatusTransition(TransactionStatus.CANCELLED);
        this.status = TransactionStatus.CANCELLED;
        addDomainEvent(new TransactionCancelledEvent(this.transactionId, this.portfolioId, reason));
        updateTransaction();
    }

    public void fail(String reason) {
        Objects.requireNonNull(reason, "Failure reason cannot be null");
        validateStatusTransition(TransactionStatus.FAILED);
        this.status = TransactionStatus.FAILED;
        addDomainEvent(new TransactionFailedEvent(this.transactionId, this.portfolioId, reason));
        updateTransaction();
    }

    public void reverse(Transaction reversalTransaction, Instant reversedAt) {
        Objects.requireNonNull(reversalTransaction, "Reversal transaction cannot be null");
        Objects.requireNonNull(reversedAt, "Reversed at cannot be null");
        if (isReversed()) {
            throw new TransactionAlreadyReversedException("Transaction is already reversed");
        }

        if (!canBeReversed()) {
            throw new InvalidTransactionStateException("Transaction cannot be reversed in current state: " + this.status);
        }

        ReversalValidator.validateReversalTransaction(this, reversalTransaction);

        this.status = TransactionStatus.REVERSED;
        addDomainEvent(new TransactionReversedEvent(this.transactionId, this.portfolioId, 
                                                   reversalTransaction.getTransactionId()));
        updateVisibility(true, reversedAt);
    }

    public void hide() {
        if (!canBeUpdated()) {
            throw new InvalidTransactionStateException("Cannot hide transaction in state: " + this.status);
        }
        updateVisibility(true, Instant.now());
    }

    public void show() {
        updateVisibility(false, Instant.now());
    }

    // =============================================
    // QUERY METHODS
    // =============================================

    public Money getNetCostInPortfolioCurrency() {
        return this.transactionNetImpact.getPortfolioAmount();
    }

    public Money getAbsoluteAmount() {
        return this.transactionNetImpact.nativeAmount().abs();
    }

    public TransactionCategory getCategory() {
        return type.getCategory();
    }

    public boolean isReversal() {
        return type.isReversal();
    }

    public boolean isReversed() {
        return this.status == TransactionStatus.REVERSED;
    }

    public boolean canBeReversed() {
        return this.status == TransactionStatus.COMPLETED && !isReversal();    
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

    public boolean isPending() {
        return this.status == TransactionStatus.PENDING;
    }

    public boolean isCompleted() {
        return this.status == TransactionStatus.COMPLETED;
    }

    public boolean affectsPortfolioPerformance() {
        return !isReversal() && (isCompleted() || status == TransactionStatus.REVERSED);
    }

    // DOMAIN EVENTS //

    public void addDomainEvent(DomainEvent event) {
        Objects.requireNonNull(event, "Domain event cannot be null");
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getUncommittedEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void markEventsAsCommitted() {
        this.domainEvents.clear();
        updateUpdatedAt();
    }

    public boolean hasUncommittedEvents() {
        return !domainEvents.isEmpty();
    }

    // GETTERS //

    public TransactionId getTransactionId() { return transactionId; }
    public TransactionId getParentTransactionId() { return parentTransactionId; }
    public CorrelationId getCorrelationId() { return correlationId; }
    public PortfolioId getPortfolioId() { return portfolioId; }
    public TransactionType getType() { return type; }
    public TransactionDetails getDetails() { return details; }
    public Instant getTransactionDate() { return transactionDate; }
    public MonetaryAmount getTransactionNetImpact() { return transactionNetImpact; }
    public Instant getCreatedAt() { return createdAt; }
    public Map<String, String> getMetadata() { 
        return metadata != null ? Collections.unmodifiableMap(metadata) : null; 
    }
    public TransactionStatus getStatus() { return status; }
    public boolean isHidden() { return hidden; }
    public int getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }

    // PRIVATE HELPER METHODS //

    private void validateStatusTransition(TransactionStatus newStatus) {
        Set<TransactionStatus> allowedTransitions = VALID_TRANSITIONS.get(this.status);
        if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
            throw new InvalidTransactionStateException(this.status, newStatus);
        }
    }

    private void updateVisibility(boolean hidden, Instant updatedAt) {
        Objects.requireNonNull(updatedAt, "Updated at cannot be null");

        if (updatedAt.isBefore(this.updatedAt)) {
            throw new IllegalArgumentException("New UpdatedAt cannot be before current updatedAt.");
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

    // BUILDER PATTERN //

    public static class Builder {
        private TransactionId transactionId;
        private TransactionId parentTransactionId;
        private CorrelationId correlationId;
        private PortfolioId portfolioId;
        private TransactionType type;
        private TransactionStatus status = TransactionStatus.PENDING;
        private TransactionDetails details;
        private Instant transactionDate;
        private MonetaryAmount transactionNetImpact;
        private boolean hidden = false;
        private int version = 0;
        private Instant createdAt;
        private Instant updatedAt;
        private Map<String, String> metadata;

        public Builder() {
            Instant now = Instant.now();
            this.transactionId = TransactionId.createRandom();
            this.correlationId = CorrelationId.createRandom();
            this.createdAt = now;
            this.updatedAt = now;
        }

        public Builder transactionId(TransactionId transactionId) {
            this.transactionId = transactionId;
            return this;
        }

        public Builder parentTransactionId(TransactionId parentTransactionId) {
            this.parentTransactionId = parentTransactionId;
            return this;
        }

        public Builder correlationId(CorrelationId correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder portfolioId(PortfolioId portfolioId) {
            this.portfolioId = portfolioId;
            return this;
        }

        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder status(TransactionStatus status) {
            this.status = status;
            return this;
        }

        public Builder details(TransactionDetails details) {
            this.details = details;
            return this;
        }

        public Builder transactionDate(Instant transactionDate) {
            this.transactionDate = transactionDate;
            return this;
        }

        public Builder netImpact(MonetaryAmount netImpact) {
            this.transactionNetImpact = netImpact;
            return this;
        }

        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Transaction build() {
            return new Transaction(this);
        }
    }

    // VALIDATION HELPER CLASS //

    private static class TransactionValidator {
        private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(5);

        static void validateDate(Instant transactionDate) {
            Objects.requireNonNull(transactionDate, "Transaction date cannot be null");
            
            Instant now = Instant.now();
            if (transactionDate.isAfter(now.plus(FUTURE_TOLERANCE))) {
                throw new InvalidTransactionDateException("Transaction date cannot be significantly in the future: " + transactionDate);
            }
        }

        static void validateAmount(Money amount) {
            Objects.requireNonNull(amount, "Transaction amount cannot be null");
            
            if (amount.isZero()) {
                throw new InvalidTransactionAmountException("Transaction amount cannot be zero.");
            }
        }
    }

    // REVERSAL VALIDATION HELPER CLASS //

    private static class ReversalValidator {
        static void validateReversalTransaction(Transaction original, Transaction reversal) {
            if (!(reversal.getDetails() instanceof ReversalTransactionDetails reversalDetails)) {
                throw new InvalidReversalTransactionException("Reversal transaction must carry ReversalTransactionDetails.");
            }

            if (!reversalDetails.getOriginalTransactionId().equals(original.transactionId)) {
                throw new InvalidReversalTransactionException("Reversal details must point back to original transaction.");
            }

            if (!reversal.getType().equals(original.type.getReversalType())) {
                throw new InvalidReversalTransactionException("Reversal transaction type must match expected reversal type.");
            }

            if (!reversal.getPortfolioId().equals(original.portfolioId)) {
                throw new InvalidReversalTransactionException("Reversal transaction must belong to same portfolio.");
            }
        }
    }

    // OBJECT METHODS //

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(transactionId, that.transactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }

    @Override
    public String toString() {
        return "Transaction{" +
            "id=" + transactionId +
            ", type=" + type +
            ", status=" + status +
            ", amount=" + transactionNetImpact +
            ", date=" + transactionDate +
            ", portfolio=" + portfolioId +
            '}';
    }
}
