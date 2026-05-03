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
import { useNetWorth } from "@/features/portfolio/queries/usePortfolio";
import { useAccount } from "@/features/portfolio/queries/useAccount";
import { CreateAccountRequest, CreatePortfolioRequest } from "@/lib/api/types";

export default function DashboardPage() {
  const [activePortfolioId, setActivePortfolioId] = useState("all");
  const [activeAccountId, setActiveAccountId] = useState<string | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const { portfolios, isLoading, createPortfolio } = usePortfolios();
  const { logout } = useLogout();

  const isAllView = activePortfolioId === "all";
  const hasAccount = !isAllView && activeAccountId !== null;

  // Live net worth for the selected portfolio
  const { data: netWorth } = useNetWorth(
    isAllView ? "" : activePortfolioId,
    { enabled: !isAllView }
  );

  // Account detail for stats when an account is selected
  const { data: accountDetail } = useAccount(
    activePortfolioId,
    activeAccountId ?? "",
    { enabled: hasAccount }
  );

  const activePortfolio = useMemo(
    () => portfolios?.find((p) => p.id === activePortfolioId),
    [portfolios, activePortfolioId]
  );

  // ── Derived display values ─────────────────────────────────────────────────

  const totalValue = hasAccount
    ? (accountDetail?.totalValue?.amount ?? 0)
    : (netWorth?.totalNetWorth ?? activePortfolio?.totalValue ?? 0);

  const cashBalance = hasAccount
    ? (accountDetail?.cashBalance?.amount ?? 0)
    : 0;

  // Gain/loss isn't in the net-worth response; keep as 0 until you wire
  // a performance endpoint. Honest zeros beat fabricated numbers.
  const totalGainLoss = 0;
  const totalGainLossPercent = 0;

  // ── Handlers ──────────────────────────────────────────────────────────────

  const handleSelectPortfolio = (id: string) => {
    setActivePortfolioId(id);
    setActiveAccountId(null); // clear account selection when switching portfolios
  };

  const handleSelectAccount = (portfolioId: string, accountId: string) => {
    setActivePortfolioId(portfolioId);
    setActiveAccountId(accountId);
  };

  const handleCreatePortfolio = (data: CreatePortfolioRequest) =>
    createPortfolio(data);

  // createAccount is handled inside PortfolioSidebar via its own hook
  const handleCreateAccount = (portfolioId: string, data: CreateAccountRequest) => {
    // Intentionally left, sidebar owns this flow
  };

  // ── Loading state ─────────────────────────────────────────────────────────

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
                  <h1 className="text-3xl font-bold tracking-tight">
                    {pageTitle}
                  </h1>
                  <p className="mt-1 text-muted-foreground">{pageSubtitle}</p>
                </div>

                {/*
                  Only show the Add Transaction button when an account is
                  selected — transactions are always account-scoped.
                */}
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

                <TabsContent value="holdings" className="border rounded-xl bg-card p-4">
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

                <TabsContent value="transactions" className="border rounded-xl bg-card p-4">
                  {hasAccount ? (
                    <TransactionHistory
                      portfolioId={activePortfolioId}
                      accountId={activeAccountId!}
                    />
                  ) : (
                    <p className="py-10 text-center text-sm text-muted-foreground">
                      Select an account from the sidebar to view its transactions.
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