"use client";

import {
  useQuery,
  useMutation,
  useQueryClient,
  type UseQueryOptions,
} from "@tanstack/react-query";
import { queryKeys } from "@/lib/api/queryKeys";
import {
  getPortfolios,
  getPortfolio,
  createPortfolio,
  updatePortfolio,
  deletePortfolio,
} from "../services/porfolio.services"; // Fixed "porfolio" typo if you renamed the file
import type {
  CreatePortfolioRequest,
  UpdatePortfolioRequest,
  PortfolioSummaryResponse,
  PortfolioResponse,
} from "@/lib/api/types";

// ─── Portfolio queries ────────────────────────────────────────────────────────

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
      // A deletion definitely changes the total valuation summary.
      qc.invalidateQueries({ queryKey: ["valuations"] }); 
    },
  });
}