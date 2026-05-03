"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { TrendingDown, TrendingUp } from "lucide-react";
import { useAccount } from "@/features/portfolio/queries/useAccount";
import type { PositionView } from "@/lib/api/types";

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtMoney(amount?: number, currencyCode?: string): string {
  if (amount == null) return "—";
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: currencyCode ?? "USD",
    minimumFractionDigits: 2,
  }).format(amount);
}

function fmtQty(amount?: number): string {
  if (amount == null) return "—";
  return parseFloat(amount.toFixed(6)).toString();
}

// Normalise the raw assetType string from the backend into a readable label
function assetTypeLabel(type?: string): string {
  if (!type) return "—";
  return type.replace(/_/g, " ");
}

// ── Component ─────────────────────────────────────────────────────────────────

interface StockHoldingsProps {
  portfolioId: string;
  accountId: string;
}

export function StockHoldings({ portfolioId, accountId }: StockHoldingsProps) {
  const { data: account, isLoading, isError } = useAccount(portfolioId, accountId);

  const positions: PositionView[] = account?.assets ?? [];

  // ── Loading ──
  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Holdings</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </CardContent>
      </Card>
    );
  }

  // ── Error ──
  if (isError) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Holdings</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-destructive">
            Failed to load positions. Check your connection and try again.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>Holdings</CardTitle>
        {positions.length > 0 && (
          <span className="text-sm text-muted-foreground">
            {positions.length} position{positions.length !== 1 ? "s" : ""}
          </span>
        )}
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Symbol</TableHead>
              <TableHead>Type</TableHead>
              <TableHead className="text-right">Qty</TableHead>
              <TableHead className="text-right">Avg Cost</TableHead>
              <TableHead className="text-right">Market Value</TableHead>
              <TableHead className="text-right">Unrealized P&L</TableHead>
              <TableHead className="text-right">Return</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {positions.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={7}
                  className="py-10 text-center text-muted-foreground"
                >
                  No positions. Record a Buy transaction to get started.
                </TableCell>
              </TableRow>
            ) : (
              positions.map((pos) => {
                const pnl = pos.unrealizedPnL?.amount;
                const pnlCcy = pos.unrealizedPnL?.currency?.code;
                const isGain = (pnl ?? 0) >= 0;
                const returnPct = pos.returnPercentage?.change;

                const avgCostAmt = pos.averageCostPerUnit?.pricePerUnit?.amount;
                const avgCostCcy =
                  pos.averageCostPerUnit?.pricePerUnit?.currency?.code;

                const marketAmt = pos.marketValue?.amount;
                const marketCcy = pos.marketValue?.currency?.code;

                return (
                  <TableRow key={pos.symbol}>
                    <TableCell className="font-mono font-semibold">
                      {pos.symbol}
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className="text-xs">
                        {assetTypeLabel(pos.assetType)}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-right text-sm tabular-nums">
                      {fmtQty(pos.quantity?.amount)}
                    </TableCell>
                    <TableCell className="text-right text-sm tabular-nums">
                      {fmtMoney(avgCostAmt, avgCostCcy)}
                    </TableCell>
                    <TableCell className="text-right text-sm tabular-nums font-medium">
                      {fmtMoney(marketAmt, marketCcy)}
                    </TableCell>
                    <TableCell className="text-right">
                      <div
                        className={`flex items-center justify-end gap-1 text-sm tabular-nums font-medium ${
                          isGain ? "text-green-600" : "text-red-600"
                        }`}
                      >
                        {isGain ? (
                          <TrendingUp className="h-3.5 w-3.5 shrink-0" />
                        ) : (
                          <TrendingDown className="h-3.5 w-3.5 shrink-0" />
                        )}
                        {fmtMoney(pnl, pnlCcy)}
                      </div>
                    </TableCell>
                    <TableCell
                      className={`text-right text-sm tabular-nums ${
                        isGain ? "text-green-600" : "text-red-600"
                      }`}
                    >
                      {returnPct != null
                        ? `${isGain ? "+" : ""}${returnPct.toFixed(2)}%`
                        : "—"}
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}