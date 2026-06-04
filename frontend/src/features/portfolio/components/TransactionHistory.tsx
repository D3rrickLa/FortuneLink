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
import { useTransactionHistory } from "../queries/useTransactions";
import type { TransactionType, TransactionView } from "@/lib/api/types";

// ── Display config ────────────────────────────────────────────────────────────

type BadgeVariant = "default" | "secondary" | "outline" | "destructive";

const TYPE_CONFIG: Record<
  TransactionType,
  { label: string; variant: BadgeVariant }
> = {
  BUY: { label: "Buy", variant: "default" },
  SELL: { label: "Sell", variant: "secondary" },
  DEPOSIT: { label: "Deposit", variant: "outline" },
  WITHDRAWAL: { label: "Withdrawal", variant: "destructive" },
  DIVIDEND: { label: "Dividend", variant: "outline" },
  DIVIDEND_REINVEST: { label: "DRIP", variant: "outline" },
  INTEREST: { label: "Interest", variant: "outline" },
  FEE: { label: "Fee", variant: "destructive" },
  TRANSFER_IN: { label: "Transfer In", variant: "outline" },
  TRANSFER_OUT: { label: "Transfer Out", variant: "outline" },
  SPLIT: { label: "Split", variant: "secondary" },
  RETURN_OF_CAPITAL: { label: "ROC", variant: "secondary" },
  OTHER: { label: "Other", variant: "outline" },
};

// ── Helpers ───────────────────────────────────────────────────────────────────

function fmtDate(iso?: string): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("en-CA", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

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
  // Strip trailing zeros for whole shares but keep up to 6 decimal places
  return parseFloat(amount.toFixed(6)).toString();
}

// ── Component ─────────────────────────────────────────────────────────────────

interface TransactionHistoryProps {
  portfolioId: string;
  accountId: string;
}

export function TransactionHistory({
  portfolioId,
  accountId,
}: TransactionHistoryProps) {
  const { data, isLoading, isError } = useTransactionHistory(
    portfolioId,
    accountId
  );

  const transactions: TransactionView[] = data?.content ?? [];

  // ── Loading state ──
  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Transaction History</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {Array.from({ length: 5 }).map((_, i) => (
            <Skeleton key={i} className="h-10 w-full" />
          ))}
        </CardContent>
      </Card>
    );
  }

  // ── Error state ──
  if (isError) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Transaction History</CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-destructive">
            Failed to load transactions. Check your connection and try again.
          </p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between">
        <CardTitle>Transaction History</CardTitle>
        {data?.totalElements != null && data.totalElements > 0 && (
          <span className="text-sm text-muted-foreground">
            {data.totalElements} record{data.totalElements !== 1 ? "s" : ""}
          </span>
        )}
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Date</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Symbol</TableHead>
              <TableHead className="text-right">Qty</TableHead>
              <TableHead className="text-right">Price</TableHead>
              <TableHead className="text-right">Total</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {transactions.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={6}
                  className="py-10 text-center text-muted-foreground"
                >
                  No transactions yet - add your first one above.
                </TableCell>
              </TableRow>
            ) : (
              transactions.map((tx) => {
                const cfg =
                  TYPE_CONFIG[tx.type!] ??
                  ({ label: tx.type, variant: "outline" } as {
                    label: string;
                    variant: BadgeVariant;
                  });

                const priceAmt = tx.price?.pricePerUnit?.amount;
                const priceCcy = tx.price?.pricePerUnit?.currency?.code;
                const totalAmt = tx.totalCost?.amount;
                const totalCcy = tx.totalCost?.currency?.code;

                return (
                  <TableRow key={tx.transactionId?.id}>
                    <TableCell className="text-sm tabular-nums">
                      {fmtDate(tx.date)}
                    </TableCell>
                    <TableCell>
                      <Badge variant={cfg.variant}>{cfg.label}</Badge>
                    </TableCell>
                    <TableCell className="font-mono text-sm">
                      {tx.symbol ?? "—"}
                    </TableCell>
                    <TableCell className="text-right text-sm tabular-nums">
                      {fmtQty(tx.quantity?.amount)}
                    </TableCell>
                    <TableCell className="text-right text-sm tabular-nums">
                      {fmtMoney(priceAmt, priceCcy)}
                    </TableCell>
                    <TableCell className="text-right text-sm font-medium tabular-nums">
                      {fmtMoney(totalAmt, totalCcy)}
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