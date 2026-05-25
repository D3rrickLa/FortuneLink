
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