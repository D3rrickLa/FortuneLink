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
  getAllAccounts,
  getAccount,
  getRealizedGains,
  createAccount,
  updateAccount,
  closeAccount,
  reopenAccount,
} from "../services/account.services";
import type {
  CreateAccountRequest,
  UpdateAccountRequest,
  AccountView,
  PageAccountView,
  RealizedGainsSummaryResponse,
} from "@/lib/api/types";

// ─── Queries ──────────────────────────────────────────────────────────────────

export function useAccounts(
  portfolioId: string,
  page = 0,
  size = 20,
  options?: Omit<UseQueryOptions<PageAccountView>, "queryKey" | "queryFn">
) {
  const { user } = useAuth();
  return useQuery({
    queryKey: queryKeys.accounts.all(portfolioId),
    queryFn: () => getAllAccounts(user!.id, portfolioId, page, size),
    enabled: Boolean(user?.id) && Boolean(portfolioId),
    staleTime: 30_000,
    ...options,
  });
}

export function useAccount(
  portfolioId: string,
  accountId: string,
  options?: Omit<UseQueryOptions<AccountView>, "queryKey" | "queryFn">
) {
  const { user } = useAuth();
  return useQuery({
    queryKey: queryKeys.accounts.detail(portfolioId, accountId),
    queryFn: () => getAccount(user!.id, portfolioId, accountId),
    enabled: Boolean(user?.id) && Boolean(portfolioId) && Boolean(accountId),
    staleTime: 30_000,
    ...options,
  });
}

export function useRealizedGains(
  portfolioId: string,
  accountId: string,
  opts: { taxYear?: number; symbol?: string; page?: number; size?: number } = {},
  options?: Omit<UseQueryOptions<RealizedGainsSummaryResponse>, "queryKey" | "queryFn">
) {
  return useQuery({
    queryKey: queryKeys.accounts.realizedGains(
      portfolioId,
      accountId,
      opts.taxYear,
      opts.symbol
    ),
    queryFn: () => getRealizedGains(portfolioId, accountId, opts),
    enabled: Boolean(portfolioId) && Boolean(accountId),
    staleTime: 5 * 60_000,
    ...options,
  });
}

// ─── Mutations ────────────────────────────────────────────────────────────────

export function useCreateAccount(portfolioId: string) {
  const qc = useQueryClient();
  const { user } = useAuth();
  return useMutation({
    mutationFn: (body: CreateAccountRequest) =>
      createAccount(user!.id, portfolioId, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.accounts.all(portfolioId) });
      qc.invalidateQueries({
        queryKey: queryKeys.portfolios.detail(portfolioId),
      });
    },
  });
}

export function useUpdateAccount(portfolioId: string, accountId: string) {
  const qc = useQueryClient();
  const { user } = useAuth();
  return useMutation({
    mutationFn: (body: UpdateAccountRequest) =>
      updateAccount(user!.id, portfolioId, accountId, body),
    onSuccess: () => {
      qc.invalidateQueries({
        queryKey: queryKeys.accounts.detail(portfolioId, accountId),
      });
    },
  });
}

export function useCloseAccount(portfolioId: string) {
  const qc = useQueryClient();
  const { user } = useAuth();
  return useMutation({
    mutationFn: (accountId: string) =>
      closeAccount(user!.id, portfolioId, accountId),
    onSuccess: (_data, accountId) => {
      qc.invalidateQueries({
        queryKey: queryKeys.accounts.detail(portfolioId, accountId),
      });
      qc.invalidateQueries({ queryKey: queryKeys.accounts.all(portfolioId) });
    },
  });
}

export function useReopenAccount(portfolioId: string) {
  const qc = useQueryClient();
  const { user } = useAuth();
  return useMutation({
    mutationFn: (accountId: string) =>
      reopenAccount(user!.id, portfolioId, accountId),
    onSuccess: (_data, accountId) => {
      qc.invalidateQueries({
        queryKey: queryKeys.accounts.detail(portfolioId, accountId),
      });
      qc.invalidateQueries({ queryKey: queryKeys.accounts.all(portfolioId) });
    },
  });
}