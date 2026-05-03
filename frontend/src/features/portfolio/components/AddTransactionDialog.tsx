"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2, Plus, Search } from "lucide-react";
import { toast } from "sonner";
import {
  useRecordBuy,
  useRecordDeposit,
  useRecordSell,
  useRecordWithdrawal,
} from "../queries/useTransactions";
import { useSymbolSearch } from "../hooks/useSymbolSearch";
import { validateSymbol } from "../services/market.services";
import type { AssetInfoResponse, AssetType } from "@/lib/api/types";

type TxType = "BUY" | "SELL" | "DEPOSIT" | "WITHDRAWAL";

interface Props {
  portfolioId: string;
  accountId: string;
}

const CURRENCIES = ["USD", "CAD", "EUR", "GBP"];

export function AddTransactionDialog({ portfolioId, accountId }: Props) {
  const [open, setOpen] = useState(false);
  const [type, setType] = useState<TxType>("BUY");

  // Symbol state
  const [symbolInput, setSymbolInput] = useState("");
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [validatedAsset, setValidatedAsset] = useState<AssetInfoResponse | null>(null);
  const [validating, setValidating] = useState(false);

  // Fields
  const [quantity, setQuantity] = useState("");
  const [price, setPrice] = useState("");
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState("USD");
  const [notes, setNotes] = useState("");

  const { data: suggestions } = useSymbolSearch(symbolInput);

  const recordBuy = useRecordBuy(portfolioId, accountId);
  const recordSell = useRecordSell(portfolioId, accountId);
  const recordDeposit = useRecordDeposit(portfolioId, accountId);
  const recordWithdrawal = useRecordWithdrawal(portfolioId, accountId);

  const isPending =
    recordBuy.isPending ||
    recordSell.isPending ||
    recordDeposit.isPending ||
    recordWithdrawal.isPending;

  const isAssetTx = type === "BUY" || type === "SELL";

  const estimatedTotal =
    isAssetTx && quantity && price
      ? parseFloat(quantity) * parseFloat(price)
      : null;

  // --- Symbol selection + validation ---

  const handleSelectSuggestion = async (symbol: string) => {
    setSymbolInput(symbol);
    setShowSuggestions(false);
    setValidatedAsset(null);

    if (type !== "BUY") {
      // SELL doesn't need validation — the asset must already exist in the account
      setValidatedAsset({ symbol } as AssetInfoResponse);
      return;
    }

    setValidating(true);
    try {
      const info = await validateSymbol(symbol);
      setValidatedAsset(info);
      toast.success(`${symbol} validated`);
    } catch {
      toast.error(`Symbol "${symbol}" not found or unsupported.`);
    } finally {
      setValidating(false);
    }
  };

  const handleManualValidate = () => {
    if (symbolInput.trim()) handleSelectSuggestion(symbolInput.trim().toUpperCase());
  };

  // --- Submit ---

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      if (type === "BUY") {
        if (!validatedAsset?.symbol) {
          toast.error("Validate the symbol before recording a buy.");
          return;
        }
        await recordBuy.mutateAsync({
          symbol: validatedAsset.symbol,
          type: (validatedAsset.assetType as AssetType) ?? "STOCK",
          quantity: parseFloat(quantity),
          price: parseFloat(price),
          currency,
          notes: notes.trim() || undefined,
        });
      } else if (type === "SELL") {
        const sym = validatedAsset?.symbol ?? symbolInput.trim().toUpperCase();
        if (!sym) {
          toast.error("Enter the symbol you want to sell.");
          return;
        }
        await recordSell.mutateAsync({
          symbol: sym,
          quantity: parseFloat(quantity),
          price: parseFloat(price),
          currency,
          notes: notes.trim() || undefined,
        });
      } else if (type === "DEPOSIT") {
        await recordDeposit.mutateAsync({
          amount: parseFloat(amount),
          currency,
          notes: notes.trim() || undefined,
        });
      } else {
        await recordWithdrawal.mutateAsync({
          amount: parseFloat(amount),
          currency,
          notes: notes.trim() || undefined,
        });
      }

      toast.success("Transaction recorded.");
      handleClose();
    } catch (err: unknown) {
      // Backend returns { code, message, errors, timestamp } — never an object
      // directly to toast or React will try to render it as a child and crash.
      const data = (err as { response?: { data?: unknown } })?.response?.data;
      let msg = "Failed to record transaction.";

      if (typeof data === "string") {
        msg = data;
      } else if (data && typeof data === "object") {
        const d = data as Record<string, unknown>;
        // Spring error envelope: { message: "...", errors: [...] }
        if (typeof d.message === "string") {
          msg = d.message;
        }
        // Append field-level validation errors if present
        if (Array.isArray(d.errors) && d.errors.length > 0) {
          const fieldErrors = (d.errors as Array<{ field?: string; defaultMessage?: string }>)
            .map((e) => (e.field ? `${e.field}: ${e.defaultMessage}` : e.defaultMessage))
            .filter(Boolean)
            .join("; ");
          if (fieldErrors) msg = `${msg} — ${fieldErrors}`;
        }
      }

      toast.error(msg);
    }
  };

  const handleClose = () => {
    setOpen(false);
    // Reset — don't reset type so the user can quickly add another of the same kind
    setSymbolInput("");
    setValidatedAsset(null);
    setQuantity("");
    setPrice("");
    setAmount("");
    setNotes("");
  };

  const handleTypeChange = (v: string) => {
    setType(v as TxType);
    setSymbolInput("");
    setValidatedAsset(null);
  };

  return (
    <Dialog
      open={open}
      onOpenChange={(v) => {
        if (!v) handleClose();
        else setOpen(true);
      }}
    >
      <DialogTrigger asChild>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          Add Transaction
        </Button>
      </DialogTrigger>

      <DialogContent className="sm:max-w-[460px]">
        <DialogHeader>
          <DialogTitle>Record Transaction</DialogTitle>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4 pt-2">
          {/* ── Type ── */}
          <div className="space-y-1.5">
            <Label>Type</Label>
            <Select value={type} onValueChange={handleTypeChange}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="BUY">Buy Asset</SelectItem>
                <SelectItem value="SELL">Sell Asset</SelectItem>
                <SelectItem value="DEPOSIT">Deposit Cash</SelectItem>
                <SelectItem value="WITHDRAWAL">Withdraw Cash</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* ── Symbol search (asset transactions only) ── */}
          {isAssetTx && (
            <div className="space-y-1.5">
              <Label>
                Symbol
                {type === "BUY" && (
                  <span className="ml-1.5 text-xs text-muted-foreground">
                    (validate before buying)
                  </span>
                )}
              </Label>
              <div className="relative flex gap-2">
                <div className="relative flex-1">
                  <Search className="absolute left-2.5 top-2 h-4 w-4 text-muted-foreground pointer-events-none" />
                  <Input
                    className="pl-8"
                    placeholder="AAPL, BTC-USD…"
                    value={symbolInput}
                    autoComplete="off"
                    onChange={(e) => {
                      setSymbolInput(e.target.value);
                      setValidatedAsset(null);
                      setShowSuggestions(true);
                    }}
                    onFocus={() => setShowSuggestions(true)}
                    onBlur={() =>
                      setTimeout(() => setShowSuggestions(false), 150)
                    }
                  />
                </div>

                {type === "BUY" && !validatedAsset && symbolInput && (
                  <Button
                    type="button"
                    variant="outline"
                    disabled={validating}
                    onClick={handleManualValidate}
                    className="shrink-0"
                  >
                    {validating ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      "Validate"
                    )}
                  </Button>
                )}
              </div>

              {/* Autocomplete dropdown */}
              {showSuggestions && suggestions && suggestions.length > 0 && (
                <div className="rounded-md border bg-popover shadow-md overflow-hidden">
                  {suggestions.slice(0, 6).map((s) => (
                    <button
                      key={s.symbol}
                      type="button"
                      onMouseDown={() => handleSelectSuggestion(s.symbol!)}
                      className="flex w-full items-center justify-between px-3 py-2 text-sm hover:bg-accent transition-colors"
                    >
                      <span className="font-mono font-medium">{s.symbol}</span>
                      <span className="text-muted-foreground text-xs truncate ml-3 max-w-[200px]">
                        {s.name} · {s.exchange}
                      </span>
                    </button>
                  ))}
                </div>
              )}

              {/* Validation status pill */}
              {type === "BUY" && validatedAsset && (
                <p className="text-xs text-green-600 flex items-center gap-1">
                  <span>✓</span>
                  <span>
                    {validatedAsset.symbol} · {validatedAsset.assetType} ·{" "}
                    {validatedAsset.exchange} validated
                  </span>
                </p>
              )}
            </div>
          )}

          {/* ── Quantity + Price (asset tx) ── */}
          {isAssetTx && (
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1.5">
                <Label htmlFor="qty">Quantity</Label>
                <Input
                  id="qty"
                  type="number"
                  step="any"
                  min="0"
                  placeholder="10"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                  required
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="price">Price / Unit</Label>
                <Input
                  id="price"
                  type="number"
                  step="any"
                  min="0"
                  placeholder="175.50"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  required
                />
              </div>
            </div>
          )}

          {/* ── Amount (cash tx) ── */}
          {!isAssetTx && (
            <div className="space-y-1.5">
              <Label htmlFor="amount">Amount</Label>
              <Input
                id="amount"
                type="number"
                step="any"
                min="0"
                placeholder="1000.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                required
              />
            </div>
          )}

          {/* ── Currency ── */}
          <div className="space-y-1.5">
            <Label>Currency</Label>
            <Select value={currency} onValueChange={setCurrency}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {CURRENCIES.map((c) => (
                  <SelectItem key={c} value={c}>
                    {c}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* ── Estimated total ── */}
          {estimatedTotal !== null && !isNaN(estimatedTotal) && (
            <div className="rounded-md bg-muted px-3 py-2 text-sm flex justify-between">
              <span className="text-muted-foreground">Estimated total</span>
              <span className="font-medium tabular-nums">
                {currency}{" "}
                {estimatedTotal.toLocaleString("en-US", {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </span>
            </div>
          )}

          {/* ── Notes ── */}
          <div className="space-y-1.5">
            <Label htmlFor="notes">
              Notes{" "}
              <span className="text-muted-foreground text-xs">(optional)</span>
            </Label>
            <Input
              id="notes"
              placeholder="Quarterly rebalance…"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>

          {/* ── Actions ── */}
          <div className="flex gap-2 pt-1">
            <Button
              type="button"
              variant="outline"
              className="flex-1"
              onClick={handleClose}
              disabled={isPending}
            >
              Cancel
            </Button>
            <Button type="submit" className="flex-1" disabled={isPending}>
              {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Record
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}