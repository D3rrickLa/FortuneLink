import apiClient, { userIdParam, newIdempotencyKey } from "@/lib/api/client";
import type {
  TransactionView,
  PageTransactionView,
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

const base = (portfolioId: string, accountId: string) =>
  `/api/v1/portfolios/${portfolioId}/accounts/${accountId}/transactions`;

// ─── Queries ──────────────────────────────────────────────────────────────────

export async function getTransactionHistory(
  userId: string,
  portfolioId: string,
  accountId: string,
  filters: {
    symbol?: string;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
  } = {}
): Promise<PageTransactionView> {
  const { data } = await apiClient.get<PageTransactionView>(
    base(portfolioId, accountId),
    { params: { ...userIdParam(userId), ...filters } }
  );
  return data;
}

export async function getTransaction(
  userId: string,
  portfolioId: string,
  accountId: string,
  transactionId: string
): Promise<TransactionView> {
  const { data } = await apiClient.get<TransactionView>(
    `${base(portfolioId, accountId)}/${transactionId}`,
    { params: userIdParam(userId) }
  );
  return data;
}

/** Returns the URL string to trigger a browser download of the CSV template. */
export function getCsvTemplateUrl(portfolioId: string, accountId: string): string {
  const apiBase = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
  return `${apiBase}${base(portfolioId, accountId)}/import/template`;
}

// ─── Idempotent mutation helper ───────────────────────────────────────────────

function idempotentHeaders(key?: string) {
  return { "Idempotency-Key": key ?? newIdempotencyKey() };
}

// ─── Asset transactions ───────────────────────────────────────────────────────

/**
 * BUY — decreases cash, increases position.
 * validateSymbol must be called first to seed the asset in the database.
 */
export async function recordBuy(
  portfolioId: string,
  accountId: string,
  body: RecordPurchaseRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/buy`,
    body,
    { headers: idempotentHeaders(idempotencyKey) }
  );
  return data;
}

/** SELL — increases cash, triggers realised gain/loss calculation. */
export async function recordSell(
  portfolioId: string,
  accountId: string,
  body: RecordSaleRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/sell`,
    body,
    { headers: idempotentHeaders(idempotencyKey) }
  );
  return data;
}

/** SPLIT — adjusts quantity and cost basis via ratio. No cash impact. */
export async function recordSplit(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: RecordSplitRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/split`,
    body,
    {
      params: userIdParam(userId),
      headers: idempotentHeaders(idempotencyKey),
    }
  );
  return data;
}

/** DRIP — dividend reinvestment; creates an income and buy event. */
export async function recordDRIP(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: RecordDRIPRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/drip`,
    body,
    {
      params: userIdParam(userId),
      headers: idempotentHeaders(idempotencyKey),
    }
  );
  return data;
}

/** Return of Capital — reduces asset cost basis, increases cash. */
export async function recordReturnOfCapital(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: RecordReturnOfCapitalRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/return-of-capital`,
    body,
    {
      params: userIdParam(userId),
      headers: idempotentHeaders(idempotencyKey),
    }
  );
  return data;
}

// ─── Income transactions ──────────────────────────────────────────────────────

/** DIVIDEND — cash income from a held asset. */
export async function recordDividend(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: RecordDividendRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/dividend`,
    body,
    {
      params: userIdParam(userId),
      headers: idempotentHeaders(idempotencyKey),
    }
  );
  return data;
}

/** INTEREST — interest earned on cash or a specific bond/GIC. */
export async function recordInterest(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: RecordInterestRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/interest`,
    body,
    {
      params: userIdParam(userId),
      headers: idempotentHeaders(idempotencyKey),
    }
  );
  return data;
}

// ─── Cash transactions ────────────────────────────────────────────────────────

export async function recordDeposit(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: RecordDepositRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/deposit`,
    body,
    {
      params: userIdParam(userId),
      headers: idempotentHeaders(idempotencyKey),
    }
  );
  return data;
}

export async function recordWithdrawal(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: RecordWithdrawalRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/withdrawal`,
    body,
    {
      params: userIdParam(userId),
      headers: idempotentHeaders(idempotencyKey),
    }
  );
  return data;
}

export async function recordTransferIn(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: RecordTransferInRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/transfer-in`,
    body,
    {
      params: userIdParam(userId),
      headers: idempotentHeaders(idempotencyKey),
    }
  );
  return data;
}

export async function recordTransferOut(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: RecordTransferOutRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/transfer-out`,
    body,
    {
      params: userIdParam(userId),
      headers: idempotentHeaders(idempotencyKey),
    }
  );
  return data;
}

export async function recordFee(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: RecordStandaloneFeeRequest,
  idempotencyKey?: string
): Promise<TransactionView> {
  const { data } = await apiClient.post<TransactionView>(
    `${base(portfolioId, accountId)}/fee`,
    body,
    {
      params: userIdParam(userId),
      headers: idempotentHeaders(idempotencyKey),
    }
  );
  return data;
}

// ─── Lifecycle ────────────────────────────────────────────────────────────────

/**
 * Soft-removes a transaction from P&L and position calculations.
 * Idempotency-Key is required by the backend for this endpoint.
 */
export async function excludeTransaction(
  userId: string,
  portfolioId: string,
  accountId: string,
  transactionId: string,
  body: ExcludeTransactionRequest,
  idempotencyKey: string
): Promise<TransactionView> {
  const { data } = await apiClient.patch<TransactionView>(
    `${base(portfolioId, accountId)}/${transactionId}/exclude`,
    body,
    {
      params: userIdParam(userId),
      headers: { "Idempotency-Key": idempotencyKey },
    }
  );
  return data;
}

/** Re-activates a previously excluded transaction. */
export async function restoreTransaction(
  userId: string,
  portfolioId: string,
  accountId: string,
  transactionId: string,
  idempotencyKey: string
): Promise<TransactionView> {
  const { data } = await apiClient.patch<TransactionView>(
    `${base(portfolioId, accountId)}/${transactionId}/restore`,
    null,
    {
      params: userIdParam(userId),
      headers: { "Idempotency-Key": idempotencyKey },
    }
  );
  return data;
}

// ─── CSV import ───────────────────────────────────────────────────────────────

/**
 * Validates ALL rows before committing. On failure, returns row-level errors
 * and commits nothing — the backend is all-or-nothing.
 */
export async function importCsv(
  userId: string,
  portfolioId: string,
  accountId: string,
  file: File
): Promise<CsvImportResult> {
  const form = new FormData();
  form.append("file", file);
  form.append("id", userId); // UserId.id field

  const { data } = await apiClient.post<CsvImportResult>(
    `${base(portfolioId, accountId)}/import`,
    form,
    { headers: { "Content-Type": "multipart/form-data" } }
  );
  return data;
}