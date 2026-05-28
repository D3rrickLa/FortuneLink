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
// Scope
// ─────────────────────────────────────────────────────────────────────────────

export interface ValuationScope {
  portfolioId?: string | null;
  accountId?: string | null;
}

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

export function useValuation(
  scope?: ValuationScope,
  enabled = true
): ValuationState {
  const portfolioId = scope?.portfolioId;
  const accountId = scope?.accountId;

  const isAccountQuery =
    !!portfolioId && !!accountId;

  const isPortfolioQuery =
    !!portfolioId && !accountId;

  const query = useQuery({
    queryKey: [
      "valuation",
      portfolioId ?? "all",
      accountId ?? "all",
    ],

    queryFn: async (): Promise<RawValuationResponse | null> => {
      let response;

      if (isAccountQuery) {
        response = await apiClient.get(
          `/api/v1/portfolios/${portfolioId}/accounts/${accountId}/valuation`
        );
      } else if (isPortfolioQuery) {
        response = await apiClient.get(
          `/api/v1/valuations/${portfolioId}`
        );
      } else {
        response = await apiClient.get(
          "/api/v1/valuations/summary"
        );
      }

      if (response.status === 204) {
        return null;
      }

      if (response.status >= 400) {
        throw new Error("Failed to fetch valuation");
      }

      return response.data as RawValuationResponse;
    },

    enabled,

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

export function useValuationChart(
  days: number,
  scope?: ValuationScope
) {
  const portfolioId = scope?.portfolioId;
  const accountId = scope?.accountId;

  const query = useQuery({
    queryKey: [
      "valuation-history",
      portfolioId ?? "all",
      accountId ?? "all",
      days,
    ],

    queryFn: async (): Promise<RawSnapshotResponse[]> => {
      let url = "/api/v1/valuations/history";

      if (portfolioId && accountId) {
        url = `/api/v1/portfolios/${portfolioId}/accounts/${accountId}/history`;
      } else if (portfolioId) {
        url = `/api/v1/portfolios/${portfolioId}/history`;
      }

      try {
        const response = await apiClient.get(url, {
          params: { days },
        });

        const data = response?.data;

        if (!data) return [];

        if (Array.isArray(data)) return data;

        if (Array.isArray(data?.data)) return data.data;

        if (Array.isArray(data?.points)) return data.points;

        console.error("Invalid history response shape:", data);

        return [];
      } catch (err) {
        console.error("History fetch failed:", err);
        return [];
      }
    },

    staleTime: 1000 * 60,
    enabled: true,
  });

  const safeArray: RawSnapshotResponse[] = Array.isArray(query.data)
    ? query.data
    : [];

  const points: ChartPoint[] = safeArray.map((snapshot) => ({
    date: snapshot.snapshotDate ?? "",
    value: Number(snapshot.totalValue ?? 0),
  }));

  return {
    points,
    isLoading: query.isLoading,
    isError: query.isError,
    isEmpty: !query.isLoading && points.length === 0,
  };
}