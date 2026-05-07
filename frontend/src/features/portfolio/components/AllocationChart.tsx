"use client";

import { useMemo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Legend,
  Tooltip,
} from "recharts";
import { Skeleton } from "@/components/ui/skeleton";
import type { AccountView, PositionView } from "@/lib/api/types";

// ── Types ─────────────────────────────────────────────────────────────────────

interface AllocationSlice {
  name: string;
  value: number;   // market value in account currency
  percent: number; // 0–100
}

// ── Derivation ────────────────────────────────────────────────────────────────

/**
 * Derives allocation by asset type from the positions already held in
 * AccountView.assets. This avoids an extra network call — the data is already
 * in the React Query cache from useAccount.
 *
 * For portfolio-level allocation you need the backend's AssetAllocationService
 * exposed via a REST endpoint. That work is tracked separately.
 */
function deriveAllocation(account: AccountView): AllocationSlice[] {
  const positions: PositionView[] = account.assets ?? [];
  const cashBalance = account.cashBalance?.amount ?? 0;

  // Bucket positions by assetType, summing marketValue
  const buckets = new Map<string, number>();

  for (const pos of positions) {
    const type = pos.assetType ?? "OTHER";
    const marketValue = pos.marketValue?.amount ?? 0;
    buckets.set(type, (buckets.get(type) ?? 0) + marketValue);
  }

  // Add cash as its own bucket if non-zero
  if (cashBalance > 0) {
    buckets.set("CASH", (buckets.get("CASH") ?? 0) + cashBalance);
  }

  const totalValue = [...buckets.values()].reduce((a, b) => a + b, 0);
  if (totalValue === 0) return [];

  return [...buckets.entries()]
    .map(([name, value]) => ({
      name: formatAssetType(name),
      value,
      percent: (value / totalValue) * 100,
    }))
    .sort((a, b) => b.value - a.value); // largest slice first
}

function formatAssetType(raw: string): string {
  const map: Record<string, string> = {
    STOCK: "Stocks",
    ETF: "ETFs",
    CRYPTO: "Crypto",
    BOND: "Bonds",
    CASH: "Cash",
    COMMODITY: "Commodities",
    REAL_ESTATE: "Real Estate",
    FOREX_PAIR: "Forex",
    OTHER: "Other",
  };
  return map[raw] ?? raw.replace(/_/g, " ");
}

// ── Chart colours ─────────────────────────────────────────────────────────────

const COLORS = [
  "#3b82f6", // blue
  "#10b981", // emerald
  "#f59e0b", // amber
  "#8b5cf6", // violet
  "#ef4444", // red
  "#ec4899", // pink
  "#14b8a6", // teal
  "#f97316", // orange
];

// ── Props ─────────────────────────────────────────────────────────────────────

interface AllocationChartProps {
  /**
   * Provide the full AccountView when an account is selected. When null/undefined
   * (portfolio or all-portfolios view), the chart renders an informational
   * placeholder until the allocation endpoint is implemented on the backend.
   */
  account?: AccountView | null;
  isLoading?: boolean;
}

// ── Component ─────────────────────────────────────────────────────────────────

export function AllocationChart({ account, isLoading }: AllocationChartProps) {
  const slices = useMemo(
    () => (account ? deriveAllocation(account) : []),
    [account]
  );

  const currency = account?.baseCurrency?.code ?? "USD";

  // ── Loading ──
  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Asset Allocation</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-[300px] w-full" />
        </CardContent>
      </Card>
    );
  }

  // ── No account selected (portfolio / all view) ──
  if (!account) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Asset Allocation</CardTitle>
        </CardHeader>
        <CardContent className="flex h-[300px] items-center justify-center">
          <p className="text-sm text-center text-muted-foreground max-w-[220px]">
            Select an account to view its allocation breakdown.
            <br />
            <span className="text-xs mt-1 block opacity-70">
              Portfolio-level allocation is coming in a future update.
            </span>
          </p>
        </CardContent>
      </Card>
    );
  }

  // ── Account selected but no positions ──
  if (slices.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Asset Allocation</CardTitle>
        </CardHeader>
        <CardContent className="flex h-[300px] items-center justify-center">
          <p className="text-sm text-muted-foreground">
            No holdings to display.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Asset Allocation</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-[300px]">
          <ResponsiveContainer width="100%" height={400}>
            <PieChart>
              <Pie
                data={slices}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={100}
                paddingAngle={2}
                dataKey="value"
              >
                {slices.map((_, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={COLORS[index % COLORS.length]}
                  />
                ))}
              </Pie>
              <Tooltip
                formatter={(value: any | undefined) => {
                  if (value === undefined) return "";
                  return new Intl.NumberFormat("en-US", {
                    style: "currency",
                    currency,
                    minimumFractionDigits: 2,
                  }).format(value);
                }}
              />
              <Legend
                formatter={(value, entry: any) => {
                  const slice = slices.find((s) => s.name === value);
                  return slice
                    ? `${value} (${slice.percent.toFixed(1)}%)`
                    : value;
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}