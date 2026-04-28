"use client";

import {
  useQuery,
  useMutation,
  useQueryClient,
  type UseQueryOptions,
} from "@tanstack/react-query";
import { queryKeys } from "@/lib/api/queryKeys";
import { useAuth } from "@/features/auth/hooks/userAuth";
import { newIdempotencyKey } from "@/lib/api/client";
import {
  getTransactionHistory,
  getTransaction,
  recordBuy,
  recordSell,
  recordDeposit,
  recordWithdrawal,
  recordTransferIn,
  recordTransferOut,
  recordDividend,
  recordDRIP,
  recordInterest,
  recordReturnOfCapital,
  recordSplit,
  recordFee,
  excludeTransaction,
  restoreTransaction,
  importCsv,
} from "../services/transaction.services";
import type {
  PageTransactionView,
  TransactionView,
  CsvImportResult,
  RecordPurchaseRequest,
  RecordSaleRequest,
  RecordDepositRequest,
  RecordWithdrawalRequest,
  RecordTransferInRequest,
  RecordTransferOutRequest,
  RecordDividendRequest,
  RecordDRIPRequest,
  RecordInterestRequest,
  RecordReturnOfCapitalRequest,
  RecordSplitRequest,
  RecordStandaloneFeeRequest,
  ExcludeTransactionRequest,
} from "@/lib/api/types";

// ─── Shared invalidation helper ───────────────────────────────────────────────

function useTransactionInvalidation(portfolioId: string, accountId: string) {
  const qc = useQueryClient();
  return () => {
    qc.invalidateQueries({
      queryKey: queryKeys.transactions.list(portfolioId, accountId),
    });
    qc.invalidateQueries({
      queryKey: queryKeys.accounts.detail(portfolioId, accountId),
    });
    qc.invalidateQueries({
      queryKey: queryKeys.portfolios.netWorth(portfolioId),
    });
  };
}

// ─── Queries ──────────────────────────────────────────────────────────────────

export function useTransactionHistory(
  portfolioId: string,
  accountId: string,
  filters: {
    symbol?: string;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
  } = {},
  options?: Omit<UseQueryOptions<PageTransactionView>, "queryKey" | "queryFn">
) {
  const { user } = useAuth();
  return useQuery({
    queryKey: queryKeys.transactions.list(portfolioId, accountId, filters),
    queryFn: () =>
      getTransactionHistory(user!.id, portfolioId, accountId, filters),
    enabled:
      Boolean(user?.id) && Boolean(portfolioId) && Boolean(accountId),
    staleTime: 30_000,
    ...options,
  });
}

export function useTransaction(
  portfolioId: string,
  accountId: string,
  transactionId: string,
  options?: Omit<UseQueryOptions<TransactionView>, "queryKey" | "queryFn">
) {
  const { user } = useAuth();
  return useQuery({
    queryKey: queryKeys.transactions.detail(portfolioId, accountId, transactionId),
    queryFn: () =>
      getTransaction(user!.id, portfolioId, accountId, transactionId),
    enabled:
      Boolean(user?.id) &&
      Boolean(portfolioId) &&
      Boolean(accountId) &&
      Boolean(transactionId),
    ...options,
  });
}

// ─── Mutations ────────────────────────────────────────────────────────────────

export function useRecordBuy(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordPurchaseRequest) =>
      recordBuy(portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

export function useRecordSell(portfolioId: string, accountId: string) {
  const qc = useQueryClient();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordSaleRequest) =>
      recordSell(portfolioId, accountId, body),
    onSuccess: () => {
      invalidate();
      // Realized gains are now stale
      qc.invalidateQueries({
        queryKey: queryKeys.accounts.realizedGains(portfolioId, accountId),
      });
    },
  });
}

export function useRecordDeposit(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordDepositRequest) =>
      recordDeposit(user!.id, portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

export function useRecordWithdrawal(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordWithdrawalRequest) =>
      recordWithdrawal(user!.id, portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

export function useRecordTransferIn(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordTransferInRequest) =>
      recordTransferIn(user!.id, portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

export function useRecordTransferOut(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordTransferOutRequest) =>
      recordTransferOut(user!.id, portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

export function useRecordDividend(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordDividendRequest) =>
      recordDividend(user!.id, portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

export function useRecordDRIP(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordDRIPRequest) =>
      recordDRIP(user!.id, portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

export function useRecordInterest(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordInterestRequest) =>
      recordInterest(user!.id, portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

export function useRecordReturnOfCapital(
  portfolioId: string,
  accountId: string
) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordReturnOfCapitalRequest) =>
      recordReturnOfCapital(user!.id, portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

export function useRecordSplit(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordSplitRequest) =>
      recordSplit(user!.id, portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

export function useRecordFee(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (body: RecordStandaloneFeeRequest) =>
      recordFee(user!.id, portfolioId, accountId, body),
    onSuccess: invalidate,
  });
}

// ─── Lifecycle mutations ──────────────────────────────────────────────────────

export function useExcludeTransaction(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const qc = useQueryClient();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: ({
      transactionId,
      body,
    }: {
      transactionId: string;
      body: ExcludeTransactionRequest;
    }) =>
      excludeTransaction(
        user!.id,
        portfolioId,
        accountId,
        transactionId,
        body,
        newIdempotencyKey()
      ),
    onSuccess: (_data, { transactionId }) => {
      qc.invalidateQueries({
        queryKey: queryKeys.transactions.detail(
          portfolioId,
          accountId,
          transactionId
        ),
      });
      invalidate();
    },
  });
}

export function useRestoreTransaction(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const qc = useQueryClient();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation({
    mutationFn: (transactionId: string) =>
      restoreTransaction(
        user!.id,
        portfolioId,
        accountId,
        transactionId,
        newIdempotencyKey()
      ),
    onSuccess: (_data, transactionId) => {
      qc.invalidateQueries({
        queryKey: queryKeys.transactions.detail(
          portfolioId,
          accountId,
          transactionId
        ),
      });
      invalidate();
    },
  });
}

// ─── CSV import ───────────────────────────────────────────────────────────────

export function useImportCsv(portfolioId: string, accountId: string) {
  const { user } = useAuth();
  const invalidate = useTransactionInvalidation(portfolioId, accountId);
  return useMutation<CsvImportResult, Error, File>({
    mutationFn: (file: File) =>
      importCsv(user!.id, portfolioId, accountId, file),
    onSuccess: (result) => {
      if (result.success) invalidate();
    },
  });
}