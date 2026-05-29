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

import {
  TrendingUp,
  TrendingDown,
  Clock,
} from "lucide-react";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import { Skeleton } from "@/components/ui/skeleton";

import {
  useValuation,
  useValuationChart,
} from "@/features/portfolio/hooks/useValuations";

// ─────────────────────────────────────────────

type TimePeriod = "1m" | "3m" | "ytd" | "all";

const PERIODS: { value: TimePeriod; label: string }[] = [
  { value: "1m", label: "1M" },
  { value: "3m", label: "3M" },
  { value: "ytd", label: "YTD" },
  { value: "all", label: "All" },
];

function getDays(period: TimePeriod) {
  switch (period) {
    case "1m":
      return 30;
    case "3m":
      return 90;
    case "ytd": {
      const now = new Date();
      return Math.ceil(
        (now.getTime() -
          new Date(now.getFullYear(), 0, 1).getTime()) /
        86_400_000
      );
    }
    case "all":
      return 365;
  }
}

// ─────────────────────────────────────────────

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
        className={`mt-0.5 text-sm font-semibold tabular-nums ${highlight === "gain"
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

// ─────────────────────────────────────────────

export function PerformanceChart({
  portfolioId,
  accountId,
  currency = "CAD",
}: {
  portfolioId?: string | null;
  accountId?: string | null;
  currency?: string;
}) {
  const [period, setPeriod] = useState<TimePeriod>("3m");
  const accountMode = !!portfolioId && !!accountId;
  const portfolioMode = !!portfolioId && !accountId;
  const showMetricsPanel = portfolioMode;

  const scope = useMemo(
    () => ({ portfolioId, accountId }),
    [portfolioId, accountId]
  );

  const current = useValuation(scope, true);
  const history = useValuationChart(getDays(period), scope);

  const resolvedCurrency =
    current.currency ?? currency;

  const fmt = useMemo(
    () =>
      new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: resolvedCurrency,
        minimumFractionDigits: 2,
      }),
    [resolvedCurrency]
  );

  const fmtCompact = useMemo(
    () =>
      new Intl.NumberFormat("en-US", {
        style: "currency",
        currency: resolvedCurrency,
        notation: "compact",
        maximumFractionDigits: 1,
      }),
    [resolvedCurrency]
  );

  const chartData = history.points ?? [];

  const isLoading =
    current.isLoading || history.isLoading;

  const isError =
    current.isError || history.isError;

  // ─────────────────────────────────────────────
  // Loading / Error
  // ─────────────────────────────────────────────

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Performance</CardTitle>
        </CardHeader>
        <CardContent>
          <Skeleton className="h-[300px]" />
        </CardContent>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Performance</CardTitle>
        </CardHeader>
        <CardContent className="flex h-[300px] items-center justify-center">
          <p className="text-sm text-destructive">
            Failed to load performance history.
          </p>
        </CardContent>
      </Card>
    );
  }

  // ─────────────────────────────────────────────
  // Metrics panel (account OR portfolio works now)
  // ─────────────────────────────────────────────

  const metricsPanel = (
    <div className="grid grid-cols-2 gap-4 mb-4">
      <Metric
        label="Total Value"
        value={fmt.format(current.totalValue)}
      />
      <Metric
        label="Cost Basis"
        value={fmt.format(current.totalCostBasis)}
      />
      <Metric
        label="Cash Balance"
        value={fmt.format(current.totalCashBalance)}
      />
      <Metric
        label="Invested"
        value={fmt.format(current.totalInvestedValue)}
      />
      <Metric
        label="Unrealized P&L"
        value={`${current.unrealizedGainLoss >= 0 ? "+" : ""}${fmt.format(
          current.unrealizedGainLoss
        )}`}
        highlight={
          current.unrealizedGainLoss >= 0 ? "gain" : "loss"
        }
      />
      <Metric
        label="Return"
        value={`${current.returnPercentage >= 0 ? "+" : ""}${current.returnPercentage.toFixed(
          2
        )}%`}
        highlight={
          current.returnPercentage >= 0 ? "gain" : "loss"
        }
      />
    </div>
  );

  // ─────────────────────────────────────────────
  // Empty state
  // ─────────────────────────────────────────────

  if (chartData.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Performance</CardTitle>
        </CardHeader>

        <CardContent className="flex flex-col gap-4">
          {showMetricsPanel && metricsPanel}

          <div className="flex h-[200px] flex-col items-center justify-center gap-3 text-center">
            <Clock className="h-8 w-8 text-muted-foreground opacity-40" />

            <div className="space-y-1">
              <p className="text-sm font-medium">
                No history yet
              </p>
              <p className="max-w-[240px] text-xs text-muted-foreground">
                Valuation snapshots are recorded daily.
              </p>
            </div>

            <p className="text-base font-semibold">
              Current: {fmt.format(current.totalValue)}
            </p>
          </div>
        </CardContent>
      </Card>
    );
  }

  // ─────────────────────────────────────────────
  // Chart
  // ─────────────────────────────────────────────

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <CardTitle>
              {accountMode
                ? "Account Performance"
                : "Portfolio Performance"}
            </CardTitle>
            <div className={`flex items-center gap-2 text-sm font-semibold ${current.unrealizedGainLoss >= 0 ? "text-green-600" : "text-red-600"
              }`}>
              {current.unrealizedGainLoss >= 0
                ? <TrendingUp className="h-4 w-4" />
                : <TrendingDown className="h-4 w-4" />}
              <span>
                {current.unrealizedGainLoss >= 0 ? "+" : ""}
                {fmt.format(current.unrealizedGainLoss)}
              </span>
              <span className="font-normal text-muted-foreground">
                ({current.returnPercentage >= 0 ? "+" : ""}
                {current.returnPercentage.toFixed(2)}%)
              </span>
            </div>
            <p className="text-xs text-muted-foreground">
              {accountMode ? "Account net value" : "Total net worth"}
            </p>
          </div>

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

      <CardContent className="space-y-4">
        {showMetricsPanel && metricsPanel}

        <div className="h-[300px]">
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />

              <XAxis
                dataKey="date"
                tickFormatter={(d) =>
                  new Date(d).toLocaleDateString("en-CA", {
                    month: "short",
                    day: "numeric",
                  })
                }
              />

              <YAxis
                tickFormatter={(v) =>
                  fmtCompact.format(Number(v))
                }
              />

              <Tooltip
                formatter={(v: any) => [
                  fmt.format(Number(v)),
                  "Value",
                ]}
              />

              <Line
                type="monotone"
                dataKey="value"
                stroke="#16a34a"
                strokeWidth={2}
                dot={false}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}