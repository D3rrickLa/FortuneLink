
---
### Use Case Checks
| Use Case | Status Before | Status After |
| :--- | :--- | :--- |
| **Record New Asset Purchase** | ✅ | ✅ |
| **Record Asset Sale** | ✅ | ✅ |
| **Record Deposit/Withdrawal** | ✅ | ✅ |
| **Record Income (Dividend/Interest)** | ✅ | ✅ |
| **View Net Worth** | ⚠️ (liabilities always zero) |✅ | 
| **View Portfolio Performance** | ❌ | Explicitly deferred |
| **Analyze Asset Allocation** | ❌ | Explicitly deferred |
| **Add New Liability** | ❌ | Explicitly deferred |
| **Record Liability Payment** | ❌ | Explicitly deferred |
| **Transaction History with filters** | ✅ (partial — date OR symbol, not both) | ✅ |
| **Realized Gains Report** | ❌ | ✅ |
| **CSV Import** | ❌ | ✅ |

### Completed

| Use Case | Status |
| :--- | :--- |
| **Realized Gains Report** | ✅ |
| **Redis resilience** | ✅ |
| **DRIP safety** | ✅ (warned, not blocked) |
| **Filter docs** | ✅ |
| **Net Worth disclaimer** | ✅ |
| **Loan Management** | Explicitly deferred |
| **CSV Import** | Architecture ready |
| **Performance Calculation** | Explicitly deferred |
| **Asset Allocation** | Explicitly deferred |

# Portfolio Tracker Development Roadmap

## 1. Critical Infrastructure (Application Blockers)
* [✅] **Implement REST Controllers**: Create the HTTP layer for `Portfolio`, `Account`, `Transaction`, and `MarketData` (symbol lookup).
* [✅] **Security Configuration**: Implement `@Configuration` class for Spring Security filter chain and JWT/OAuth2 resource server setup.
* [✅] **Global Exception Handling**: Create `@RestControllerAdvice` to map domain exceptions (`InsufficientFundsException`, etc.) to HTTP status codes.
* [✅] **External Service Implementations**:
    * [✅] `ExchangeRateService`: Implement Bank of Canada (BOC) integration for FX conversions.
    * [✅] `MarketDataProvider`: Implement concrete provider (e.g., FMP) for live price feeds.
* [✅] **Database Migration (V3)**: Add missing Flyway script for `PositionJpaEntity` (last_modified_at) to prevent startup failure.

## 2. Logic & Stability Fixes
* [✅] **Enable Spring Retry**: Add `@EnableRetry` to the main application/config class to activate `@Retryable` annotations.
* [*1] **Historical Import Support**: Add `enforceCashCheck` flag to `RecordPurchaseCommand` to allow buys without pre-existing cash balances during imports.
* [✅] **Business Logic Guards**: Replace `try-catch` database constraints in `PortfolioLifecycleService` with explicit `existsActiveByUserId()` checks.
* [✅] **JPA Performance**: Update `AccountJpaEntity` to implement `Persistable<UUID>` to avoid unnecessary `SELECT` calls before `INSERT`.
* [✅] **Redis Resilience**: Add fallback logic to `PositionRecalculationService` to handle Redis/Redisson outages gracefully.

## 3. Functional Gaps (Missing Features)
* [✅] **Realized Gains Reporting**: Implement `GetRealizedGainsQuery` and corresponding service/endpoint for tax reporting.
* [ ] **Performance Calculation**: Implement `PerformanceCalculationService` (Total Return, TWR, Unrealized Gains).
* [ ] **Asset Allocation**: Implement `AssetAllocationService` for breakdown by asset type, account, and currency.
* [✅] **CSV Import Engine**: Build parsing logic, bulk commands, and file upload endpoints.
* [✅] **Symbol Validation**: Expose a search endpoint to validate symbols before transaction submission.

## 4. Documentation & Cleanup
* [✅] **Stale Code Cleanup**: Remove/update "Bug 6" comments in `TransactionType.java`.
* [✅] **Net Worth Disclaimer**: Add `liabilitiesIncluded` flag to `NetWorthView` to warn users that Loan context is currently deferred.
* [✅] **Filter Improvements**: Update `GetTransactionHistoryQuery` to allow simultaneous filtering by Date AND Symbol.
* [✅] **DRIP Validation**: Implement a check to prevent duplicate recording of dividends and dividend reinvestments.


NOTE:
* 1 - we didn't add a 'flag', we in the `TransactionRecordingServiceImpl.java` used the account HealthStatus to verify ✅

---

### Implementation Progress

| Category | Priority | Status |
| :--- | :--- | :--- |
| **Infrastructure** | High (Blocker) | 🔴 Not Started |
| **Domain Logic** | High | 🟡 Partial |
| **New Features** | Medium | ❌ Missing |
| **Documentation** | Low | 🟡 Needs Cleanup |

---
### No Performance Calculation Service
PerformanceCalculationService appears in your domain diagram with calculateTotalReturn, calculateRealizedGains, calculateUnrealizedGains, calculateTimeWeightedReturn. None of these are implemented. "View Portfolio Performance" is a core domain use case in your documentation. This is not a future concern — it's table stakes for a portfolio tracker.

### No Asset Allocation Service
AssetAllocationService in your diagram with calculateAllocationByType, calculateAllocationByAccount, calculateAllocationByCurrency — none implemented. Your documentation explicitly says "Analyze Asset Allocation" as a use case.
No CSV Import Service
TransactionMetadata.csvImport() factory method exists, but there's no:

### CSV parsing service, Architecture Only
Bulk transaction command
File upload endpoint

Your documentation explicitly called this out as important for UX.

This is its own sprint. What you need when you get there:
```
application/
  commands/
    ImportTransactionsCommand.java     // accountId, userId, portfolioId, List<CsvRow>
  services/
    CsvImportService.java              // orchestrates parse + validate + execute
  
infrastructure/
  csv/
    CsvTransactionParser.java          // String -> List<CsvTransactionRow>
    CsvTransactionRow.java             // raw DTO matching CSV columns
    CsvImportResult.java               // per-row success/failure report
```


### No Symbol Search/Validation Endpoint
Before a user records a BUY, they need to search for AAPL or BTC-USD. MarketDataService.isSymbolSupported() and getAssetInfo() exist, but there's no exposed endpoint. The frontend has no way to validate a symbol or retrieve its metadata before submitting a transaction.

#### code example
```
@GetMapping("/{symbol}/validate")
    public ResponseEntity<AssetInfoResponse> validateSymbol(@PathVariable String symbol) {
        if (!marketDataService.isSymbolSupported(symbol)) {
            return ResponseEntity.notFound().build();
        }
        
        var info = marketDataService.getAssetInfo(symbol);
        return ResponseEntity.ok(new AssetInfoResponse(info));
    }
```

How to implement the other stuff
```
old useValuation code
"use client";

import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/client";
import { components } from "@/lib/api/schema";

// ─────────────────────────────────────────────────────────────────────────────
// Raw API types
// ─────────────────────────────────────────────────────────────────────────────

type RawValuationResponse =
  components["schemas"]["ValuationResponse"];

type RawSnapshotResponse =
  components["schemas"]["ValuationSnapshotResponse"];

// ─────────────────────────────────────────────────────────────────────────────
// Normalized UI types
// ─────────────────────────────────────────────────────────────────────────────

export interface ValuationState {
  totalValue: number;
  totalCostBasis: number;
  unrealizedGainLoss: number;
  returnPercentage: number;
  totalCashBalance: number;
  totalInvestedValue: number;
  currency: string;
  hasStaleData: boolean;

  isLoading: boolean;
  isError: boolean;
  isEmpty: boolean;
}

export interface ChartPoint {
  date: string;
  value: number;
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

function normalizeValuation(
  data?: RawValuationResponse | null
) {
  if (!data) {
    return {
      totalValue: 0,
      totalCostBasis: 0,
      unrealizedGainLoss: 0,
      returnPercentage: 0,
      totalCashBalance: 0,
      totalInvestedValue: 0,
      currency: "CAD",
      hasStaleData: false,
    };
  }

  return {
    totalValue: data.totalValue?.amount ?? 0,

    totalCostBasis:
      data.totalCostBasis?.amount ?? 0,

    unrealizedGainLoss:
      data.unrealizedGainLoss?.amount ?? 0,

    returnPercentage:
      data.gainLossPercent ?? 0,

    totalCashBalance:
      data.totalCashBalance?.amount ?? 0,

    totalInvestedValue:
      data.totalInvestedValue?.amount ?? 0,

    currency:
      data.currency ??
      data.totalValue?.currency ??
      "CAD",

    hasStaleData:
      data.hasStaleData ?? false,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// useValuation
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Global valuation:
 *   useValuation()
 *
 * Portfolio valuation:
 *   useValuation(portfolioId)
 */
export function useValuation(
  portfolioId?: string,
  enabled = true
): ValuationState {
  const isPortfolioQuery =
    !!portfolioId && portfolioId !== "all";

  const query = useQuery({
    queryKey: isPortfolioQuery
      ? ["portfolio-valuation", portfolioId]
      : ["valuation-summary"],

    queryFn: async (): Promise<RawValuationResponse | null> => {
      // Inside useValuation queryFn
      const response = isPortfolioQuery
        ? await apiClient.get(`/api/v1/valuations/${portfolioId}`)
        : await apiClient.get("/api/v1/valuations/summary");

      // 204 = empty portfolios
      if (response.status === 204) {
        return null;
      }

      if (response.status >= 400) {
        throw new Error("Failed to fetch valuation");
      }

      return response.data as RawValuationResponse;
    },

    enabled:
      enabled &&
      (!isPortfolioQuery || !!portfolioId),

    staleTime: 1000 * 60,
  });

  const normalized =
    normalizeValuation(query.data);

  return {
    ...normalized,

    isLoading: query.isLoading,

    isError: query.isError,

    isEmpty:
      !query.isLoading &&
      query.data === null,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// useValuationChart
// ─────────────────────────────────────────────────────────────────────────────

export function useValuationChart(days: number) {
  const query = useQuery({
    queryKey: ["valuation-history", days],

    queryFn: async (): Promise<
      RawSnapshotResponse[]
    > => {
      const response = await apiClient.get(
        "/api/v1/valuations/history",
        {
          params: {
            query: { days },
          },
        }
      );

      if (response.status >= 400) {
        throw new Error(
          "Failed to fetch valuation history"
        );
      }

      return response.data as RawSnapshotResponse[];
    },

    staleTime: 1000 * 60,
  });

  const points: ChartPoint[] =
    query.data?.map((snapshot) => ({
      date: snapshot.snapshotDate ?? "",

      value:
        Number(snapshot.totalValue) ?? 0,
    })) ?? [];

  return {
    points,

    isLoading: query.isLoading,

    isError: query.isError,

    isEmpty:
      !query.isLoading &&
      points.length === 0,
  };
}


@Transactional
public void snapshotForUser(UserId userId) {
  LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
  
  // 1. Calculate the fresh, live valuation right now (including any newly added accounts)
  ValuationView currentValuation = portfolioValuationService.computeLiveValuation(userId);

  // 2. Look to see if we already saved a snapshot for today
  Optional<ValuationSnapshot> existingSnapshot = 
      snapshotRepository.findByUserIdAndSnapshotDate(userId, todayUtc);

  if (existingSnapshot.isPresent()) {
    // 3. UPDATE: Overwrite the existing snapshot with the new, correct totals
    ValuationSnapshot snapshot = existingSnapshot.get();
    snapshot.updateValues(currentValuation); 
    snapshotRepository.save(snapshot);
    log.debug("Updated existing snapshot for userId={} to include recent changes.", userId);
  } else {
    // 4. INSERT: Brand new snapshot for the day
    ValuationSnapshot newSnapshot = ValuationSnapshot.createNew(userId, todayUtc, currentValuation);
    snapshotRepository.save(newSnapshot);
    log.debug("Created fresh daily snapshot for userId={}", userId);
  }
}

public ValuationView computeLiveValuation(UserId userId) {
    // 1. Fetch all portfolios containing their inner accounts for this specific user
    List<Portfolio> portfolios = portfolioRepository.findAllByUserIdWithDetails(userId);
    
    if (portfolios.isEmpty()) {
        // Fallback if the user has a completely blank profile with 0 portfolios
        Currency defaultCurrency = Currency.getInstance("CAD");
        return ValuationView.of(
            Money.zero(defaultCurrency), Money.zero(defaultCurrency), 
            Money.zero(defaultCurrency), Money.zero(defaultCurrency), 
            defaultCurrency, false, Instant.now()
        );
    }

    // 2. Select your top-level profile target currency (e.g., from the first portfolio)
    Currency targetCurrency = portfolios.get(0).getBaseCurrency();

    // 3. Gather every single unique AssetSymbol across ALL portfolios and ALL accounts
    Set<AssetSymbol> symbols = portfolios.stream()
        .flatMap(p -> p.getAccounts().stream())
        .filter(Account::isActive)
        .flatMap(a -> a.getPositionEntries().keySet().stream())
        .collect(Collectors.toSet());

    // 4. Batch-fetch the market prices in a single efficient I/O trip
    Map<AssetSymbol, MarketAssetQuote> quoteCache = marketDataService.getBatchQuotes(symbols);

    // 5. Delegate the complex financial aggregates directly to your service implementation
    return portfolioValuationService.calculateUserValuation(portfolios, targetCurrency, quoteCache);
}




"use client";

import { useMemo, useState } from "react";
import {
  LineChart, Line, XAxis, YAxis, Tooltip,
  ResponsiveContainer, CartesianGrid,
} from "recharts";
import { TrendingUp, TrendingDown, Clock } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useValuation,
  useAccountValuation,
  useValuationChart,
} from "@/features/portfolio/hooks/useValuations";

type TimePeriod = "1m" | "3m" | "ytd" | "all";

const PERIODS: { value: TimePeriod; label: string }[] = [
  { value: "1m", label: "1M" },
  { value: "3m", label: "3M" },
  { value: "ytd", label: "YTD" },
  { value: "all", label: "All" },
];

function getDays(period: TimePeriod): number {
  switch (period) {
    case "1m": return 30;
    case "3m": return 90;
    case "ytd": {
      const now = new Date();
      return Math.ceil(
        (now.getTime() - new Date(now.getFullYear(), 0, 1).getTime()) / 86_400_000
      );
    }
    case "all": return 365;
  }
}

function makeFmt(currency: string) {
  return (v: number) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
      minimumFractionDigits: 2,
    }).format(v ?? 0);
}

function makeCompactFmt(currency: string) {
  return (v: number) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
      notation: "compact",
      maximumFractionDigits: 1,
    }).format(v ?? 0);
}

function Metric({ label, value, highlight }: {
  label: string;
  value: string;
  highlight?: "gain" | "loss";
}) {
  return (
    <div className="rounded-lg bg-muted/40 px-3 py-2">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className={`mt-0.5 text-sm font-semibold tabular-nums ${highlight === "gain" ? "text-green-600"
        : highlight === "loss" ? "text-red-600"
          : "text-foreground"
        }`}>
        {value}
      </p>
    </div>
  );
}

interface PerformanceChartProps {
  portfolioId?: string | null;
  accountId?: string | null;
  currency?: string;
}

export function PerformanceChart({
  portfolioId,
  accountId,
  currency = "CAD",
}: PerformanceChartProps) {
  const [period, setPeriod] = useState<TimePeriod>("3m");
  const accountMode = !!accountId;
  const currentScope = {
    portfolioId: portfolioId ?? undefined,
    accountId: accountId ?? undefined,
  };

  const valuation = useValuation(currentScope, true);
  const accountValuation = useAccountValuation(
    portfolioId ?? undefined,
    accountId ?? undefined,
    accountMode
  );

  const current = accountMode ? accountValuation : valuation;

  // Both portfolio and account history now go through the same hook;
  // the URL is resolved inside useValuationChart based on what IDs are present.
  const history = useValuationChart(
    getDays(period),
    currentScope // Passes the scope object cleanly
  );

  const resolvedCurrency = current.currency ?? currency;
  const fmt = useMemo(() => makeFmt(resolvedCurrency), [resolvedCurrency]);
  const fmtCompact = useMemo(() => makeCompactFmt(resolvedCurrency), [resolvedCurrency]);

  const safeFmt = useMemo(() => (v: unknown) => {
    const num = typeof v === "number" ? v : Number(v);
    return Number.isFinite(num) ? fmt(num) : "";
  }, [fmt]);

  const safeCompactFmt = useMemo(() => (v: unknown) => {
    const num = typeof v === "number" ? v : Number(v);
    return Number.isFinite(num) ? fmtCompact(num) : "";
  }, [fmtCompact]);

  const chartData = useMemo(() => {
    if (!history.points || !Array.isArray(history.points)) return [];
    return history.points.filter(p => p && p.date && typeof p.value === "number");
  }, [history.points]);

  const metrics = useMemo(() => {
    if (chartData.length < 2) return null;
    const start = chartData[0]?.value;
    const end = chartData[chartData.length - 1]?.value;
    if (typeof start !== "number" || typeof end !== "number") return null;
    const change = end - start;
    const pct = start > 0 ? (change / start) * 100 : 0;
    return { change, pct, isPositive: change >= 0 };
  }, [chartData]);

  const strokeColor = useMemo(() => {
    if (!metrics) return "#6b7280";
    return metrics.isPositive ? "#16a34a" : "#dc2626";
  }, [metrics]);

  const isLoading = current.isLoading || history.isLoading;
  const isError = current.isError || history.isError;

  if (isLoading) {
    return (
      <Card>
        <CardHeader><CardTitle>Performance</CardTitle></CardHeader>
        <CardContent><Skeleton className="h-[300px] w-full" /></CardContent>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card>
        <CardHeader><CardTitle>Performance</CardTitle></CardHeader>
        <CardContent className="flex h-[300px] items-center justify-center">
          <p className="text-sm text-destructive">Failed to load performance history.</p>
        </CardContent>
      </Card>
    );
  }

  // Account-specific metrics panel shown above the chart regardless of history availability
  const accountMetrics = accountMode ? (
    <div className="grid grid-cols-2 gap-4 mb-4">
      <Metric label="Total Value" value={fmt(current.totalValue)} />
      <Metric label="Cost Basis" value={fmt(current.totalCostBasis)} />
      <Metric label="Cash Balance" value={fmt(current.totalCashBalance)} />
      <Metric label="Invested" value={fmt(current.totalInvestedValue)} />
      <Metric
        label="Unrealized P&L"
        value={`${current.unrealizedGainLoss >= 0 ? "+" : ""}${fmt(current.unrealizedGainLoss)}`}
        highlight={current.unrealizedGainLoss >= 0 ? "gain" : "loss"}
      />
      <Metric
        label="Return"
        value={`${current.returnPercentage >= 0 ? "+" : ""}${current.returnPercentage.toFixed(2)}%`}
        highlight={current.returnPercentage >= 0 ? "gain" : "loss"}
      />
    </div>
  ) : null;

  if (chartData.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>{accountMode ? "Account Performance" : "Performance"}</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {accountMetrics}
          <div className="flex h-[200px] flex-col items-center justify-center gap-3 text-center">
            <Clock className="h-8 w-8 text-muted-foreground opacity-40" />
            <div className="space-y-1">
              <p className="text-sm font-medium">No history yet</p>
              <p className="max-w-[240px] text-xs text-muted-foreground">
                Valuation snapshots are recorded daily. Check back tomorrow once
                your first snapshot has been captured.
              </p>
            </div>
            <p className="text-base font-semibold">Current: {fmt(current.totalValue)}</p>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <CardTitle>{accountMode ? "Account Performance" : "Performance"}</CardTitle>
            <div className={`flex items-center gap-2 text-sm font-semibold ${current.unrealizedGainLoss >= 0 ? "text-green-600" : "text-red-600"
              }`}>
              {current.unrealizedGainLoss >= 0
                ? <TrendingUp className="h-4 w-4" />
                : <TrendingDown className="h-4 w-4" />}
              <span>
                {current.unrealizedGainLoss >= 0 ? "+" : ""}
                {fmt(current.unrealizedGainLoss)}
              </span>
              <span className="font-normal text-muted-foreground">
                ({current.returnPercentage >= 0 ? "+" : ""}
                {current.returnPercentage.toFixed(2)}%)
              </span>
            </div>
            <p className="text-xs text-muted-foreground">
              {accountMode ? "Account net value" : "Total net worth"}
            </p>
          </div>
          <div className="flex gap-1">
            {PERIODS.map((p) => (
              <button
                key={p.value}
                onClick={() => setPeriod(p.value)}
                className={`rounded-md px-3 py-1 text-xs font-medium transition-colors ${period === p.value
                  ? "bg-primary text-primary-foreground"
                  : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                  }`}
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {accountMetrics}
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData} margin={{ top: 4, right: 8, left: 8, bottom: 4 }}>
            <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
            <XAxis
              dataKey="date"
              tick={{ fontSize: 11 }}
              tickFormatter={(iso: string) =>
                new Date(iso).toLocaleDateString("en-CA", {
                  month: "short",
                  day: "numeric",
                })
              }
              minTickGap={40}
            />
            <YAxis
              tick={{ fontSize: 11 }}
              tickFormatter={(v) => safeCompactFmt(v)}
              width={72}
            />
            <Tooltip
              labelFormatter={(label: any) =>
                new Date(label).toLocaleDateString("en-CA", {
                  year: "numeric",
                  month: "short",
                  day: "numeric",
                })
              }
              formatter={(value: any) => {
                const num = typeof value === "number" ? value : Number(value);
                return [Number.isFinite(num) ? fmt(num) : "", "Value"];
              }}
            />
            <Line
              type="monotone"
              dataKey="value"
              stroke={strokeColor}
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 4 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </CardContent>
    </Card>
  );
}