import apiClient from "@/lib/api/client";
import type {
  MarketQuoteResponse,
  AssetInfoResponse,
  SymbolSearchResponse,
  BatchQuoteRequest,
} from "@/lib/api/types";

const BASE = "/api/v1/market";

// ─── Symbol lifecycle ─────────────────────────────────────────────────────────

/**
 * MUST be called before recording a BUY transaction.
 * Checks internal DB cache, then fans out to FMP if not found.
 * Seeds the internal database on success.
 * Rate limit: 20/min.
 */
export async function validateSymbol(
  symbol: string
): Promise<AssetInfoResponse> {
  const { data } = await apiClient.get<AssetInfoResponse>(
    `${BASE}/validate/${encodeURIComponent(symbol)}`
  );
  return data;
}

/**
 * Autocomplete search — returns shallow results (name, exchange, currency).
 * Caller is responsible for debouncing (minimum 300ms recommended).
 * Rate limit: 30/min.
 */
export async function searchSymbols(
  query: string
): Promise<SymbolSearchResponse[]> {
  const { data } = await apiClient.get<SymbolSearchResponse[]>(
    `${BASE}/search`,
    { params: { query } }
  );
  return data;
}

// ─── Quotes ───────────────────────────────────────────────────────────────────

/**
 * Single quote — Redis-cached with a 5-minute TTL.
 * Returns 404 if symbol has no transaction history in the system.
 */
export async function getQuote(symbol: string): Promise<MarketQuoteResponse> {
  const { data } = await apiClient.get<MarketQuoteResponse>(
    `${BASE}/quotes/${encodeURIComponent(symbol)}`
  );
  return data;
}

/**
 * Batch quotes — primary endpoint for portfolio views.
 * Hard limit: 20 symbols per request.
 * Uses Redis MGET; fans out to FMP on cache miss.
 * Rate limit: 10/min.
 */
export async function getBatchQuotes(
  symbols: string[]
): Promise<Record<string, MarketQuoteResponse>> {
  if (symbols.length === 0) return {};
  if (symbols.length > 20) {
    throw new Error("getBatchQuotes: hard limit is 20 symbols per request");
  }
  const body: BatchQuoteRequest = { symbols };
  const { data } = await apiClient.post<Record<string, MarketQuoteResponse>>(
    `${BASE}/quotes/batch`,
    body
  );
  return data;
}

// ─── Asset metadata ───────────────────────────────────────────────────────────

/**
 * Slow-changing metadata: sector, type, trading currency.
 * DB-cached for 7 days — safe to call on every page load.
 */
export async function getAssetInfo(
  symbol: string
): Promise<AssetInfoResponse> {
  const { data } = await apiClient.get<AssetInfoResponse>(
    `${BASE}/info/${encodeURIComponent(symbol)}`
  );
  return data;
}