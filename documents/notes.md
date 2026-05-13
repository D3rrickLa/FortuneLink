
---
### Use Case Checks
| Use Case | Status Before | Status After |
| :--- | :--- | :--- |
| **Record New Asset Purchase** | ✅ | ✅ |
| **Record Asset Sale** | ✅ | ✅ |
| **Record Deposit/Withdrawal** | ✅ | ✅ |
| **Record Income (Dividend/Interest)** | ✅ | ✅ |
| **View Net Worth** | ⚠️ (liabilities always zero) |✅ | 
| **View Portfolio Performance** | ❌ | Explicitly deferred |
| **Analyze Asset Allocation** | ❌ | Explicitly deferred |
| **Add New Liability** | ❌ | Explicitly deferred |
| **Record Liability Payment** | ❌ | Explicitly deferred |
| **Transaction History with filters** | ✅ (partial — date OR symbol, not both) | ✅ |
| **Realized Gains Report** | ❌ | ✅ |
| **CSV Import** | ❌ | ✅ |

### Completed

| Use Case | Status |
| :--- | :--- |
| **Realized Gains Report** | ✅ |
| **Redis resilience** | ✅ |
| **DRIP safety** | ✅ (warned, not blocked) |
| **Filter docs** | ✅ |
| **Net Worth disclaimer** | ✅ |
| **Loan Management** | Explicitly deferred |
| **CSV Import** | Architecture ready |
| **Performance Calculation** | Explicitly deferred |
| **Asset Allocation** | Explicitly deferred |

# Portfolio Tracker Development Roadmap

## 1. Critical Infrastructure (Application Blockers)
* [✅] **Implement REST Controllers**: Create the HTTP layer for `Portfolio`, `Account`, `Transaction`, and `MarketData` (symbol lookup).
* [✅] **Security Configuration**: Implement `@Configuration` class for Spring Security filter chain and JWT/OAuth2 resource server setup.
* [✅] **Global Exception Handling**: Create `@RestControllerAdvice` to map domain exceptions (`InsufficientFundsException`, etc.) to HTTP status codes.
* [✅] **External Service Implementations**:
    * [✅] `ExchangeRateService`: Implement Bank of Canada (BOC) integration for FX conversions.
    * [✅] `MarketDataProvider`: Implement concrete provider (e.g., FMP) for live price feeds.
* [✅] **Database Migration (V3)**: Add missing Flyway script for `PositionJpaEntity` (last_modified_at) to prevent startup failure.

## 2. Logic & Stability Fixes
* [✅] **Enable Spring Retry**: Add `@EnableRetry` to the main application/config class to activate `@Retryable` annotations.
* [*1] **Historical Import Support**: Add `enforceCashCheck` flag to `RecordPurchaseCommand` to allow buys without pre-existing cash balances during imports.
* [✅] **Business Logic Guards**: Replace `try-catch` database constraints in `PortfolioLifecycleService` with explicit `existsActiveByUserId()` checks.
* [✅] **JPA Performance**: Update `AccountJpaEntity` to implement `Persistable<UUID>` to avoid unnecessary `SELECT` calls before `INSERT`.
* [✅] **Redis Resilience**: Add fallback logic to `PositionRecalculationService` to handle Redis/Redisson outages gracefully.

## 3. Functional Gaps (Missing Features)
* [✅] **Realized Gains Reporting**: Implement `GetRealizedGainsQuery` and corresponding service/endpoint for tax reporting.
* [ ] **Performance Calculation**: Implement `PerformanceCalculationService` (Total Return, TWR, Unrealized Gains).
* [ ] **Asset Allocation**: Implement `AssetAllocationService` for breakdown by asset type, account, and currency.
* [✅] **CSV Import Engine**: Build parsing logic, bulk commands, and file upload endpoints.
* [✅] **Symbol Validation**: Expose a search endpoint to validate symbols before transaction submission.

## 4. Documentation & Cleanup
* [✅] **Stale Code Cleanup**: Remove/update "Bug 6" comments in `TransactionType.java`.
* [✅] **Net Worth Disclaimer**: Add `liabilitiesIncluded` flag to `NetWorthView` to warn users that Loan context is currently deferred.
* [✅] **Filter Improvements**: Update `GetTransactionHistoryQuery` to allow simultaneous filtering by Date AND Symbol.
* [✅] **DRIP Validation**: Implement a check to prevent duplicate recording of dividends and dividend reinvestments.


NOTE:
* 1 - we didn't add a 'flag', we in the `TransactionRecordingServiceImpl.java` used the account HealthStatus to verify ✅

---

### Implementation Progress

| Category | Priority | Status |
| :--- | :--- | :--- |
| **Infrastructure** | High (Blocker) | 🔴 Not Started |
| **Domain Logic** | High | 🟡 Partial |
| **New Features** | Medium | ❌ Missing |
| **Documentation** | Low | 🟡 Needs Cleanup |

---
### No Performance Calculation Service
PerformanceCalculationService appears in your domain diagram with calculateTotalReturn, calculateRealizedGains, calculateUnrealizedGains, calculateTimeWeightedReturn. None of these are implemented. "View Portfolio Performance" is a core domain use case in your documentation. This is not a future concern — it's table stakes for a portfolio tracker.

### No Asset Allocation Service
AssetAllocationService in your diagram with calculateAllocationByType, calculateAllocationByAccount, calculateAllocationByCurrency — none implemented. Your documentation explicitly says "Analyze Asset Allocation" as a use case.
No CSV Import Service
TransactionMetadata.csvImport() factory method exists, but there's no:

### CSV parsing service, Architecture Only
Bulk transaction command
File upload endpoint

Your documentation explicitly called this out as important for UX.

This is its own sprint. What you need when you get there:
```
application/
  commands/
    ImportTransactionsCommand.java     // accountId, userId, portfolioId, List<CsvRow>
  services/
    CsvImportService.java              // orchestrates parse + validate + execute
  
infrastructure/
  csv/
    CsvTransactionParser.java          // String -> List<CsvTransactionRow>
    CsvTransactionRow.java             // raw DTO matching CSV columns
    CsvImportResult.java               // per-row success/failure report
```


### No Symbol Search/Validation Endpoint
Before a user records a BUY, they need to search for AAPL or BTC-USD. MarketDataService.isSymbolSupported() and getAssetInfo() exist, but there's no exposed endpoint. The frontend has no way to validate a symbol or retrieve its metadata before submitting a transaction.

#### code example
```
@GetMapping("/{symbol}/validate")
    public ResponseEntity<AssetInfoResponse> validateSymbol(@PathVariable String symbol) {
        if (!marketDataService.isSymbolSupported(symbol)) {
            return ResponseEntity.notFound().build();
        }
        
        var info = marketDataService.getAssetInfo(symbol);
        return ResponseEntity.ok(new AssetInfoResponse(info));
    }
```

How to implement the other stuff
```
class PerformanceCalculationService {
    +calculateTotalReturn(Portfolio, MarketDataService) Percentage
    +calculateRealizedGains(List~Transaction~) Money
    +calculateUnrealizedGains(Portfolio, MarketDataService) Money
    +calculateTimeWeightedReturn(Portfolio) Percentage
}

class AssetAllocationService {
    +calculateAllocationByType(Portfolio) Map~AssetType, Percentage~
    +calculateAllocationByAccount(Portfolio) Map~AccountType, Percentage~
}
```


"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import {
  Briefcase,
  Plus,
  TrendingUp,
  TrendingDown,
  LayoutGrid,
  ChevronDown,
  ChevronRight,
  Building2,
  Loader2,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { RadioGroup, RadioGroupItem } from "./ui/radio-group";
import { CreateAccountRequest, CreatePortfolioRequest } from "@/lib/api/types";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "./ui/select";
import {
  useAccounts,
  useCreateAccount,
} from "@/features/portfolio/queries/useAccount";
import { toast } from "sonner";

// ─── UI-layer types ───────────────────────────────────────────────────────────

export interface Account {
  id: string;
  name: string;
  type: string;
  // Change these to match the API objects
  totalValue?: { amount: number; currency: string };
  cashBalance?: { amount: number; currency: string };
  baseCurrency?: { code: string };
  accountId?: { id: string };
  gainLoss: number;
  gainLossPercent: number;
}

export interface Portfolio {
  id: string;
  name: string;
  currency: string;
  totalValue: number;
  gainLoss: number;
  gainLossPercent: number;
  accounts: Account[];
}

// ─── Props ────────────────────────────────────────────────────────────────────

interface PortfolioSidebarProps {
  portfolios: Portfolio[];
  activePortfolioId: string;
  activeAccountId?: string | null;
  onSelectPortfolio: (id: string) => void;
  onSelectAccount: (portfolioId: string, accountId: string) => void;
  onCreatePortfolio: (data: CreatePortfolioRequest) => void;
  // Intentionally kept for extensibility (e.g., parent-level analytics),
  // but account creation mutations are owned by the sidebar via useCreateAccount.
  onCreateAccount: (portfolioId: string, data: CreateAccountRequest) => void;
}

// ─── PortfolioItem ────────────────────────────────────────────────────────────

interface PortfolioItemProps {
  portfolio: Portfolio;
  isActive: boolean;
  activeAccountId?: string | null;
  onSelectPortfolio: (id: string) => void;
  onSelectAccount: (portfolioId: string, accountId: string) => void;
  onAddAccount: (portfolioId: string) => void;
}

function PortfolioItem({
  portfolio,
  isActive,
  activeAccountId,
  onSelectPortfolio,
  onSelectAccount,
  onAddAccount,
}: PortfolioItemProps) {
  const [expanded, setExpanded] = useState(false);
  const { data: accountsPage, isLoading: accountsLoading } = useAccounts(
    portfolio.id,
    0,
    50,
    { enabled: expanded }
  );

  const accounts = accountsPage?.content ?? [];

  const totalValue = accounts.reduce((sum, acc) => {
    return sum + (acc.totalValue?.amount ?? 0);
  }, 0);

  const totalGainLoss = portfolio.accounts?.reduce((sum, acc) => {
    // Use the exact field name from your API (e.g., gainLoss or unrealizedGainLoss)
    return sum + (acc.gainLoss ?? 0);
  }, 0) ?? 0;

  // Re-calculate percentages based on the new sums
  const costBasis = totalValue - totalGainLoss;
  const gainLossPercent = costBasis > 0 ? (totalGainLoss / costBasis) * 100 : 0;
  const isPositive = totalGainLoss >= 0;

  const fmtCurrency = (amount: number) =>
    new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: portfolio.currency || "CAD",
    }).format(amount);

  return (
    <div className="space-y-1">
      <div
        className={cn(
          "group flex items-center gap-2 rounded-lg p-2 transition-colors hover:bg-muted",
          isActive && !activeAccountId && "bg-muted"
        )}
      >
        <div className="flex items-center justify-between gap-2">
          <button
            onClick={() => setExpanded((prev) => !prev)}
            className="flex items-center gap-1 hover:opacity-70 transition-opacity"
            aria-label={expanded ? "Collapse accounts" : "Expand accounts"}
          >
            {expanded ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          </button>

          <button
            onClick={() => {
              onSelectPortfolio(portfolio.id);
              setExpanded(true);
            }}
            className="flex-1 text-left"
          >
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <span className="font-medium">{portfolio.name}</span>
                {isActive && !activeAccountId && (
                  <div className="h-2 w-2 rounded-full bg-blue-600" />
                )}
              </div>
              {/* Total Value Display */}
              <div className="text-sm text-muted-foreground">
                {fmtCurrency(totalValue)}
              </div>

              {/* Performance Metrics */}
              <div className={cn(
                "flex items-center gap-1 text-xs",
                isPositive ? "text-green-600" : "text-red-600"
              )}>
                {isPositive ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
                <span>
                  {isPositive ? "+" : ""}{fmtCurrency(totalGainLoss)}
                </span>
                <span className="opacity-70">
                  ({isPositive ? "+" : ""}{gainLossPercent.toFixed(2)}%)
                </span>
              </div>
            </div>
          </button>

          {/* Quick Add Account Button */}
          <Button
            size="sm"
            variant="ghost"
            className="h-6 w-6 p-0 shrink-0"
            onClick={() => onAddAccount(portfolio.id)}
            aria-label="Add account"
          >
            <Plus className="h-3 w-3" />
          </Button>
        </div>
      </div>

      {/* Account List (Children) */}
      {expanded && (
        <div className="ml-4 space-y-1 border-l-2 border-muted pl-2">
          {accountsLoading ? (
            <div className="flex items-center gap-2 p-2 text-xs text-muted-foreground">
              <Loader2 className="h-3 w-3 animate-spin" />
              Loading accounts...
            </div>
          ) : accounts.length === 0 ? (
            <div className="p-2 text-xs italic text-muted-foreground">
              No accounts — add one with the + button
            </div>
          ) : (
            accounts.map((account) => {
              const accountId = account.accountId?.id ?? "";
              const isAccountActive = accountId === activeAccountId;
              const totalValueAmount = account.totalValue?.amount ?? 0;
              const cashBalanceAmount = account.cashBalance?.amount ?? 0;


              return (
                <button
                  key={accountId}
                  onClick={() => onSelectAccount(portfolio.id, accountId)}
                  className={cn(
                    "w-full rounded-lg p-2 text-left transition-colors hover:bg-muted",
                    isAccountActive && "bg-muted border-l-2 border-blue-600"
                  )}
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <Building2 className="h-3 w-3 shrink-0 text-muted-foreground" />
                      <span className="text-sm font-medium truncate">
                        {account.name ?? "Unnamed Account"}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-xs text-muted-foreground">
                      <span>
                        {account.type ?? "—"} · {account.baseCurrency?.code ?? "—"}
                      </span>
                      <span className="font-medium text-foreground">
                        {/* Use the extracted amount here */}
                        {fmtCurrency(totalValueAmount)}
                      </span>
                    </div>
                    {/* Logic to show Cash if it exists */}
                    {cashBalanceAmount > 0 && (
                      <div className="text-xs text-muted-foreground">
                        Cash: {fmtCurrency(cashBalanceAmount)}
                      </div>
                    )}
                  </div>
                </button>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}

// ─── PortfolioSidebar ─────────────────────────────────────────────────────────

export function PortfolioSidebar({
  portfolios,
  activePortfolioId,
  activeAccountId,
  onSelectPortfolio,
  onSelectAccount,
  onCreatePortfolio,
  onCreateAccount,
}: PortfolioSidebarProps) {
  const [portfolioDialogOpen, setPortfolioDialogOpen] = useState(false);
  const [accountDialogOpen, setAccountDialogOpen] = useState(false);
  const [selectedPortfolioForAccount, setSelectedPortfolioForAccount] =
    useState<string>("");

  const [portfolioFormData, setPortfolioFormData] =
    useState<CreatePortfolioRequest>({
      name: "",
      description: "",
      currency: "CAD",
      createDefaultAccount: true,
      defaultAccountType: "TAXABLE_INVESTMENT",
      defaultStrategy: "ACB",
    });

  const [accountFormData, setAccountFormData] = useState<CreateAccountRequest>({
    accountName: "",
    accountType: "TAXABLE_INVESTMENT",
    strategy: "ACB",
    currency: "CAD",
  });

  // ── Account creation mutation ────────────────────────────────────────────
  // Keyed on selectedPortfolioForAccount. This is the fix for accounts never
  // being created — the dashboard's onCreateAccount prop was a no-op. The
  // sidebar owns this mutation because it has all the context it needs.
  const createAccountMutation = useCreateAccount(selectedPortfolioForAccount);

  // ── Handlers ─────────────────────────────────────────────────────────────

  const handleCreatePortfolio = (e: React.SubmitEvent) => {
    e.preventDefault();
    if (portfolioFormData.name.trim()) {
      onCreatePortfolio(portfolioFormData);
      setPortfolioDialogOpen(false);
      setPortfolioFormData((prev) => ({ ...prev, name: "", description: "" }));
    }
  };

  const handleCreateAccount = (e: React.SubmitEvent) => {
    e.preventDefault();
    if (!accountFormData.accountName.trim() || !selectedPortfolioForAccount) return;

    createAccountMutation.mutate(accountFormData, {
      onSuccess: () => {
        toast.success("Account created");
        setAccountDialogOpen(false);
        setAccountFormData((prev) => ({ ...prev, accountName: "" }));
        // Notify parent in case it wants to react (e.g., select the new account)
        onCreateAccount(selectedPortfolioForAccount, accountFormData);
      },
      onError: (err: unknown) => {
        const msg =
          err instanceof Error ? err.message : "Failed to create account";
        toast.error(msg);
      },
    });
  };

  const openAccountDialog = (portfolioId: string) => {
    setSelectedPortfolioForAccount(portfolioId);
    setAccountDialogOpen(true);
  };

  // ── Aggregate sidebar stats ───────────────────────────────────────────────

  const combinedValue = portfolios.reduce((sum, p) => sum + p.totalValue, 0);
  const combinedGainLoss = portfolios.reduce((sum, p) => sum + p.gainLoss, 0);

  const totalCostBasis = combinedValue - combinedGainLoss;
  const combinedGainLossPercent = totalCostBasis > 0
    ? (combinedGainLoss / totalCostBasis) * 100
    : 0;

  const isCombinedPositive = combinedGainLoss >= 0;


  return (
    <div className="flex h-full flex-col border-r bg-muted/10">
      {/* Header */}
      <div className="border-b p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Briefcase className="h-5 w-5" />
            <h2 className="font-semibold">Portfolios</h2>
          </div>

          <Dialog
            open={portfolioDialogOpen}
            onOpenChange={setPortfolioDialogOpen}
          >
            <DialogTrigger asChild>
              <Button size="sm" variant="ghost">
                <Plus className="h-4 w-4" />
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-[425px]">
              <DialogHeader>
                <DialogTitle>Create New Portfolio</DialogTitle>
                <DialogDescription>
                  Set up a new investment portfolio with a default account.
                </DialogDescription>
              </DialogHeader>
              <form onSubmit={handleCreatePortfolio} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="port-name">Portfolio Name</Label>
                  <Input
                    id="port-name"
                    value={portfolioFormData.name}
                    onChange={(e) =>
                      setPortfolioFormData((prev) => ({
                        ...prev,
                        name: e.target.value,
                      }))
                    }
                    placeholder="e.g., Retirement"
                    required
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="port-currency">Base Currency</Label>
                  <Select
                    value={portfolioFormData.currency}
                    onValueChange={(val) =>
                      setPortfolioFormData((prev) => ({
                        ...prev,
                        currency: val,
                      }))
                    }
                  >
                    <SelectTrigger id="port-currency">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="CAD">CAD</SelectItem>
                      <SelectItem value="USD">USD</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="account-type">Default Account Type</Label>
                  <Select
                    value={portfolioFormData.defaultAccountType}
                    onValueChange={(val) =>
                      setPortfolioFormData((prev) => ({
                        ...prev,
                        defaultAccountType:
                          val as CreatePortfolioRequest["defaultAccountType"],
                      }))
                    }
                  >
                    <SelectTrigger id="account-type">
                      <SelectValue placeholder="Select type" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="TAXABLE_INVESTMENT">
                        Taxable Investment
                      </SelectItem>
                      <SelectItem value="NON_REGISTERED_INVESTMENT">
                        Non-Registered
                      </SelectItem>
                      <SelectItem value="TFSA">TFSA</SelectItem>
                      <SelectItem value="RRSP">RRSP</SelectItem>
                      <SelectItem value="FHSA">FHSA</SelectItem>
                      <SelectItem value="RESP">RESP</SelectItem>
                      <SelectItem value="MARGIN">Margin</SelectItem>
                      <SelectItem value="ROTH_IRA">Roth IRA</SelectItem>
                      <SelectItem value="CHEQUING">Chequing</SelectItem>
                      <SelectItem value="SAVINGS">Savings</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>Cost Basis Strategy</Label>
                  <RadioGroup
                    value={portfolioFormData.defaultStrategy}
                    onValueChange={(val) =>
                      setPortfolioFormData((prev) => ({
                        ...prev,
                        defaultStrategy:
                          val as CreatePortfolioRequest["defaultStrategy"],
                      }))
                    }
                    className="flex flex-col gap-2"
                  >
                    <div className="flex items-center gap-3">
                      <RadioGroupItem value="ACB" id="acb" />
                      <Label htmlFor="acb" className="font-normal">
                        Adjusted Cost Basis (ACB) — recommended for Canadians
                      </Label>
                    </div>
                    <div className="flex items-center gap-3">
                      <RadioGroupItem value="FIFO" id="fifo" />
                      <Label htmlFor="fifo" className="font-normal">
                        First-In, First-Out (FIFO)
                      </Label>
                    </div>
                  </RadioGroup>
                </div>

                <Button type="submit" className="w-full">
                  Create Portfolio
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      {/* List */}
      <div className="flex-1 overflow-auto p-2">
        <div className="space-y-1">
          {/* All Portfolios aggregate row */}
          <button
            onClick={() => onSelectPortfolio("all")}
            className={cn(
              "w-full rounded-lg border-2 border-dashed p-3 text-left transition-colors hover:bg-muted",
              activePortfolioId === "all" && "border-blue-600 bg-muted"
            )}
          >
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <LayoutGrid className="h-4 w-4" />
                  <span className="font-medium">All Portfolios</span>
                </div>
                {activePortfolioId === "all" && (
                  <div className="h-2 w-2 rounded-full bg-blue-600" />
                )}
              </div>
              <div className="text-sm text-muted-foreground">
                $
                {combinedValue.toLocaleString("en-US", {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </div>
              <div
                className={cn(
                  "flex items-center gap-1 text-xs",
                  isCombinedPositive ? "text-green-600" : "text-red-600"
                )}
              >
                {isCombinedPositive ? (
                  <TrendingUp className="h-3 w-3" />
                ) : (
                  <TrendingDown className="h-3 w-3" />
                )}
                <span>
                  {isCombinedPositive ? "+" : ""}$
                  {Math.abs(combinedGainLoss).toLocaleString("en-US", {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}
                </span>
                <span className="text-muted-foreground">
                  ({isCombinedPositive ? "+" : ""}
                  {combinedGainLossPercent.toFixed(2)}%)
                </span>
              </div>
            </div>
          </button>

          <div className="my-2 border-t" />

          {portfolios.map((portfolio) => (
            <PortfolioItem
              key={portfolio.id}
              portfolio={portfolio}
              isActive={portfolio.id === activePortfolioId}
              activeAccountId={activeAccountId}
              onSelectPortfolio={onSelectPortfolio}
              onSelectAccount={onSelectAccount}
              onAddAccount={openAccountDialog}
            />
          ))}
        </div>
      </div>

      {/* Footer summary */}
      <div className="border-t p-4">
        <div className="space-y-1 text-sm">
          <div className="flex justify-between text-muted-foreground">
            <span>Portfolios:</span>
            <span className="font-medium text-foreground">
              {portfolios.length}
            </span>
          </div>
          <div className="flex justify-between text-muted-foreground">
            <span>Combined Value:</span>
            <span className="font-medium text-foreground">
              $
              {combinedValue.toLocaleString("en-US", {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2,
              })}
            </span>
          </div>
        </div>
      </div>

      {/* Add Account dialog — owned by sidebar, opened via openAccountDialog() */}
      <Dialog open={accountDialogOpen} onOpenChange={setAccountDialogOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>Add Account</DialogTitle>
            <DialogDescription>
              Add a new account to the selected portfolio.
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleCreateAccount} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="acct-name">Account Name</Label>
              <Input
                id="acct-name"
                placeholder="e.g., TFSA Growth"
                value={accountFormData.accountName}
                onChange={(e) =>
                  setAccountFormData((prev) => ({
                    ...prev,
                    accountName: e.target.value,
                  }))
                }
                required
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="acct-type">Account Type</Label>
              <Select
                value={accountFormData.accountType}
                onValueChange={(val: CreateAccountRequest["accountType"]) =>
                  setAccountFormData((prev) => ({ ...prev, accountType: val }))
                }
              >
                <SelectTrigger id="acct-type">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="TAXABLE_INVESTMENT">
                    Taxable Investment
                  </SelectItem>
                  <SelectItem value="NON_REGISTERED_INVESTMENT">
                    Non-Registered
                  </SelectItem>
                  <SelectItem value="TFSA">TFSA</SelectItem>
                  <SelectItem value="RRSP">RRSP</SelectItem>
                  <SelectItem value="FHSA">FHSA</SelectItem>
                  <SelectItem value="RESP">RESP</SelectItem>
                  <SelectItem value="MARGIN">Margin</SelectItem>
                  <SelectItem value="ROTH_IRA">Roth IRA</SelectItem>
                  <SelectItem value="CHEQUING">Chequing</SelectItem>
                  <SelectItem value="SAVINGS">Savings</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="acct-currency">Currency</Label>
              <Select
                value={accountFormData.currency}
                onValueChange={(val) =>
                  setAccountFormData((prev) => ({ ...prev, currency: val }))
                }
              >
                <SelectTrigger id="acct-currency">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="CAD">CAD</SelectItem>
                  <SelectItem value="USD">USD</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Cost Basis Strategy</Label>
              <RadioGroup
                value={accountFormData.strategy}
                onValueChange={(val: CreateAccountRequest["strategy"]) =>
                  setAccountFormData((prev) => ({ ...prev, strategy: val }))
                }
                className="flex flex-col gap-2"
              >
                <div className="flex items-center gap-3">
                  <RadioGroupItem value="ACB" id="acct-acb" />
                  <Label htmlFor="acct-acb" className="font-normal">
                    ACB
                  </Label>
                </div>
                <div className="flex items-center gap-3">
                  <RadioGroupItem value="FIFO" id="acct-fifo" />
                  <Label htmlFor="acct-fifo" className="font-normal">
                    FIFO
                  </Label>
                </div>
              </RadioGroup>
            </div>

            <Button
              type="submit"
              className="w-full"
              disabled={createAccountMutation.isPending}
            >
              {createAccountMutation.isPending ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Creating...
                </>
              ) : (
                "Add Account"
              )}
            </Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}

// ... inside PortfolioItem ... const { data: accountsPage, isLoading: accountsLoading } = useAccounts( portfolio.id, 0, 50, { enabled: expanded } ); // This is the fresh data const accounts = accountsPage?.content ?? []; // 1. SUM FROM THE FRESH 'accounts' ARRAY const totalValue = accounts.reduce((sum, acc) => { return sum + (acc.totalValue?.amount ?? 0); }, 0); const totalGainLoss = accounts.reduce((sum, acc) => { // Use the gainLoss field from the account object return sum + (acc.gainLoss ?? 0); }, 0); // 2. Re-calculate logic remains the same const costBasis = totalValue - totalGainLoss; const gainLossPercent = costBasis > 0 ? (totalGainLoss / costBasis) * 100 : 0; const isPositive = totalGainLoss >= 0; // ... rest of your component



was trying to follow this implementation, the totalGainLoss seems a bit broken, along with most of the things