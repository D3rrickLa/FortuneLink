import apiClient from "@/lib/api/client";
import type {
  CreateAccountRequest,
  UpdateAccountRequest,
  AccountView,
  PageAccountView,
  RealizedGainsSummaryResponse,
} from "@/lib/api/types";

const base = (portfolioId: string) =>
  `/api/v1/portfolios/${portfolioId}/accounts`;

// ─── Queries ──────────────────────────────────────────────────────────────────

export async function getAllAccounts(
  userId: string,
  portfolioId: string,
  page = 0,
  size = 20
): Promise<PageAccountView> {
  const { data } = await apiClient.get<PageAccountView>(base(portfolioId), {
    params: { ...userIdParam(userId), page, size },
  });
  return data;
}

export async function getAccount(
  userId: string,
  portfolioId: string,
  accountId: string
): Promise<AccountView> {
  const { data } = await apiClient.get<AccountView>(
    `${base(portfolioId)}/${accountId}`,
    { params: userIdParam(userId) }
  );
  return data;
}

export async function getRealizedGains(
  portfolioId: string,
  accountId: string,
  opts: {
    taxYear?: number;
    symbol?: string;
    page?: number;
    size?: number;
  } = {}
): Promise<RealizedGainsSummaryResponse> {
  const { data } = await apiClient.get<RealizedGainsSummaryResponse>(
    `${base(portfolioId)}/${accountId}/realized-gains`,
    { params: opts }
  );
  return data;
}

// ─── Mutations ────────────────────────────────────────────────────────────────

export async function createAccount(
  userId: string,
  portfolioId: string,
  body: CreateAccountRequest
): Promise<AccountView> {
  const { data } = await apiClient.post<AccountView>(base(portfolioId), body, {
    params: userIdParam(userId),
  });
  return data;
}

export async function updateAccount(
  userId: string,
  portfolioId: string,
  accountId: string,
  body: UpdateAccountRequest
): Promise<void> {
  await apiClient.put(
    `${base(portfolioId)}/${accountId}`,
    body,
    { params: userIdParam(userId) }
  );
}

/** Transitions the account to CLOSED. Requires zero balance + no positions. */
export async function closeAccount(
  userId: string,
  portfolioId: string,
  accountId: string
): Promise<void> {
  await apiClient.delete(`${base(portfolioId)}/${accountId}`, {
    params: userIdParam(userId),
  });
}

/** Re-activates a CLOSED account, preserving all transaction history. */
export async function reopenAccount(
  userId: string,
  portfolioId: string,
  accountId: string
): Promise<void> {
  await apiClient.patch(
    `${base(portfolioId)}/${accountId}/reopen`,
    null,
    { params: userIdParam(userId) }
  );
}