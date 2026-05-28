"use client";

import { useMemo, useState } from "react";

import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from "recharts";

import { TrendingUp, TrendingDown, Clock } from "lucide-react";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import { Skeleton } from "@/components/ui/skeleton";

import {
  useValuation,
  useAccountValuation,
  useValuationChart,
} from "@/features/portfolio/queries/useValuation";

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
        (now.getTime() - new Date(now.getFullYear(), 0, 1).getTime()) /
          86_400_000
      );
    }
    case "all":
      return 365;
  }
}

// ---------------------------------------------------------------------------
// Props
//
// portfolioId / accountId accept null because the dashboard passes null when
// "All Portfolios" is selected rather than undefined. Both are treated as
// "no selection" inside the hooks.
// ---------------------------------------------------------------------------

interface PerformanceChartProps {
  portfolioId?: string | null;
  accountId?: string | null;
  currency?: string;
}

// ---------------------------------------------------------------------------
// Formatters
// ---------------------------------------------------------------------------

function makeFmt(currency: string) {
  return (v: number) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
      minimumFractionDigits: 2,
    }).format(v ?? 0);
}

function makeCompactFmt(currency: string) {
  return (v: number) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
      notation: "compact",
      maximumFractionDigits: 1,
    }).format(v ?? 0);
}

// ---------------------------------------------------------------------------
// Metric card
// ---------------------------------------------------------------------------

function Metric({
  label,
  value,
  highlight,
}: {
  label: string;
  value: string;
  highlight?: "gain" | "loss";
}) {
  return (
    <div className="rounded-lg bg-muted/40 px-3 py-2">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p
        className={`mt-0.5 text-sm font-semibold tabular-nums ${
          highlight === "gain"
            ? "text-green-600"
            : highlight === "loss"
            ? "text-red-600"
            : "text-foreground"
        }`}
      >
        {value}
      </p>
    </div>
  );
}

// ---------------------------------------------------------------------------
// Component
// ---------------------------------------------------------------------------

export function PerformanceChart({
  portfolioId,
  accountId,
  currency = "CAD",
}: PerformanceChartProps) {
  const [period, setPeriod] = useState<TimePeriod>("3m");
  const accountMode = !!accountId;

  // ── Data hooks ─────────────────────────────────────────────────────────────

  const valuation = useValuation(portfolioId, !accountMode);
  const accountValuation = useAccountValuation(portfolioId, accountId, accountMode);

  const current = accountMode ? accountValuation : valuation;

  // History is now fetched dynamically for both account and portfolio scopes
  const enabled = !!(portfolioId || accountId);
  const history = useValuationChart(getDays(period), portfolioId, accountId, enabled);

  const resolvedCurrency = current.currency ?? currency;

  const fmt = useMemo(() => makeFmt(resolvedCurrency), [resolvedCurrency]);
  const fmtCompact = useMemo(
    () => makeCompactFmt(resolvedCurrency),
    [resolvedCurrency]
  );

  const safeCompactFmt = (v: unknown) => {
    const num = typeof v === "number" ? v : Number(v);
    return Number.isFinite(num) ? fmtCompact(num) : "";
  };

  const chartData = useMemo(() => {
    if (!Array.isArray(history.points)) return [];
    return history.points.filter(
      (p) => p && p.date && typeof p.value === "number"
    );
  }, [history.points]);

  const strokeColor = useMemo(() => {
    if (!chartData.length) return "#6b7280";
    const first = chartData[0].value;
    const last = chartData[chartData.length - 1].value;
    return last >= first ? "#16a34a" : "#dc2626";
  }, [chartData]);

  // ── Derived loading / error states ─────────────────────────────────────────

  const isLoading = current.isLoading || history.isLoading;
  const isError = current.isError || history.isError;

  // ── Loading ────────────────────────────────────────────────────────────────

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

  // ── Error ──────────────────────────────────────────────────────────────────

  if (isError) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Performance</CardTitle>
        </CardHeader>
        <CardContent className="flex h-[300px] items-center justify-center">
          <p className="text-sm text-destructive">
            Failed to load performance data.
          </p>
        </CardContent>
      </Card>
    );
  }

  // ── Empty state (chart data not yet available) ──────────────────────────────

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
              Valuation snapshots are recorded daily. Check back tomorrow once
              your first snapshot has been captured.
            </p>
          </div>
          <p className="text-base font-semibold">
            Current: {fmt(current.totalValue)}
          </p>
        </CardContent>
      </Card>
    );
  }

  // ── Main chart (Shared across Portfolios and Accounts) ──────────────────────

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <CardTitle>Performance</CardTitle>
            <div
              className={`flex items-center gap-2 text-sm font-semibold ${
                current.unrealizedGainLoss >= 0
                  ? "text-green-600"
                  : "text-red-600"
              }`}
            >
              {current.unrealizedGainLoss >= 0 ? (
                <TrendingUp className="h-4 w-4" />
              ) : (
                <TrendingDown className="h-4 w-4" />
              )}
              <span>
                {current.unrealizedGainLoss >= 0 ? "+" : ""}
                {fmt(current.unrealizedGainLoss)}
              </span>
              <span className="font-normal text-muted-foreground">
                ({current.returnPercentage >= 0 ? "+" : ""}
                {current.returnPercentage.toFixed(2)}%)
              </span>
            </div>
            <p className="text-xs text-muted-foreground">Total value</p>
          </div>

          <div className="flex gap-1">
            {PERIODS.map((p) => (
              <button
                key={p.value}
                onClick={() => setPeriod(p.value)}
                className={`rounded-md px-3 py-1 text-xs font-medium transition-colors ${
                  period === p.value
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
        <ResponsiveContainer width="100%" height={300}>
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
              tickFormatter={(v) => safeCompactFmt(v)}
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
              formatter={(value: unknown) => {
                const num = typeof value === "number" ? value : Number(value);
                return [Number.isFinite(num) ? fmt(num) : "", "Value"];
              }}
            />
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
      </CardContent>
    </Card>
  );
}