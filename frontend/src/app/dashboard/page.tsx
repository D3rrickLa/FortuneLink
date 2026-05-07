"use client";

import { useMemo, useState } from "react";
import { Navbar } from "@/components/Navbar";
import { PortfolioSidebar } from "@/components/PortfolioSidebar";
import { usePortfolios } from "@/features/portfolio/hooks/usePortfolios";
import { useLogout } from "@/features/portfolio/hooks/useLogout";
import { Toaster } from "sonner";
import { SettingsDialog } from "@/components/SettingsDialog";
import { AddTransactionDialog } from "@/features/portfolio/components/AddTransactionDialog";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { PortfolioOverview } from "@/features/portfolio/components/PortfolioOverview";
import { PerformanceChart } from "@/features/portfolio/components/PerformanceChart";
import { AllocationChart } from "@/features/portfolio/components/AllocationChart";
import { TransactionHistory } from "@/features/portfolio/components/TransactionHistory";
import { StockHoldings } from "@/features/portfolio/components/StockHoldings";
import { useNetWorth, usePortfolio } from "@/features/portfolio/queries/usePortfolio";
import { useAccount } from "@/features/portfolio/queries/useAccount";
import { AccountView, CreateAccountRequest, CreatePortfolioRequest } from "@/lib/api/types";
import { EditAccountDialog } from "@/features/portfolio/components/EditAccountDialog";

export default function DashboardPage() {
  const [activePortfolioId, setActivePortfolioId] = useState("all");
  const [activeAccountId, setActiveAccountId] = useState<string | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const { portfolios, isLoading, createPortfolio } = usePortfolios();
  const { logout } = useLogout();

  const isAllView = activePortfolioId === "all";
  const hasPortfolio = !isAllView;
  const hasAccount = hasPortfolio && activeAccountId !== null;
  // ── Data fetching ───────────────────────────────────────────────────────────

  // Net worth for the selected portfolio — skip on "all" view.
  const { data: netWorth } = useNetWorth(
    isAllView ? "" : activePortfolioId,
    { enabled: hasPortfolio }
  );

  // Full portfolio detail — needed to sum cash balances when no account is
  // selected. PortfolioResponse.accounts is AccountSummary[], each with a
  // top-level `cashBalance: number` field (pre-converted to base currency).
  // Only fetch when a specific portfolio is selected but no account is drilled
  // into, to avoid a redundant call when accountDetail already covers it.
  const { data: portfolioDetail } = usePortfolio(
    isAllView ? "" : activePortfolioId,
    { enabled: hasPortfolio && !hasAccount }
  );

  // Account detail for granular stats when a specific account is active.
  const { data: accountDetail } = useAccount(
    activePortfolioId,
    activeAccountId ?? "",
    { enabled: hasAccount }
  );

  const activePortfolio = useMemo(
    () => portfolios?.find((p) => p.id === activePortfolioId),
    [portfolios, activePortfolioId]
  );

  const activeAccount = accountDetail || portfolios
    ?.find((p) => p.id === activePortfolioId)
    ?.accounts?.find((a) => a.id === activeAccountId);

  // ── Derived display values ──────────────────────────────────────────────────

  const totalValue = hasAccount
    ? (accountDetail?.totalValue?.amount ?? 0)
    : (netWorth?.totalNetWorth ?? activePortfolio?.totalValue ?? 0);

  // Cash balance derivation:
  //  - Account view: pull directly from the AccountView response.
  //  - Portfolio view: sum all AccountSummary.cashBalance values from the
  //    PortfolioResponse. These are already converted to the portfolio's base
  //    currency by the backend, so summing is safe.
  //  - All-portfolios view: we'd need to sum across N portfolios which means
  //    N additional requests. Not worth it for the MVP — show 0 and revisit
  //    if a dedicated aggregate endpoint gets added later.
  const cashBalance = useMemo(() => {
    if (hasAccount) {
      return accountDetail?.cashBalance?.amount ?? 0;
    }
    if (hasPortfolio && portfolioDetail?.accounts) {
      return portfolioDetail.accounts.reduce(
        (sum, acct) => sum + (acct.cashBalance ?? 0),
        0
      );
    }
    return 0;
  }, [hasAccount, hasPortfolio, accountDetail, portfolioDetail]);

  const { totalGainLoss, totalGainLossPercent } = useMemo(() => {
    let gain = 0;
    let percentage = 0;

    if (hasAccount && accountDetail) {
      // Calculate for specific account
      gain = accountDetail.totalGainLoss?.amount ?? 0;
      percentage = accountDetail.totalGainLossPercentage ?? 0;
    } else if (hasPortfolio && netWorth) {
      // Calculate for specific portfolio using netWorth data
      gain = netWorth.totalGainLoss ?? 0;
      percentage = netWorth.totalGainLossPercentage ?? 0;
    } else if (isAllView && portfolios) {
      // Aggregate gain across all portfolios for the "All Portfolios" view
      gain = portfolios.reduce((sum, p) => sum + (p.totalGainLoss ?? 0), 0);

      const totalCost = portfolios.reduce((sum, p) => sum + (p.totalCost ?? 0), 0);
      percentage = totalCost > 0 ? (gain / totalCost) * 100 : 0;
    }

    return { totalGainLoss: gain, totalGainLossPercent: percentage };
  }, [hasAccount, accountDetail, hasPortfolio, netWorth, isAllView, portfolios]);

  // ── Handlers ────────────────────────────────────────────────────────────────

  const handleSelectPortfolio = (id: string) => {
    setActivePortfolioId(id);
    setActiveAccountId(null);
  };

  const handleSelectAccount = (portfolioId: string, accountId: string) => {
    setActivePortfolioId(portfolioId);
    setActiveAccountId(accountId);
  };

  const handleCreatePortfolio = (data: CreatePortfolioRequest) =>
    createPortfolio(data);

  // Account creation is fully owned by PortfolioSidebar via useCreateAccount.
  // This prop exists for future extensibility (e.g., analytics, toast at page
  // level) but does not need to trigger any mutation here.
  const handleCreateAccount = (
    _portfolioId: string,
    _data: CreateAccountRequest
  ) => { };

  // ── Loading state ──────────────────────────────────────────────────────────

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="animate-pulse text-lg text-muted-foreground">
          Loading your financial data…
        </div>
      </div>
    );
  }

  // ── Page title ─────────────────────────────────────────────────────────────

  let pageTitle = "All Portfolios";
  let pageSubtitle = "Consolidated view of all your investments";

  if (!isAllView && activePortfolio) {
    pageTitle = activePortfolio.name;
    pageSubtitle = "Portfolio overview";
  }

  if (hasAccount && accountDetail) {
    pageTitle = accountDetail.name ?? pageTitle;
    pageSubtitle = `${accountDetail.type ?? ""} · ${accountDetail.status ?? ""}`;
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background">
      <Navbar onLogout={logout} onOpenSettings={() => setSettingsOpen(true)} />
      <SettingsDialog open={settingsOpen} onOpenChange={setSettingsOpen} />
      <Toaster />

      <div className="flex flex-1 overflow-hidden">
        {/* ── Sidebar ── */}
        <aside className="w-80 shrink-0 border-r bg-card overflow-auto">
          <PortfolioSidebar
            portfolios={portfolios}
            activePortfolioId={activePortfolioId}
            activeAccountId={activeAccountId}
            onSelectPortfolio={handleSelectPortfolio}
            onSelectAccount={handleSelectAccount}
            onCreatePortfolio={handleCreatePortfolio}
            onCreateAccount={handleCreateAccount}
          />
        </aside>

        {/* ── Main ── */}
        <main className="flex-1 overflow-auto bg-slate-50/30">
          <div className="p-4 md:p-8">
            <div className="mx-auto max-w-7xl space-y-8">

              {/* Header */}
              <div className="flex items-start justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <h1 className="text-3xl font-bold tracking-tight">
                      {pageTitle}
                    </h1>

                    {/* Only show edit actions if we have a valid account selected */}
                    {hasAccount && activeAccount && (
                      <EditAccountDialog
                        portfolioId={activePortfolioId}
                        accountId={activeAccountId!}
                        account={activeAccount as AccountView} // Type assertion to bypass the "Summary vs View" check
                        onAccountClosed={() => {
                          setActiveAccountId(null); // Reset selection
                        }}
                      />
                    )}
                  </div>
                  <p className="mt-1 text-muted-foreground">{pageSubtitle}</p>
                </div>

                {hasAccount && (
                  <AddTransactionDialog
                    portfolioId={activePortfolioId}
                    accountId={activeAccountId!}
                  />
                )}
              </div>

              {/* Key stats */}
              <PortfolioOverview
                totalValue={totalValue}
                cashBalance={cashBalance}
                totalGainLoss={totalGainLoss}
                totalGainLossPercent={totalGainLossPercent}
              />

              {/* Charts */}
              <div className="grid gap-8 lg:grid-cols-2">
                <PerformanceChart portfolioId={activePortfolioId} />
                <AllocationChart portfolioId={activePortfolioId} />
              </div>

              {/* Detail tables */}
              <Tabs defaultValue="holdings" className="space-y-4">
                <TabsList className="grid w-full max-w-md grid-cols-2">
                  <TabsTrigger value="holdings">Holdings</TabsTrigger>
                  <TabsTrigger value="transactions">Transactions</TabsTrigger>
                </TabsList>

                <TabsContent
                  value="holdings"
                  className="border rounded-xl bg-card p-4"
                >
                  {hasAccount ? (
                    <StockHoldings
                      portfolioId={activePortfolioId}
                      accountId={activeAccountId!}
                    />
                  ) : (
                    <p className="py-10 text-center text-sm text-muted-foreground">
                      Select an account from the sidebar to view its holdings.
                    </p>
                  )}
                </TabsContent>

                <TabsContent
                  value="transactions"
                  className="border rounded-xl bg-card p-4"
                >
                  {hasAccount ? (
                    <TransactionHistory
                      portfolioId={activePortfolioId}
                      accountId={activeAccountId!}
                    />
                  ) : (
                    <p className="py-10 text-center text-sm text-muted-foreground">
                      Select an account from the sidebar to view its
                      transactions.
                    </p>
                  )}
                </TabsContent>
              </Tabs>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}