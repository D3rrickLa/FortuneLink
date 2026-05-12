/**
 * Hook layer for valuation data.
 *
 * Follows the same pattern as hooks/usePortfolios.ts:
 *   service (HTTP) → query (cache) → hook (UI shape)
 *
 * useValuation:  user-level summary across ALL portfolios. Use on the
 *                dashboard header and the "all portfolios" view.
 *
 * useValuationChart: time-series data for the performance chart.
 *                    Thin wrapper — the query is already clean enough
 *                    but the hook gives a consistent import path.
 */

import { useValuationSummary, useValuationHistory } from "@/features/portfolio/queries/useValuation";

// ---------------------------------------------------------------------------
// Shapes the dashboard actually consumes
// ---------------------------------------------------------------------------

export interface ValuationState {
  /** Total market value of all positions + cash across all portfolios. */
  totalValue: number;
  /** Total amount invested (cost basis). */
  totalCostBasis: number;
  /** Unrealised gain or loss (totalValue - totalCostBasis). Negative = loss. */
  unrealizedGainLoss: number;
  /** Return expressed as a percentage. */
  returnPercentage: number;
  /** ISO currency code the backend used for aggregation. */
  currency: string;
  /** True if one or more positions used cost-basis fallback (market data miss). */
  hasStaleData: boolean;
  /** True while the first fetch is in flight. */
  isLoading: boolean;
  /** True if the user has no active portfolios (backend returned 204). */
  isEmpty: boolean;
  /** True if the request failed outright. */
  isError: boolean;
}

// ---------------------------------------------------------------------------
// useValuation
// ---------------------------------------------------------------------------

/**
 * User-level valuation across ALL portfolios.
 *
 * Use this on the dashboard header, the "all portfolios" view, and anywhere
 * you need an aggregate number. Do NOT use this for per-portfolio numbers —
 * use useNetWorth(portfolioId) for that.
 */
export function useValuation(): ValuationState {
  const { data: summary, isLoading, isError } = useValuationSummary();

  return {
    totalValue: summary?.totalValue ?? 0,
    totalCostBasis: summary?.totalCostBasis ?? 0,
    unrealizedGainLoss: summary?.unrealizedGainLoss ?? 0,
    returnPercentage: summary?.returnPercentage ?? 0,
    currency: summary?.currency ?? "USD",
    hasStaleData: summary?.hasStaleData ?? false,
    isLoading,
    isError,
    // null means the backend returned 204 (no active portfolios)
    isEmpty: !isLoading && summary === null,
  };
}

// ---------------------------------------------------------------------------
// useValuationChart
// ---------------------------------------------------------------------------

export interface ChartPoint {
  date: string;
  value: number;
}

/**
 * Time-series snapshots for the performance chart.
 *
 * @param days  Calendar days to fetch (1–1825).
 */
export function useValuationChart(days: number) {
  const { data: snapshots, isLoading, isError } = useValuationHistory(days);

  const points: ChartPoint[] =
    snapshots?.map((s) => ({
      date: s.snapshotDate ?? "",
      value: s.totalValue ?? 0,
    })) ?? [];

  return {
    points,
    isLoading,
    isError,
    isEmpty: !isLoading && points.length === 0,
  };
}