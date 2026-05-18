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
import { useValuation } from "@/features/portfolio/hooks/useValuation";
import { usePortfolio } from "@/features/portfolio/queries/usePortfolio";
import { useAccount } from "@/features/portfolio/queries/useAccount";
import { AccountView, CreateAccountRequest, CreatePortfolioRequest } from "@/lib/api/types";
import { EditAccountDialog } from "@/features/portfolio/components/EditAccountDialog";
import { GainLossSummary, GAIN_LOSS_UNAVAILABLE } from "@/lib/portfolio/gainLoss";
import { EditPortfolioDialog } from "@/features/portfolio/components/EditPortfolioDialog";

export default function DashboardPage() {
  const [activePortfolioId, setActivePortfolioId] = useState("all");
  const [activeAccountId, setActiveAccountId] = useState<string | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);

  const { portfolios, isLoading: portfoliosLoading, createPortfolio } = usePortfolios();
  const { logout } = useLogout();

  const isAllView = activePortfolioId === "all";
  const hasPortfolio = !isAllView;
  const hasAccount = hasPortfolio && activeAccountId !== null;

  // ── Data fetching ───────────────────────────────────────────────────────────

  // Aggregated valuation for the "All Portfolios" view
  const globalValuation = useValuation();

  // Portfolio detail for specific views
  const { data: portfolioDetail, isLoading: portfolioLoading } = usePortfolio(
    isAllView ? "" : activePortfolioId,
    { enabled: hasPortfolio }
  );

  // Account detail for granular stats
  const { data: accountDetail, isLoading: accountLoading } = useAccount(
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

  const totalValue = useMemo(() => {
    if (hasAccount) return accountDetail?.totalValue?.amount ?? 0;
    if (hasPortfolio) return portfolioDetail?.totalValue ?? activePortfolio?.totalValue ?? 0;
    return globalValuation.totalValue;
  }, [hasAccount, hasPortfolio, accountDetail, portfolioDetail, activePortfolio, globalValuation]);

  const cashBalance = useMemo(() => {
    if (hasAccount) return accountDetail?.cashBalance?.amount ?? 0;
    if (hasPortfolio && portfolioDetail?.accounts) {
      return portfolioDetail.accounts.reduce(
        (sum, acct) => sum + (acct.cashBalance ?? 0),
        0
      );
    }
    // Global cash summary would require a backend field in ValuationResponse
    return 0;
  }, [hasAccount, hasPortfolio, accountDetail, portfolioDetail]);

  const gainLossSummary = useMemo((): GainLossSummary => {
    // 1. Account Level
    if (hasAccount && accountDetail?.assets) {
      const positions = accountDetail.assets;
      const totalGainLoss = positions.reduce(
        (sum, pos) => sum + (pos.unrealizedPnL?.amount ?? 0),
        0
      );
      const totalCostBasis = positions.reduce(
        (sum, pos) => sum + (pos.totalCostBasis?.pricePerUnit?.amount ?? 0),
        0
      );
      const totalGainLossPercent = totalCostBasis > 0 ? (totalGainLoss / totalCostBasis) * 100 : 0;
      return { totalGainLoss, totalGainLossPercent, isAvailable: true };
    }

    // 2. Global View (Using new consolidated valuation hook)
    if (isAllView && !globalValuation.isEmpty) {
      return {
        totalGainLoss: globalValuation.unrealizedGainLoss,
        totalGainLossPercent: globalValuation.returnPercentage,
        isAvailable: true
      };
    }

    // 3. Portfolio View fallback
    return GAIN_LOSS_UNAVAILABLE;
  }, [hasAccount, accountDetail, isAllView, globalValuation]);

  const { totalGainLoss, totalGainLossPercent, isAvailable: gainLossAvailable } = gainLossSummary;

  const chartCurrency = useMemo(() => {
    if (hasAccount) return accountDetail?.baseCurrency?.code ?? "USD";
    if (hasPortfolio) return activePortfolio?.currency ?? "USD";
    return globalValuation.currency;
  }, [hasAccount, hasPortfolio, accountDetail, activePortfolio, globalValuation]);

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

  const handleCreateAccount = (
    _portfolioId: string,
    _data: CreateAccountRequest
  ) => { };

  // ── Loading state ──────────────────────────────────────────────────────────

  const isInitialLoading = portfoliosLoading || (isAllView && globalValuation.isLoading);

  if (isInitialLoading) {
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

  if (hasPortfolio && activePortfolio) {
    pageTitle = activePortfolio.name;
    pageSubtitle = portfolioDetail?.description ?? activePortfolio.description ?? "Portfolio overview";
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

                    {/* Render Edit Actions for a Portfolio (when NO account is targeted) */}
                    {hasPortfolio && !hasAccount && activePortfolio && (
                      <EditPortfolioDialog
                        portfolioId={activePortfolioId}
                        initialData={{
                          name: activePortfolio.name,
                          description: portfolioDetail?.description ?? activePortfolio.description,
                          currency: portfolioDetail?.currency ?? activePortfolio.currency
                        }}
                      />
                    )}

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
                gainLossAvailable={gainLossAvailable}
              />

              {/* Charts */}
              <div className="grid gap-8 lg:grid-cols-2">
                <PerformanceChart
                  currency={chartCurrency}
                  account={hasAccount ? accountDetail ?? null : null}
                />
                <AllocationChart
                  account={hasAccount ? accountDetail ?? null : null}
                  isLoading={hasAccount && !accountDetail}
                />
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