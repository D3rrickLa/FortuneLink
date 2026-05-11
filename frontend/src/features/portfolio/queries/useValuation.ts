"use client";

import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/client";

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
  totalValue: number;          // Matches Java totalValue / DB total_value_amount
  totalCostBasis: number;      // Matches Java totalCostBasis
  unrealizedGainLoss: number;  // Matches Java unrealizedGainLoss
  gainLossPercent: number;     // Matches Java gainLossPercent
  totalCashBalance: number;    // Matches Java totalCashBalance
  totalInvestedValue: number;  // Matches Java totalInvestedValue
  currency: string;
  hasStaleData: boolean;
  snapshotDate: string;        // Matches Java snapshotDate
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