import apiClient from "@/lib/api/client";
import type { 
  SupportedCurrenciesResponse, 
  ExchangeRateResponse 
} from "@/lib/api/types";

const BASE = "/api/v1/exchange-rates";

/**
 * Returns all ISO-4217 codes available for exchange rate lookup.
 * Sourced from Bank of Canada.
 */
export async function getSupportedCurrencies(): Promise<string[]> {
  const { data } = await apiClient.get<SupportedCurrenciesResponse>(`${BASE}/supported`);
  return data.currencies || [];
}

/**
 * Returns the rate for '1 {from} = X {to}'.
 * Rates are cached for 1 hour. Cross-pairs are computed via CAD triangulation.
 * * @param from - Source currency code (e.g., 'USD')
 * @param to - Target currency code (e.g., 'CAD')
 */
export async function getCurrentRate(from: string, to: string): Promise<ExchangeRateResponse> {
  const { data } = await apiClient.get<ExchangeRateResponse>(`${BASE}/current`, {
    params: { from, to }
  });
  return data;
}