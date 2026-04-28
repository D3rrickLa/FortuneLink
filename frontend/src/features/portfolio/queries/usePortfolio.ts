"use client";

import {
  useQuery,
  useMutation,
  useQueryClient,
  type UseQueryOptions,
} from "@tanstack/react-query";
import { queryKeys } from "@/lib/api/queryKeys";
import { useAuth } from "@/features/auth/hooks/userAuth";
import {
  getPortfolios,
  getPortfolio,
  getNetWorth,
  getNetWorthHistory,
  createPortfolio,
  updatePortfolio,
  deletePortfolio,
} from "../services/porfolio.services";
import type {
  CreatePortfolioRequest,
  UpdatePortfolioRequest,
  PortfolioSummaryResponse,
  PortfolioResponse,
  NetWorthResponse,
  NetWorthSnapshotResponse,
} from "@/lib/api/types";

// ─── Queries ──────────────────────────────────────────────────────────────────

export function usePortfolios(
  options?: Omit<UseQueryOptions<PortfolioSummaryResponse[]>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: queryKeys.portfolios.list(),
    queryFn: getPortfolios,
    staleTime: 60_000,
    ...options,
  });
}

export function usePortfolio(
  portfolioId: string,
  options?: Omit<UseQueryOptions<PortfolioResponse>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: queryKeys.portfolios.detail(portfolioId),
    queryFn: () => getPortfolio(portfolioId),
    enabled: Boolean(portfolioId),
    ...options,
  });
}

export function useNetWorth(
  portfolioId: string,
  options?: Omit<UseQueryOptions<NetWorthResponse>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: queryKeys.portfolios.netWorth(portfolioId),
    queryFn: () => getNetWorth(portfolioId),
    enabled: Boolean(portfolioId),
    // Live valuation — keep reasonably fresh but avoid hammering the backend.
    staleTime: 30_000,
    ...options,
  });
}

export function useNetWorthHistory(
  days?: number,
  options?: Omit<UseQueryOptions<NetWorthSnapshotResponse[]>, "queryKey" | "queryFn">
) {
  const { user } = useAuth();
  return useQuery({
    queryKey: queryKeys.netWorthHistory(days),
    queryFn: () => getNetWorthHistory(user!.id, days),
    enabled: Boolean(user?.id),
    staleTime: 5 * 60_000,
    ...options,
  });
}

// ─── Mutations ────────────────────────────────────────────────────────────────

export function useCreatePortfolio() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: CreatePortfolioRequest) => createPortfolio(body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.portfolios.list() });
    },
  });
}

export function useUpdatePortfolio(portfolioId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: UpdatePortfolioRequest) =>
      updatePortfolio(portfolioId, body),
    onSuccess: (updated) => {
      qc.setQueryData(queryKeys.portfolios.detail(portfolioId), updated);
      qc.invalidateQueries({ queryKey: queryKeys.portfolios.list() });
    },
  });
}

export function useDeletePortfolio() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      portfolioId,
      opts,
    }: {
      portfolioId: string;
      opts?: { softDelete?: boolean; recursive?: boolean };
    }) => deletePortfolio(portfolioId, opts),
    onSuccess: (_data, { portfolioId }) => {
      qc.removeQueries({ queryKey: queryKeys.portfolios.detail(portfolioId) });
      qc.invalidateQueries({ queryKey: queryKeys.portfolios.list() });
    },
  });
}