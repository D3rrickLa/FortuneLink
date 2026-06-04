import apiClient from "@/lib/api/client";
import type {
  CreatePortfolioRequest,
  UpdatePortfolioRequest,
  PortfolioResponse,
  PortfolioSummaryResponse,
} from "@/lib/api/types";

const BASE = "/api/v1/portfolios";

// ─── Queries ──────────────────────────────────────────────────────────────────
 
/**
 * Returns a flat summary list for all portfolios owned by the authenticated
 * user. Auth is resolved from the JWT; no explicit userId needed.
 */
export async function getPortfolios(): Promise<PortfolioSummaryResponse[]> {
  const { data } = await apiClient.get<PortfolioSummaryResponse[]>(BASE);
  return data;
}
 
/** Full portfolio detail including account list and stale-data flag. */
export async function getPortfolio(
  portfolioId: string
): Promise<PortfolioResponse> {
  const { data } = await apiClient.get<PortfolioResponse>(
    `${BASE}/${portfolioId}`
  );
  return data;
}
 
// ─── Mutations ────────────────────────────────────────────────────────────────
 
/** Creates a new portfolio, optionally with a first account. */
export async function createPortfolio(
  body: CreatePortfolioRequest
): Promise<PortfolioResponse> {
  const { data } = await apiClient.post<PortfolioResponse>(BASE, body);
  return data;
}
 
/** Updates name, description, or base currency of an existing portfolio. */
export async function updatePortfolio(
  portfolioId: string,
  body: UpdatePortfolioRequest
): Promise<PortfolioResponse> {
  const { data } = await apiClient.patch<PortfolioResponse>(
    `${BASE}/${portfolioId}`,
    body
  );
  return data;
}
 
/**
 * Deletes a portfolio.
 * `softDelete` defaults to true for non-admin users (enforced server-side).
 * `recursive` will cascade to all child accounts and transactions.
 */
export async function deletePortfolio(
  portfolioId: string,
  opts: { softDelete?: boolean; recursive?: boolean } = {}
): Promise<void> {
  await apiClient.delete(`${BASE}/${portfolioId}`, { params: opts });
}