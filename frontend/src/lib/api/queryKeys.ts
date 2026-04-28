/**
 * Centralised React Query key factory.
 *
 * Rules:
 *   1. Always use these keys — never inline strings in useQuery/useMutation.
 *   2. Broader keys are prefixes of narrower ones, so invalidating a parent
 *      also invalidates all children (queryClient.invalidateQueries).
 *   3. Keys are `as const` tuples for strict type inference.
 */

export const queryKeys = {
  // ── Portfolios ────────────────────────────────────────────────────────────
  portfolios: {
    all: () => ["portfolios"] as const,
    list: () => ["portfolios", "list"] as const,
    detail: (portfolioId: string) =>
      ["portfolios", portfolioId] as const,
    netWorth: (portfolioId: string) =>
      ["portfolios", portfolioId, "net-worth"] as const,
  },

  // ── Accounts ──────────────────────────────────────────────────────────────
  accounts: {
    all: (portfolioId: string) =>
      ["portfolios", portfolioId, "accounts"] as const,
    detail: (portfolioId: string, accountId: string) =>
      ["portfolios", portfolioId, "accounts", accountId] as const,
    realizedGains: (
      portfolioId: string,
      accountId: string,
      taxYear?: number,
      symbol?: string
    ) =>
      [
        "portfolios",
        portfolioId,
        "accounts",
        accountId,
        "realized-gains",
        { taxYear, symbol },
      ] as const,
  },

  // ── Transactions ──────────────────────────────────────────────────────────
  transactions: {
    list: (
      portfolioId: string,
      accountId: string,
      filters?: {
        symbol?: string;
        startDate?: string;
        endDate?: string;
        page?: number;
        size?: number;
      }
    ) =>
      [
        "portfolios",
        portfolioId,
        "accounts",
        accountId,
        "transactions",
        filters ?? {},
      ] as const,
    detail: (
      portfolioId: string,
      accountId: string,
      transactionId: string
    ) =>
      [
        "portfolios",
        portfolioId,
        "accounts",
        accountId,
        "transactions",
        transactionId,
      ] as const,
  },

  // ── Net worth history (user-scoped, not portfolio-scoped) ─────────────────
  netWorthHistory: (days?: number) =>
    ["net-worth", "history", { days }] as const,

  // ── Market data ───────────────────────────────────────────────────────────
  market: {
    quote: (symbol: string) => ["market", "quote", symbol] as const,
    batchQuotes: (symbols: string[]) =>
      ["market", "quotes", "batch", [...symbols].sort().join(",")] as const,
    assetInfo: (symbol: string) => ["market", "asset", symbol] as const,
    validateSymbol: (symbol: string) =>
      ["market", "validate", symbol] as const,
    searchSymbols: (query: string) =>
      ["market", "search", query] as const,
    supportedCurrencies: () => ["market", "currencies"] as const,
  },

  // ── Exchange rates ────────────────────────────────────────────────────────
  exchangeRates: {
    current: (from: string, to: string) =>
      ["exchange-rates", from, to] as const,
  },
} as const;