"use client";

import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/client";
import { components } from "@/lib/api/schema";

// ─────────────────────────────────────────────────────────────────────────────
// Raw API types
// ─────────────────────────────────────────────────────────────────────────────

type RawValuationResponse = components["schemas"]["ValuationResponse"];
type RawSnapshotResponse = components["schemas"]["ValuationSnapshotResponse"];

// ─────────────────────────────────────────────────────────────────────────────
// UI types
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

function normalizeValuation(data?: RawValuationResponse | null) {
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
    currency: data.currency ?? data.totalValue?.currency ?? "CAD",
    hasStaleData: data.hasStaleData ?? false,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// useValuation — global summary or per-portfolio
//
// portfolioId: string  → GET /api/v1/valuations/{portfolioId}
// portfolioId: falsy   → GET /api/v1/valuations/summary
// ─────────────────────────────────────────────────────────────────────────────

export function useValuation(
  portfolioId?: string | null,
  enabled = true
): ValuationState {
  // Treat null / undefined / "all" as "no specific portfolio"
  const isPortfolioQuery =
    !!portfolioId && portfolioId !== "all";

  const query = useQuery({
    queryKey: ["valuation", isPortfolioQuery ? portfolioId : "global"],

    queryFn: async (): Promise<RawValuationResponse | null> => {
      const response = isPortfolioQuery
        ? await apiClient.get(`/api/v1/valuations/${portfolioId}`)
        : await apiClient.get("/api/v1/valuations/summary");

      if (response.status === 204) return null;
      if (response.status >= 400) throw new Error("Failed to fetch valuation");

      return response.data as RawValuationResponse;
    },

    enabled: enabled && (!isPortfolioQuery || !!portfolioId),
    staleTime: 60_000,
  });

  return {
    ...normalizeValuation(query.data),
    isLoading: query.isLoading,
    isError: query.isError,
    isEmpty: !query.isLoading && !query.data,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// useAccountValuation
//
// GET /api/v1/portfolios/{portfolioId}/accounts/{accountId}/valuation
// ─────────────────────────────────────────────────────────────────────────────

export function useAccountValuation(
  portfolioId?: string | null,
  accountId?: string | null,
  enabled = true
): ValuationState {
  const canFetch = !!portfolioId && !!accountId;

  const query = useQuery({
    queryKey: ["account-valuation", portfolioId, accountId],

    enabled: enabled && canFetch,

    queryFn: async (): Promise<RawValuationResponse | null> => {
      const response = await apiClient.get(
        `/api/v1/portfolios/${portfolioId}/accounts/${accountId}/valuation`
      );

      if (response.status === 204) return null;
      if (response.status >= 400)
        throw new Error("Failed to fetch account valuation");

      return response.data as RawValuationResponse;
    },

    staleTime: 60_000,
  });

  return {
    ...normalizeValuation(query.data),
    isLoading: query.isLoading,
    isError: query.isError,
    isEmpty: !query.isLoading && !query.data,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// useValuationChart
//
// THE ONLY history endpoint in the current API contract is:
//   GET /api/v1/valuations/history?days=N
//
// There are no per-portfolio or per-account history endpoints. Using any other
// URL here returns either a single ValuationResponse (not an array) or 404,
// both of which silently produce an empty chart.
//
// When the backend adds scoped history endpoints this function should be
// updated — the queryKey already scopes by portfolioId/accountId so cache
// invalidation will work correctly once those endpoints exist.
// ─────────────────────────────────────────────────────────────────────────────

export function useValuationChart(
  days: number,
  portfolioId?: string | null,
  accountId?: string | null,
  enabled = true
) {
  const query = useQuery({
    queryKey: [
      "valuation-history",
      portfolioId ?? "global",
      accountId ?? "none",
      days,
    ],

    enabled,

    queryFn: async (): Promise<RawSnapshotResponse[]> => {
      const response = await apiClient.get("/api/v1/valuations/history", {
        params: { days },
      });

      const data = response?.data;
      if (!data) return [];
      if (Array.isArray(data)) return data;

      // Shouldn't happen with a correct endpoint, but guard anyway
      console.error("[useValuationChart] Unexpected response shape:", data);
      return [];
    },

    staleTime: 60_000,
  });

  const safeData: RawSnapshotResponse[] = Array.isArray(query.data)
    ? query.data
    : [];

  const points: ChartPoint[] = safeData
    .filter((s) => s.snapshotDate != null && s.totalValue != null)
    .map((snapshot) => ({
      date: snapshot.snapshotDate!,
      value: Number(snapshot.totalValue),
    }));

  return {
    points,
    isLoading: query.isLoading,
    isError: query.isError,
    isEmpty: !query.isLoading && points.length === 0,
  };
}