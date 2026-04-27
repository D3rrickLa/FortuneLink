import apiClient from "@/lib/api/client";
import type {
  AccountView,
  PageAccountView,
  CreateAccountRequest,
  UpdateAccountRequest,
  PaginationParams,
} from "@/lib/api/types";

/**
 * NOTE on `userId`:
 * The backend expects a `userId` query parameter (UUID string) on most account
 * endpoints. This is separate from the JWT, the backend uses it for ownership
 * scoping. Pass `user.id` from the Supabase session here.
 *
 * If the backend changes to resolve userId purely from the JWT, remove these params.
 */

export interface GetAllAccountsParams extends PaginationParams {
  userId: string;
}

export const accountService = {
  /**
   * Returns all accounts in the portfolio with live market valuation.
   * Cache results where possible — this hits market data on every call.
   */
  getAll: async (
    portfolioId: string,
    params: GetAllAccountsParams
  ): Promise<PageAccountView> => {
    const { data } = await apiClient.get<PageAccountView>(
      `/api/v1/portfolios/${portfolioId}/accounts`,
      { params }
    );
    return data;
  },

  /**
   * Returns a single account with current positions and live market valuation.
   */
  getOne: async (
    portfolioId: string,
    accountId: string,
    userId: string
  ): Promise<AccountView> => {
    const { data } = await apiClient.get<AccountView>(
      `/api/v1/portfolios/${portfolioId}/accounts/${accountId}`,
      { params: { userId } }
    );
    return data;
  },

  /**
   * Initializes a new account within a portfolio.
   */
  create: async (
    portfolioId: string,
    userId: string,
    body: CreateAccountRequest
  ): Promise<AccountView> => {
    const { data } = await apiClient.post<AccountView>(
      `/api/v1/portfolios/${portfolioId}/accounts`,
      body,
      { params: { userId } }
    );
    return data;
  },

  /**
   * Updates mutable properties of an account (currently just display name).
   * Returns 204 — no response body.
   */
  update: async (
    portfolioId: string,
    accountId: string,
    userId: string,
    body: UpdateAccountRequest
  ): Promise<void> => {
    await apiClient.put(
      `/api/v1/portfolios/${portfolioId}/accounts/${accountId}`,
      body,
      { params: { userId } }
    );
  },

  /**
   * Transitions account to CLOSED state.
   * Requires zero cash balance and no open positions — will 409 otherwise.
   */
  close: async (
    portfolioId: string,
    accountId: string,
    userId: string
  ): Promise<void> => {
    await apiClient.delete(
      `/api/v1/portfolios/${portfolioId}/accounts/${accountId}`,
      { params: { userId } }
    );
  },

  /**
   * Transitions a CLOSED account back to ACTIVE, preserving all history.
   */
  reopen: async (
    portfolioId: string,
    accountId: string,
    userId: string
  ): Promise<void> => {
    await apiClient.patch(
      `/api/v1/portfolios/${portfolioId}/accounts/${accountId}/reopen`,
      null,
      { params: { userId } }
    );
  },
};