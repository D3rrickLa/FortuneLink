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

export default function DashboardPage() {
  const [activePortfolioId, setActivePortfolioId] = useState('all');
  const [settingsOpen, setSettingsOpen] = useState(false);
  const { portfolios, isLoading, createPortfolio } = usePortfolios();
  const { logout } = useLogout();

  const isAllPortfoliosView = activePortfolioId === 'all';

  const activePortfolio = useMemo(() => {
    return portfolios?.find((p) => p.id == activePortfolioId);
  }, [portfolios, activePortfolioId]);

  // Placeholder logic for your "Figma" stats
  // In production, these should come from your API or a useMemo calculation
  const totalValue = isAllPortfoliosView ? 125000.50 : activePortfolio?.totalValue || 0;
  const cashBalance = activePortfolio?.totalValue || 5000;
  const totalGainLoss = 1240.20;
  const totalGainLossPercent = 5.2;

  // 3. Handlers
  const handleOpenSettings = () => setSettingsOpen(true);
  const handleCreatePortfolio = (data: { name: string; description?: string; currency: string; createDefaultAccount: boolean; defaultAccountType?: "FHSA" | "TFSA" | "RRSP" | "RESP" | "ROTH_IRA" | "SOLO_401K" | "CHEQUING" | "SAVINGS" | "MARGIN" | "TAXABLE_INVESTMENT" | "NON_REGISTERED_INVESTMENT"; defaultStrategy?: "ACB" | "FIFO" | "LIFO" | "SPECIFIC_ID"; }) => createPortfolio(data);
  const handleAddTransaction = (data: any) => {
    console.log("Adding transaction to:", activePortfolioId, data);
    // Add your mutation logic here
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="animate-pulse text-lg">Loading your financial data...</div>
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background">
      <Navbar onLogout={logout} onOpenSettings={handleOpenSettings} />
      <SettingsDialog open={settingsOpen} onOpenChange={setSettingsOpen} />
      <Toaster />


      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar */}
        <aside className="w-80 flex-shirnk-0 border-r bg-card">
          <PortfolioSidebar
            portfolios={portfolios}
            activePortfolioId={activePortfolioId}
            onSelectPortfolio={setActivePortfolioId}
            onCreatePortfolio={handleCreatePortfolio}
          />
        </aside>

        {/* Main Content Area */}
        <main className="flex-1 overflow-auto bg-slate-50/30">
          <div className="p-4 md:p-8">
            <div className="mx-auto ax-w-7xl space-y-8">
              {/* Header Section */}
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="text-3xl font-bold tracking-tight">
                    {isAllPortfoliosView ? 'All Portfolios' : activePortfolio?.name}
                  </h1>
                  <p className="text-muted-foreground">
                    {isAllPortfoliosView
                      ? 'Consolidated view of all your investments'
                      : 'Track your investments and cash flow'}
                  </p>
                </div>
                {!isAllPortfoliosView && (
                  <AddTransactionDialog onAddTransaction={handleAddTransaction} />
                )}
              </div>
              {/* Key Stats Grid */}
              <PortfolioOverview
                totalValue={totalValue}
                cashBalance={cashBalance}
                totalGainLoss={totalGainLoss}
                totalGainLossPercent={totalGainLossPercent}
              />

              {/* Charts Grid */}
              <div className="grid gap-8 lg:grid-cols-2">
                <PerformanceChart portfolioId={activePortfolioId} />
                <AllocationChart portfolioId={activePortfolioId} />
              </div>

              {/* Detailed Tables */}
              <Tabs defaultValue="holdings" className="space-y-4">
                <TabsList className="grid w-full max-w-md grid-cols-2">
                  <TabsTrigger value="holdings">Stock Holdings</TabsTrigger>
                  <TabsTrigger value="transactions">Transaction History</TabsTrigger>
                </TabsList>

                <TabsContent value="holdings" className="border rounded-xl bg-card p-4">
                  <StockHoldings portfolioId={activePortfolioId} />
                </TabsContent>

                <TabsContent value="transactions" className="border rounded-xl bg-card p-4">
                  <TransactionHistory portfolioId={activePortfolioId} />
                </TabsContent>
              </Tabs>
            </div>
          </div>
        </main>
      </div>
    </div>
  );
}