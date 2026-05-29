"use client";

import {
  useValuationQuery,
  useAccountValuationQuery,
  useValuationHistoryQuery,
  ValuationScope,
} from "../queries/useValuationQueries";

import type {
  ValuationResponse,
  ValuationSnapshotResponse,
  AccountHistorySnapshotResponse,
} from "@/lib/api/types";

// ─────────────────────────────────────────────────────────────
// UI Types
// ─────────────────────────────────────────────────────────────

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

// ─────────────────────────────────────────────────────────────
// Normalizer
// ─────────────────────────────────────────────────────────────

function normalizeValuation(
  data?: ValuationResponse | null
): Omit<ValuationState, "isLoading" | "isError" | "isEmpty"> {
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
    totalCostBasis: data.totalCostBasis?.amount ?? 0,
    unrealizedGainLoss: data.unrealizedGainLoss?.amount ?? 0,
    returnPercentage: data.gainLossPercent ?? 0,
    totalCashBalance: data.totalCashBalance?.amount ?? 0,
    totalInvestedValue: data.totalInvestedValue?.amount ?? 0,
    currency:
      data.currency ??
      data.totalValue?.currency ??
      "CAD",
    hasStaleData: data.hasStaleData ?? false,
  };
}

// ─────────────────────────────────────────────────────────────
// useValuation
// ─────────────────────────────────────────────────────────────

export function useValuation(
  scope?: ValuationScope,
  enabled = true
): ValuationState {
  const query = useValuationQuery(scope, enabled);

  const normalized = normalizeValuation(query.data);

  return {
    ...normalized,
    isLoading: query.isLoading,
    isError: query.isError,
    isEmpty: !query.isLoading && query.data === null,
  };
}

// ─────────────────────────────────────────────────────────────
// Optional account hook wrapper
// ─────────────────────────────────────────────────────────────

export function useAccountValuation(
  portfolioId?: string | null,
  accountId?: string | null,
  enabled = true
): ValuationState {
  const query = useAccountValuationQuery(
    portfolioId,
    accountId,
    enabled
  );

  const normalized = normalizeValuation(query.data);

  return {
    ...normalized,
    isLoading: query.isLoading,
    isError: query.isError,
    isEmpty: !query.isLoading && !query.data,
  };
}

// ─────────────────────────────────────────────────────────────
// Chart mapping
// ─────────────────────────────────────────────────────────────

function mapHistoryToPoints(
  data: (
    | ValuationSnapshotResponse
    | AccountHistorySnapshotResponse
  )[] = []
): ChartPoint[] {
  return data
    .map((snapshot) => {
      const rawDate =
        (snapshot as any).snapshotDate ?? "";

      const parsed = rawDate
        ? new Date(rawDate)
        : null;

      if (!parsed || isNaN(parsed.getTime())) {
        return null;
      }

      const value =
        typeof (snapshot as any).totalValue === "object"
          ? Number(
              (snapshot as any).totalValue?.amount ?? 0
            )
          : Number((snapshot as any).totalValue ?? 0);

      return {
        date: parsed.toISOString().split("T")[0],
        value,
      };
    })
    .filter((p): p is ChartPoint => p !== null);
}

// ─────────────────────────────────────────────────────────────
// useValuationChart
// ─────────────────────────────────────────────────────────────

export function useValuationChart(
  days: number,
  scope?: ValuationScope
) {
  const query = useValuationHistoryQuery(days, scope);

  const points = mapHistoryToPoints(query.data ?? []);

  return {
    points,
    isLoading: query.isLoading,
    isError: query.isError,
    isEmpty: !query.isLoading && points.length === 0,
  };
}