/**
 * Centralised React Query cache key factory.
 *
 * Rules:
 * - Every key is a const tuple so TypeScript narrows it correctly.
 * - Hierarchical: broader keys nest narrower ones so a single
 *   queryClient.invalidateQueries({ queryKey: queryKeys.portfolios.all })
 *   wipes everything portfolio-related without listing every sub-key.
 * - No magic strings outside this file.
 */

export const queryKeys = {
  // ── Portfolios ────────────────────────────────────────────────────────────
  portfolios: {
    all: ["portfolios"] as const,
    list: () => [...queryKeys.portfolios.all, "list"] as const,
    detail: (id: string) => [...queryKeys.portfolios.all, "detail", id] as const,
    netWorth: (id: string) => [...queryKeys.portfolios.all, "netWorth", id] as const,
  },

  // ── Accounts ──────────────────────────────────────────────────────────────
  accounts: {
    all: (portfolioId: string) => ["accounts", portfolioId] as const,
    list: (portfolioId: string) =>
      [...queryKeys.accounts.all(portfolioId), "list"] as const,
    detail: (portfolioId: string, accountId: string) =>
      [...queryKeys.accounts.all(portfolioId), "detail", accountId] as const,
  },

  // ── Transactions ──────────────────────────────────────────────────────────
  transactions: {
    all: (portfolioId: string, accountId: string) =>
      ["transactions", portfolioId, accountId] as const,
    list: (portfolioId: string, accountId: string) =>
      [...queryKeys.transactions.all(portfolioId, accountId), "list"] as const,
    detail: (portfolioId: string, accountId: string, transactionId: string) =>
      [
        ...queryKeys.transactions.all(portfolioId, accountId),
        "detail",
        transactionId,
      ] as const,
  },

  // ── Valuations ────────────────────────────────────────────────────────────
  // These are user-scoped (all portfolios), not portfolio-scoped, so they live
  // at the top level rather than nested under portfolios.
  valuations: {
    all: ["valuations"] as const,
    summary: () => [...queryKeys.valuations.all, "summary"] as const,
    history: (days: number) =>
      [...queryKeys.valuations.all, "history", days] as const,
  },

  // ── Market data ───────────────────────────────────────────────────────────
  market: {
    all: ["market"] as const,
    search: (query: string) => [...queryKeys.market.all, "search", query] as const,
    quote: (symbol: string) => [...queryKeys.market.all, "quote", symbol] as const,
    info: (symbol: string) => [...queryKeys.market.all, "info", symbol] as const,
  },

  // ── Realized gains ────────────────────────────────────────────────────────
  realizedGains: {
    all: (portfolioId: string, accountId: string) =>
      ["realizedGains", portfolioId, accountId] as const,
    list: (portfolioId: string, accountId: string, taxYear?: number, symbol?: string | undefined) =>
      [
        ...queryKeys.realizedGains.all(portfolioId, accountId),
        "list",
        taxYear ?? "all",
        symbol ?? "",
      ] as const,
  },

  // ── DEPRECATED — remove once PerformanceChart migration is complete ───────
  /** @deprecated Use queryKeys.valuations.history instead */
  netWorthHistory: (days?: number) => ["netWorthHistory", days ?? "all"] as const,
} as const;