import apiClient from "@/lib/api/client";
import type { ValuationSnapshotResponse } from "@/lib/api/types";

/**
 * GET /api/v1/valuations/summary
 * Returns null on 204 (no active portfolios).
 */
export async function getValuationSummary(): Promise<ValuationSnapshotResponse | null> {
  const res = await apiClient.get<ValuationSnapshotResponse>("/api/v1/valuations/summary", {
    validateStatus: (status) => status === 204 || (status >= 200 && status < 300),
  });

  if (res.status === 204) return null;
  return res.data;
}

/**
 * GET /api/v1/valuations/history?days={n}
 */
export async function getValuationHistory(
  days: number
): Promise<ValuationSnapshotResponse[]> {
  const res = await apiClient.get<ValuationSnapshotResponse[]>(
    "/api/v1/valuations/history",
    { params: { days } }
  );
  return res.data;
}