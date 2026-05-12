"use client";

import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/client";
import type { components } from "@/lib/api/schema";

// ─── Schema types (what the API actually returns) ─────────────────────────────

type RawValuationResponse = components["schemas"]["ValuationResponse"];
type RawSnapshotResponse = components["schemas"]["ValuationSnapshotResponse"];

// ─── Normalised flat shapes (what our UI consumes) ────────────────────────────
// All monetary values are unwrapped from MoneyResponse → plain number.
// Field names are normalised (gainLossPercent → returnPercentage) so every
// consumer keeps working without a sweep of edits.

export interface ValuationResponse {
  totalValue: number;
  totalCostBasis: number;
  unrealizedGainLoss: number;
  /** gainLossPercent from the API, renamed for UI consistency */
  returnPercentage: number;
  totalCashBalance: number;
  totalInvestedValue: number;
  currency: string;
  hasStaleData: boolean;
}

export interface ValuationSnapshotResponse {
  totalValue: number;
  totalCostBasis: number;
  unrealizedGainLoss: number;
  gainLossPercent: number;
  totalCashBalance: number;
  totalInvestedValue: number;
  currency: string;
  hasStaleData: boolean;
  snapshotDate: string;
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * The backend ValuationResponse wraps monetary fields in MoneyResponse objects
 * ({ amount: number, currency: string }).  Extract the raw amount defensively
 * so we don't silently return NaN when the shape is a nested object.
 */
function toNum(field: unknown): number {
  if (field === null || field === undefined) return 0;
  if (typeof field === "number") return Number.isFinite(field) ? field : 0;
  // MoneyResponse shape: { amount?: number, currency?: string }
  if (typeof field === "object" && "amount" in (field as object)) {
    const amt = (field as { amount?: unknown }).amount;
    if (typeof amt === "number" && Number.isFinite(amt)) return amt;
  }
  return 0;
}

function toCurrency(raw: RawValuationResponse): string {
  if (raw.currency) return raw.currency;
  // Fallback: pull currency off any MoneyResponse field
  const anyMoney = raw.totalValue ?? raw.unrealizedGainLoss ?? raw.totalCostBasis;
  if (anyMoney && typeof anyMoney === "object" && "currency" in anyMoney) {
    return (anyMoney as { currency?: string }).currency ?? "USD";
  }
  return "USD";
}

function normaliseValuation(raw: RawValuationResponse): ValuationResponse {
  console.log("Raw API Data:", raw); // Check if it's { amount, currency }
  const normalized = {
    totalValue: toNum(raw.totalValue),
    // ... rest of the fields
  };
  console.log("Normalized Data:", normalized); // Check if it's now a flat number
  return {
    totalValue:        toNum(raw.totalValue),
    totalCostBasis:    toNum(raw.totalCostBasis),
    unrealizedGainLoss: toNum(raw.unrealizedGainLoss),
    // API field is gainLossPercent — rename for consistency with callers
    returnPercentage:  raw.gainLossPercent ?? 0,
    totalCashBalance:  toNum(raw.totalCashBalance),
    totalInvestedValue: toNum(raw.totalInvestedValue),
    currency:          toCurrency(raw),
    hasStaleData:      raw.hasStaleData ?? false,
  };
}

// ─── Cache keys ───────────────────────────────────────────────────────────────

export const valuationKeys = {
  all:     ["valuations"] as const,
  summary: () => [...valuationKeys.all, "summary"] as const,
  history: (days: number) => [...valuationKeys.all, "history", days] as const,
};

// ─── Hooks ────────────────────────────────────────────────────────────────────

export function useValuationSummary() {
  return useQuery({
    queryKey: valuationKeys.summary(),
    queryFn: async (): Promise<ValuationResponse | null> => {
      const res = await apiClient.get<RawValuationResponse>(
        "/api/v1/valuations/summary",
        { validateStatus: (s) => s === 204 || (s >= 200 && s < 300) }
      );
      if (res.status === 204) return null;
      return normaliseValuation(res.data);
    },
    staleTime: 5 * 60 * 1000,
  });
}

export function useValuationHistory(days: number) {
  return useQuery({
    queryKey: valuationKeys.history(days),
    queryFn: async (): Promise<ValuationSnapshotResponse[]> => {
      const res = await apiClient.get<RawSnapshotResponse[]>(
        "/api/v1/valuations/history",
        { params: { days } }
      );
      // Snapshots use plain numbers per schema — still run through toNum
      // defensively in case the backend ever changes shape.
      return (res.data ?? []).map((s) => ({
        totalValue:         toNum(s.totalValue),
        totalCostBasis:     toNum(s.totalCostBasis),
        unrealizedGainLoss: toNum(s.unrealizedGainLoss),
        gainLossPercent:    s.gainLossPercent ?? 0,
        totalCashBalance:   toNum(s.totalCashBalance),
        totalInvestedValue: toNum(s.totalInvestedValue),
        currency:           s.currency ?? "USD",
        hasStaleData:       s.hasStaleData ?? false,
        snapshotDate:       s.snapshotDate ?? "",
      }));
    },
    staleTime: 10 * 60 * 1000,
    enabled: days > 0 && days <= 1825,
  });
}