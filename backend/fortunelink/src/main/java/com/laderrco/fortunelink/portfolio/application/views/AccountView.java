package com.laderrco.fortunelink.portfolio.application.views;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import java.time.Instant;
import java.util.List;

public record AccountView(
    AccountId accountId,
    String name,
    AccountType type,
    AccountLifecycleState status,
    List<PositionView> assets,
    Currency baseCurrency,
    Money cashBalance,
    Money totalValue,
    Instant creationDate,

    /**
     * True when one or more BUY/SELL/income transactions have been excluded
     * from position calculations. The cash balance reflects actual cash movement
     * (because we don't reverse it on exclusion), but position state was
     * recalculated without those transactions.
     *
     * <p>
     * This flag does NOT indicate data corruption — it's intentional design.
     * The frontend should display a contextual warning when this is true so users
     * understand their shown cash balance may not reconcile against their
     * positions.
     *
     * <p>
     * This is false on the list/summary view (getAllAccounts) to avoid the extra
     * query on every portfolio page load. Only populated on the detail view
     * (getAccountSummary).
     */
    boolean hasCashImbalance,

    /**
     * Count of excluded transactions that affect positions or income.
     * Shown alongside hasCashImbalance so the frontend can render
     * "2 transactions excluded — cash balance may not reconcile" rather than
     * a generic warning.
     */
    int excludedTransactionCount) {
}