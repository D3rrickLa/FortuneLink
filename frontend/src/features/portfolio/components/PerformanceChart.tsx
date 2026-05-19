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
import { TrendingUp, TrendingDown, Clock, Info } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useValuationHistory,
  useValuationSummary,
} from "@/features/portfolio/queries/useValuation";
import type { AccountView } from "@/lib/api/types";
import { safeNum } from "@/utils/number";

// ─── Types ────────────────────────────────────────────────────────────────────

type TimePeriod = "1m" | "3m" | "ytd" | "all";
type ChartScope = "global" | "account";

const PERIODS: { value: TimePeriod; label: string }[] = [
  { value: "1m", label: "1M" },
  { value: "3m", label: "3M" },
  { value: "ytd", label: "YTD" },
  { value: "all", label: "All" },
];

function getDays(period: TimePeriod): number {
  switch (period) {
    case "1m":  return 30;
    case "3m":  return 90;
    case "ytd": {
      const now = new Date();
      return Math.ceil(
        (now.getTime() - new Date(now.getFullYear(), 0, 1).getTime()) / 86_400_000
      );
    }
    case "all": return 365;
  }
}

// ─── Props ────────────────────────────────────────────────────────────────────

interface PerformanceChartProps {
  currency?: string;
  /**
   * Pass the full AccountView when an account is selected in the sidebar.
   * When null/undefined the chart shows global (all-portfolio) performance.
   *
   * NOTE: per-account *historical* snapshots don't exist yet — the backend
   * only records user-level daily snapshots. When an account is active, the
   * chart switches to an account-level current state view instead of
   * silently showing global data.
   */
  account?: AccountView | null;
}

// ─── Formatters ───────────────────────────────────────────────────────────────

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

// ─── Account-level static snapshot ───────────────────────────────────────────
// When an account is selected we don't have historical time-series data, so we
// show a clean breakdown of the account's current state instead of pretending
// the global chart is account-specific.

function AccountSnapshot({ account, currency }: { account: AccountView; currency: string }) {
  const fmt = makeFmt(currency);

  const positions = account.assets ?? [];
  const totalMarketValue = positions.reduce(
    (sum, p) => sum + (p.marketValue?.amount ?? 0),
    0
  );
  const totalCostBasis = positions.reduce(
    (sum, p) => sum + (p.totalCostBasis?.pricePerUnit?.amount ?? 0),
    0
  );
  const totalUnrealizedPnL = positions.reduce(
    (sum, p) => sum + (p.unrealizedPnL?.amount ?? 0),
    0
  );
  const cashBalance = account.cashBalance?.amount ?? 0;
  const totalAccountValue = (account.totalValue?.amount ?? 0);

  const returnPct = totalCostBasis > 0
    ? (totalUnrealizedPnL / totalCostBasis) * 100
    : 0;
  const isPositive = totalUnrealizedPnL >= 0;

  return (
    <div className="flex h-[300px] flex-col justify-between py-2">
      {/* Scope notice */}
      <div className="flex items-start gap-2 rounded-md bg-muted/60 px-3 py-2 text-xs text-muted-foreground">
        <Info className="mt-0.5 h-3.5 w-3.5 shrink-0" />
        <span>
          Per-account historical charts will be available in a future update.
          Showing current account snapshot.
        </span>
      </div>

      {/* Key metrics */}
      <div className="grid grid-cols-2 gap-4 px-1">
        <Metric label="Total Value"    value={fmt(totalAccountValue)} />
        <Metric label="Invested"       value={fmt(totalMarketValue)} />
        <Metric label="Cash Balance"   value={fmt(cashBalance)} />
        <Metric
          label="Unrealized P&L"
          value={`${isPositive ? "+" : ""}${fmt(totalUnrealizedPnL)}`}
          highlight={isPositive ? "gain" : "loss"}
        />
        <Metric label="Cost Basis"     value={fmt(totalCostBasis)} />
        <Metric
          label="Return"
          value={`${isPositive ? "+" : ""}${returnPct.toFixed(2)}%`}
          highlight={isPositive ? "gain" : "loss"}
        />
      </div>

      {/* Position count */}
      <p className="text-center text-xs text-muted-foreground">
        {positions.length} position{positions.length !== 1 ? "s" : ""}
        {positions.length > 0 && (
          <span> · {account.type ?? ""} · {account.baseCurrency?.code ?? currency}</span>
        )}
      </p>
    </div>
  );
}

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

// ─── Main component ───────────────────────────────────────────────────────────

export function PerformanceChart({ currency = "USD", account }: PerformanceChartProps) {
  const [period, setPeriod] = useState<TimePeriod>("3m");

  // Determine rendering scope: account-level snapshot vs global time-series.
  // This is the key fix — we no longer show global history when an account is
  // selected, which was the "chart not updating per account" bug.
  const scope: ChartScope = account ? "account" : "global";

  const { data: summary } = useValuationSummary();
  const {
    data: snapshots,
    isLoading,
    isError,
  } = useValuationHistory(getDays(period));

  const resolvedCurrency =
    (scope === "account"
      ? account?.baseCurrency?.code
      : summary?.currency) ?? currency;

  const fmt        = useMemo(() => makeFmt(resolvedCurrency),        [resolvedCurrency]);
  const fmtCompact = useMemo(() => makeCompactFmt(resolvedCurrency), [resolvedCurrency]);

  // ── Account scope: render snapshot, not time-series ──────────────────────
  if (scope === "account" && account) {
    return (
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle>Performance</CardTitle>
            <span className="text-xs text-muted-foreground">Account View</span>
          </div>
        </CardHeader>
        <CardContent>
          <AccountSnapshot account={account} currency={resolvedCurrency} />
        </CardContent>
      </Card>
    );
  }

  // ── Global scope: time-series chart ──────────────────────────────────────

  const chartData = useMemo(() => {
    if (!snapshots?.length) return [];
    return snapshots.map((s) => ({
      date:  s.snapshotDate ?? "",
      value: s.totalValue   ?? 0,
    }));
  }, [snapshots]);

  // Cost-basis reference line from the global summary when no account is active.
  const costBasisLine = useMemo(() => {
    if (!summary) return null;
    const basis = summary.totalCostBasis ?? 0;
    return basis > 0 ? basis : null;
  }, [summary]);

  const metrics = useMemo(() => {
    if (chartData.length < 2) return null;
    const start  = chartData[0].value;
    const end    = chartData[chartData.length - 1].value;
    const change = end - start;
    const pct    = start > 0 ? (change / start) * 100 : 0;
    return { change, pct, isPositive: change >= 0 };
  }, [chartData]);

  if (isLoading) {
    return (
      <Card>
        <CardHeader><CardTitle>Performance</CardTitle></CardHeader>
        <CardContent><Skeleton className="h-[300px] w-full" /></CardContent>
      </Card>
    );
  }

  if (isError) {
    return (
      <Card>
        <CardHeader><CardTitle>Performance</CardTitle></CardHeader>
        <CardContent className="flex h-[300px] items-center justify-center">
          <p className="text-sm text-destructive">
            Failed to load performance history.
          </p>
        </CardContent>
      </Card>
    );
  }

  // ── Explain the empty state properly ─────────────────────────────────────
  // success=0, skipped=1 in the snapshot job means "already snapshotted today".
  // New users with 0 snapshots hit the empty branch below. This is expected —
  // tell them when to come back, don't show a broken chart.
  if (chartData.length === 0) {
    return (
      <Card>
        <CardHeader><CardTitle>Performance</CardTitle></CardHeader>
        <CardContent className="flex h-[300px] flex-col items-center justify-center gap-3 text-center">
          <Clock className="h-8 w-8 text-muted-foreground opacity-40" />
          <div className="space-y-1">
            <p className="text-sm font-medium">No history yet</p>
            <p className="max-w-[240px] text-xs text-muted-foreground">
              Net worth snapshots are recorded once per day. Check back tomorrow
              after your first snapshot has been captured.
            </p>
          </div>
          {/* Show the live summary number so the page isn't fully empty */}
          {summary && (
            <p className="text-base font-semibold">
              Current: {fmt(summary.totalValue)}
            </p>
          )}
        </CardContent>
      </Card>
    );
  }

  const strokeColor = metrics?.isPositive !== false ? "#16a34a" : "#dc2626";

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <CardTitle>Performance</CardTitle>
              {/* Always label scope so user knows this is total portfolio */}
              <span className="text-xs text-muted-foreground">All Portfolios</span>
            </div>

            {summary && (
              <div
                className={`flex items-center gap-2 text-sm font-semibold ${
                  safeNum(summary.unrealizedGainLoss) >= 0
                    ? "text-green-600"
                    : "text-red-600"
                }`}
              >
                {safeNum(summary.unrealizedGainLoss) >= 0 ? (
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

            {!summary && metrics && (
              <div
                className={`flex items-center gap-2 text-sm font-semibold ${
                  metrics.isPositive ? "text-green-600" : "text-red-600"
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

            <p className="text-xs text-muted-foreground">Total net worth</p>
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