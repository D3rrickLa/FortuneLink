"use client";

import { useMemo, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  ReferenceLine,
} from "recharts";
import { TrendingUp, TrendingDown, Clock } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { useValuationHistory, useValuationSummary } from "@/features/portfolio/queries/useValuation";
import type { AccountView } from "@/lib/api/types";
import { safeNum } from "@/utils/number";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type TimePeriod = "1m" | "3m" | "ytd" | "all";

const PERIODS: { value: TimePeriod; label: string }[] = [
  { value: "1m", label: "1M" },
  { value: "3m", label: "3M" },
  { value: "ytd", label: "YTD" },
  { value: "all", label: "All" },
];

function getDays(period: TimePeriod): number {
  switch (period) {
    case "1m":
      return 30;
    case "3m":
      return 90;
    case "ytd": {
      const now = new Date();
      return Math.ceil(
        (now.getTime() - new Date(now.getFullYear(), 0, 1).getTime()) / 86_400_000
      );
    }
    case "all":
      return 365;
  }
}

// ---------------------------------------------------------------------------
// Props
// ---------------------------------------------------------------------------

interface PerformanceChartProps {
  /** ISO currency code — falls back to USD if not provided. */
  currency?: string;
  /**
   * Optional account context. When provided, a cost-basis reference line is
   * drawn so the user can see how their net worth compares to what they paid.
   */
  account?: AccountView | null;
}

// ---------------------------------------------------------------------------
// Formatters — defined outside the component so they are not recreated on
// every render. Accept currency so they stay pure.
// ---------------------------------------------------------------------------

function makeFmt(currency: string) {
  return (v: number) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
      minimumFractionDigits: 2,
    }).format(v);
}

function makeCompactFmt(currency: string) {
  return (v: number) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
      notation: "compact",
      maximumFractionDigits: 1,
    }).format(v);
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function PerformanceChart({ currency = "USD", account }: PerformanceChartProps) {
  const [period, setPeriod] = useState<TimePeriod>("3m");

  // useValuationSummary gives us the live snapshot shown in the header.
  // useValuationHistory gives us the time-series data for the chart itself.
  const { data: summary } = useValuationSummary();
  const {
    data: snapshots,
    isLoading,
    isError,
  } = useValuationHistory(getDays(period));

  // Resolve the currency from the live summary if available — the prop is
  // a fallback for the case where summary has not loaded yet.
  const resolvedCurrency = summary?.currency ?? currency;

  const fmt = useMemo(() => makeFmt(resolvedCurrency), [resolvedCurrency]);
  const fmtCompact = useMemo(() => makeCompactFmt(resolvedCurrency), [resolvedCurrency]);

  // Normalise snapshots into {date, value} pairs the chart can consume.
  const chartData = useMemo(() => {
    if (!snapshots?.length) return [];
    return snapshots.map((s) => ({
      date: s.snapshotDate ?? "",
      value: s.totalValue ?? 0,
    }));
  }, [snapshots]);

  // Derive the cost-basis reference line from account positions when
  // an account is in context. This is display-only — the real ACB lives
  // on the backend.
  const costBasisLine = useMemo(() => {
    if (!account?.assets?.length) return null;
    const total = account.assets.reduce(
      (sum, pos) => sum + (pos.totalCostBasis?.pricePerUnit?.amount ?? 0),
      0
    );
    return total > 0 ? total : null;
  }, [account]);

  // Period-over-period change metrics derived from the chart window.
  const metrics = useMemo(() => {
    if (chartData.length < 2) return null;
    const start = chartData[0].value;
    const end = chartData[chartData.length - 1].value;
    const change = end - start;
    const pct = start > 0 ? (change / start) * 100 : 0;
    return { change, pct, isPositive: change >= 0 };
  }, [chartData]);

  // ---------------------------------------------------------------------------
  // Loading state
  // ---------------------------------------------------------------------------

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Performance</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-[300px] w-full" />
        </CardContent>
      </Card>
    );
  }

  // ---------------------------------------------------------------------------
  // Error state
  // ---------------------------------------------------------------------------

  if (isError) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Performance</CardTitle>
        </CardHeader>
        <CardContent className="flex h-[300px] items-center justify-center">
          <p className="text-sm text-destructive">Failed to load performance history.</p>
        </CardContent>
      </Card>
    );
  }

  // ---------------------------------------------------------------------------
  // Empty state. New users have no snapshots until the nightly job runs.
  // Be explicit rather than showing a confusing blank chart.
  // ---------------------------------------------------------------------------

  if (chartData.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Performance</CardTitle>
        </CardHeader>
        <CardContent className="flex h-[300px] flex-col items-center justify-center gap-3 text-center">
          <Clock className="h-8 w-8 text-muted-foreground opacity-40" />
          <div className="space-y-1">
            <p className="text-sm font-medium">No history yet</p>
            <p className="max-w-[240px] text-xs text-muted-foreground">
              Net worth snapshots are recorded daily. Check back tomorrow once
              your first snapshot has been captured.
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  // ---------------------------------------------------------------------------
  // Chart
  // ---------------------------------------------------------------------------

  const strokeColor = metrics?.isPositive ? "#16a34a" : "#dc2626";

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <CardTitle>Performance</CardTitle>

            {/* Live summary metrics from the /valuations/summary endpoint */}
            {summary && (
              <div
                className={`flex items-center gap-2 text-sm font-semibold ${(safeNum(summary.unrealizedGainLoss) ?? 0) >= 0
                  ? "text-green-600"
                  : "text-red-600"
                  }`}
              >
                {(safeNum(summary.unrealizedGainLoss) ?? 0) >= 0 ? (
                  <TrendingUp className="h-4 w-4" />
                ) : (
                  <TrendingDown className="h-4 w-4" />
                )}
                <span>
                  {summary.unrealizedGainLoss >= 0 ? "+" : ""}
                  {fmt(summary.unrealizedGainLoss)}
                </span>
                <span className="font-normal text-muted-foreground">
                  ({(summary.returnPercentage ?? 0) >= 0 ? "+" : ""}
                  {(summary.returnPercentage ?? 0).toFixed(2)}%)
                </span>
              </div>
            )}

            {/* Period-level delta as a secondary figure when summary is not available */}
            {!summary && metrics && (
              <div
                className={`flex items-center gap-2 text-sm font-semibold ${metrics.isPositive ? "text-green-600" : "text-red-600"
                  }`}
              >
                {metrics.isPositive ? (
                  <TrendingUp className="h-4 w-4" />
                ) : (
                  <TrendingDown className="h-4 w-4" />
                )}
                <span>
                  {metrics.isPositive ? "+" : ""}
                  {fmt(metrics.change)}
                </span>
                <span className="font-normal text-muted-foreground">
                  ({metrics.isPositive ? "+" : ""}
                  {metrics.pct.toFixed(2)}%)
                </span>
              </div>
            )}

            <p className="text-xs text-muted-foreground">
              Total net worth
            </p>
          </div>

          {/* Period selector */}
          <div className="flex gap-1">
            {PERIODS.map((p) => (
              <button
                key={p.value}
                onClick={() => setPeriod(p.value)}
                className={`rounded-md px-3 py-1 text-xs font-medium transition-colors ${period === p.value
                  ? "bg-primary text-primary-foreground"
                  : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                  }`}
              >
                {p.label}
              </button>
            ))}
          </div>
        </div>
      </CardHeader>

      <CardContent>
        {/* Fixed height wrapper — do not put a height on ResponsiveContainer
            when it is inside a fixed-height div; give the div the height
            and let the container fill it. */}
        <div className="h-[300px]">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart
              data={chartData}
              margin={{ top: 4, right: 8, left: 8, bottom: 4 }}
            >
              <CartesianGrid strokeDasharray="3 3" className="opacity-30" />
              <XAxis
                dataKey="date"
                tick={{ fontSize: 11 }}
                tickFormatter={(iso: string) =>
                  new Date(iso).toLocaleDateString("en-CA", {
                    month: "short",
                    day: "numeric",
                  })
                }
                minTickGap={40}
              />
              <YAxis
                tick={{ fontSize: 11 }}
                tickFormatter={fmtCompact}
                width={72}
              />
              <Tooltip
                labelFormatter={(label: any) =>
                  new Date(label).toLocaleDateString("en-CA", {
                    year: "numeric",
                    month: "short",
                    day: "numeric",
                  })
                }
                formatter={(value: any) => [fmt(value), "Net Worth"]}
              />
              {costBasisLine != null && (
                <ReferenceLine
                  y={costBasisLine}
                  stroke="#94a3b8"
                  strokeDasharray="4 4"
                  label={{
                    value: "Cost Basis",
                    position: "insideTopRight",
                    fontSize: 11,
                    fill: "#94a3b8",
                  }}
                />
              )}
              <Line
                type="monotone"
                dataKey="value"
                stroke={strokeColor}
                strokeWidth={2}
                dot={false}
                activeDot={{ r: 4 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}