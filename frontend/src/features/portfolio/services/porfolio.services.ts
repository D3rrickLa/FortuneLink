import apiClient from "@/lib/api/client";
import type {
  PortfolioSummaryResponse,
  PortfolioResponse,
  CreatePortfolioRequest,
  UpdatePortfolioRequest,
  NetWorthResponse,
} from "@/lib/api/types";

export interface DeletePortfolioParams {
  /** Forces soft-delete for regular users. Admins can set false. */
  softDelete?: boolean;
  /** If true, cascades deletion to all child accounts and transactions. */
  rescursive?: boolean;
}

export const portfolioService = {
  /**
   * Returns a summary list of all portfolios for the authenticated user.
   * Auth is resolved from the JWT — no userId param required here.
   */
  getAll: async (): Promise<PortfolioSummaryResponse[]> => {
    const { data } = await apiClient.get<PortfolioSummaryResponse[]>(
      "/api/v1/portfolios"
    );
    return data;
  },

  /**
   * Returns full details for a specific portfolio including all member accounts.
   */
  getOne: async (portfolioId: string): Promise<PortfolioResponse> => {
    const { data } = await apiClient.get<PortfolioResponse>(
      `/api/v1/portfolios/${portfolioId}`
    );
    return data;
  },

  /**
   * Initializes a portfolio for the authenticated user.
   * Optionally creates a default account in the same call.
   */
  create: async (body: CreatePortfolioRequest): Promise<PortfolioResponse> => {
    const { data } = await apiClient.post<PortfolioResponse>(
      "/api/v1/portfolios",
      body
    );
    return data;
  },

  /**
   * Updates the name, description, or base currency of an existing portfolio.
   */
  update: async (
    portfolioId: string,
    body: UpdatePortfolioRequest
  ): Promise<PortfolioResponse> => {
    const { data } = await apiClient.patch<PortfolioResponse>(
      `/api/v1/portfolios/${portfolioId}`,
      body
    );
    return data;
  },

  /**
   * Removes a portfolio. Regular users are forced to soft-delete.
   * `recursive: true` will also delete all child accounts and transactions.
   */
  delete: async (
    portfolioId: string,
    params: DeletePortfolioParams = {}
  ): Promise<void> => {
    await apiClient.delete(`/api/v1/portfolios/${portfolioId}`, { params });
  },

  /**
   * Calculates total valuation of the portfolio in its base currency.
   * Triggers real-time pricing for all underlying assets — avoid high-frequency polling.
   */
  getNetWorth: async (portfolioId: string): Promise<NetWorthResponse> => {
    const { data } = await apiClient.get<NetWorthResponse>(
      `/api/v1/portfolios/${portfolioId}/net-worth`
    );
    return data;
  },
};
