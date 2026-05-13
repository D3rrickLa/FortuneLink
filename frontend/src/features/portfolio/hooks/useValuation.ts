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
        ? await apiClient.get(
          "/api/v1/valuations/{portfolioId}", // Add the placeholder here!
          {
            params: {
              path: {
                portfolioId,
              },
            },
          }
        )
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