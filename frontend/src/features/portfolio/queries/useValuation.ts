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

// ─────────────────────────────────────────────────────────────────────────────
// GLOBAL / PORTFOLIO VALUATION
// ─────────────────────────────────────────────────────────────────────────────

export function useValuation(
  portfolioId?: string,
  enabled = true
): ValuationState {
  const isPortfolioQuery =
    !!portfolioId && portfolioId !== "all";

  const query = useQuery({
    queryKey: [
      "valuation",
      isPortfolioQuery ? portfolioId : "global",
    ],

    queryFn: async (): Promise<RawValuationResponse | null> => {
      const response = isPortfolioQuery
        ? await apiClient.get(
          `/api/v1/valuations/${portfolioId}`
        )
        : await apiClient.get(
          "/api/v1/valuations/summary"
        );

      if (response.status === 204) return null;
      if (response.status >= 400) {
        throw new Error("Failed to fetch valuation");
      }

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
// ACCOUNT VALUATION  (🔥 FIXED ENDPOINT)
// ─────────────────────────────────────────────────────────────────────────────

export function useAccountValuation(
  portfolioId?: string,
  accountId?: string,
  enabled = true
): ValuationState {
  const query = useQuery({
    queryKey: [
      "account-valuation",
      portfolioId,
      accountId,
    ],

    enabled: enabled && !!portfolioId && !!accountId,

    queryFn: async (): Promise<RawValuationResponse | null> => {
      const response = await apiClient.get(
        `/api/v1/portfolios/${portfolioId}/accounts/${accountId}/valuation`
      );

      if (response.status === 204) return null;
      if (response.status >= 400) {
        throw new Error("Failed to fetch account valuation");
      }

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
// GLOBAL HISTORY (OPTIONAL PORTFOLIO SCOPED FIX)
// ─────────────────────────────────────────────────────────────────────────────

export function useValuationChart(
  days: number,
  portfolioId?: string,
  accountId?: string,
  enabled = true
) {
  const isAccountHistory = !!portfolioId && !!accountId;
  const isPortfolioHistory = !!portfolioId && !accountId;

  const query = useQuery({
    queryKey: [
      "valuation-history",
      portfolioId ?? "global",
      accountId ?? "none",
      days,
    ],

    enabled,

    queryFn: async (): Promise<RawSnapshotResponse[]> => {
      let url = "/api/v1/valuations/history";

      if (isAccountHistory) {
        url = `/api/v1/portfolios/${portfolioId}/accounts/${accountId}`;
      } else if (isPortfolioHistory) {
        url = `/api/v1/valuations/${portfolioId}`;
      }

      const response = await apiClient.get(url, {
        params: { days },
      });

      const data = response?.data;

      if (!data) return [];

      if (Array.isArray(data)) return data;

      if (Array.isArray(data?.content)) return data.content;

      if (Array.isArray(data?.data)) return data.data;

      console.error("Invalid history response shape:", data);

      return [];
    },

    staleTime: 60_000,
  });

  const safeData = Array.isArray(query.data) ? query.data : [];

  const points: ChartPoint[] = safeData.map((snapshot) => ({
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