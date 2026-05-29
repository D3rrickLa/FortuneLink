"use client";

import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/client";
import { components } from "@/lib/api/schema";

import type {
  ValuationResponse,
  ValuationSnapshotResponse,
  AccountHistorySnapshotResponse,
} from "@/lib/api/types";

// ─────────────────────────────────────────────────────────────
// Scope
// ─────────────────────────────────────────────────────────────

export interface ValuationScope {
  portfolioId?: string | null;
  accountId?: string | null;
}

// ─────────────────────────────────────────────────────────────
// Valuation (current snapshot)
// ─────────────────────────────────────────────────────────────

export function useValuationQuery(
  scope?: ValuationScope,
  enabled = true
) {
  const portfolioId = scope?.portfolioId;
  const accountId = scope?.accountId;

  const isAccount =
    !!portfolioId && !!accountId;

  const isPortfolio =
    !!portfolioId && !accountId;

  return useQuery({
    queryKey: [
      "valuation",
      portfolioId ?? "all",
      accountId ?? "all",
    ],

    enabled,

    staleTime: 60_000,

    queryFn: async (): Promise<ValuationResponse | null> => {
      let response;

      if (isAccount) {
        response = await apiClient.get(
          `/api/v1/portfolios/${portfolioId}/accounts/${accountId}/valuation`
        );
      } else if (isPortfolio) {
        response = await apiClient.get(
          `/api/v1/valuations/${portfolioId}`
        );
      } else {
        response = await apiClient.get(
          "/api/v1/valuations/summary"
        );
      }

      if (response.status === 204) return null;
      if (response.status >= 400) {
        throw new Error("Failed to fetch valuation");
      }

      return response.data;
    },
  });
}

// ─────────────────────────────────────────────────────────────
// Account valuation (optional separate hook)
// ─────────────────────────────────────────────────────────────

export function useAccountValuationQuery(
  portfolioId?: string | null,
  accountId?: string | null,
  enabled = true
) {
  const canFetch = !!portfolioId && !!accountId;

  return useQuery({
    queryKey: [
      "account-valuation",
      portfolioId,
      accountId,
    ],

    enabled: enabled && canFetch,

    staleTime: 60_000,

    queryFn: async (): Promise<ValuationResponse | null> => {
      const response = await apiClient.get(
        `/api/v1/portfolios/${portfolioId}/accounts/${accountId}/valuation`
      );

      if (response.status === 204) return null;
      if (response.status >= 400) {
        throw new Error("Failed to fetch account valuation");
      }

      return response.data;
    },
  });
}

// ─────────────────────────────────────────────────────────────
// History query
// ─────────────────────────────────────────────────────────────

export function useValuationHistoryQuery(
  days: number,
  scope?: ValuationScope
) {
  const portfolioId = scope?.portfolioId;
  const accountId = scope?.accountId;

  return useQuery({
    queryKey: [
      "valuation-history",
      portfolioId ?? "all",
      accountId ?? "all",
      days,
    ],

    staleTime: 60_000,

    queryFn: async (): Promise<
      (
        | ValuationSnapshotResponse
        | AccountHistorySnapshotResponse
      )[]
    > => {
      let url = "/api/v1/valuations/history";

      if (portfolioId && accountId) {
        url = `/api/v1/portfolios/${portfolioId}/accounts/${accountId}/valuation/history`;
      }

      const response = await apiClient.get(url, {
        params: { days },
      });

      return Array.isArray(response.data)
        ? response.data
        : [];
    },
  });
}