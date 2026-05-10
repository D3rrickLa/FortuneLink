"use client";

import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/client";

// --- Types (Keep these here if they aren't in a shared types file yet) ---
export interface ValuationResponse {
  totalValue: number;
  totalCostBasis: number;
  unrealizedGainLoss: number;
  returnPercentage: number;
  currency: string;
  hasStaleData: boolean;
  asOf: string; 
}

export interface ValuationSnapshotResponse {
  netWorth: number;
  totalAssets: number;
  totalLiabilities: number;
  currency: string;
  hasStaleData: boolean;
  snapshotDate: string; 
}

// --- Keys ---
export const valuationKeys = {
  all: ["valuations"] as const,
  summary: () => [...valuationKeys.all, "summary"] as const,
  history: (days: number) => [...valuationKeys.all, "history", days] as const,
};

// --- Hooks ---

export function useValuationSummary() {
  return useQuery({
    queryKey: valuationKeys.summary(),
    queryFn: async (): Promise<ValuationResponse | null> => {
      const res = await apiClient.get<ValuationResponse>("/api/v1/valuations/summary", {
        validateStatus: (s) => s === 204 || (s >= 200 && s < 300)
      });
      return res.status === 204 ? null : res.data;
    },
    staleTime: 5 * 60 * 1000,
  });
}

export function useValuationHistory(days: number) {
  return useQuery({
    queryKey: valuationKeys.history(days),
    queryFn: async (): Promise<ValuationSnapshotResponse[]> => {
      const res = await apiClient.get<ValuationSnapshotResponse[]>(
        "/api/v1/valuations/history",
        { params: { days } }
      );
      return res.data;
    },
    staleTime: 10 * 60 * 1000,
    enabled: days > 0 && days <= 1825,
  });
}