import type { AccountView, PositionView } from "@/lib/api/types";

export interface GainLossSummary {
  totalGainLoss: number;
  totalGainLossPercent: number;
  /** True when derived from real data; false means the API doesn't expose this yet */
  isAvailable: boolean;
}

/**
 * Derives unrealized gain/loss for a single account by summing across all
 * positions in AccountView.assets.
 *
 * This works because PositionView already carries unrealizedPnL (Money) and
 * totalCostBasis (Price) — the backend computes these per position.
 *
 * NOTE: This is UNREALIZED gain/loss only. Realized gains live on a separate
 * endpoint (/realized-gains) and are not included here.
 */
export function deriveAccountGainLoss(account: AccountView): GainLossSummary {
  const positions: PositionView[] = account.assets ?? [];

  if (positions.length === 0) {
    return { totalGainLoss: 0, totalGainLossPercent: 0, isAvailable: true };
  }

  const totalUnrealizedPnL = positions.reduce((sum, pos) => {
    return sum + (pos.unrealizedPnL?.amount ?? 0);
  }, 0);

  // Cost basis = sum of (averageCostPerUnit.pricePerUnit.amount * quantity.amount)
  // We use totalCostBasis directly: it's already (avg cost * qty) per position.
  const totalCostBasis = positions.reduce((sum, pos) => {
    return sum + (pos.totalCostBasis?.pricePerUnit?.amount ?? 0);
  }, 0);

  const percent =
    totalCostBasis > 0 ? (totalUnrealizedPnL / totalCostBasis) * 100 : 0;

  return {
    totalGainLoss: totalUnrealizedPnL,
    totalGainLossPercent: percent,
    isAvailable: true,
  };
}

/**
 * Portfolio-level and all-portfolios gain/loss is NOT available from the
 * current API contract:
 *
 *   - NetWorthResponse only has { totalNetWorth, currency }
 *   - PortfolioSummaryResponse only has { id, name, currency, totalValue }
 *
 * To support this properly you need one of:
 *   a) A new backend field on NetWorthResponse (totalUnrealizedGainLoss, costBasis)
 *   b) A dedicated /portfolios/{id}/performance endpoint
 *   c) Fetching all accounts and summing — expensive, not recommended client-side
 *
 * Until then, return honest zeros rather than fabricating numbers.
 */
export const GAIN_LOSS_UNAVAILABLE: GainLossSummary = {
  totalGainLoss: 0,
  totalGainLossPercent: 0,
  isAvailable: false,
};