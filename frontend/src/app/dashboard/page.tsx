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
import { useAddTransaction } from "@/features/portfolio/hooks/useAddTransaction";
import { CreateAccountRequest } from "@/lib/api/types";
import { createAccount } from "@/features/portfolio/services/account.services";
import { useAuth } from "@/features/auth/hooks/userAuth";
import { useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/api/queryKeys";
import { toast } from "sonner";
import { useAccount } from "@/features/portfolio/queries/useAccount";

export default function DashboardPage() {
  const [activePortfolioId, setActivePortfolioId] = useState('all');
  const [activeAccountId, setActiveAccountId] = useState<string | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);

  // Hooks
  const { portfolios, isLoading, createPortfolio } = usePortfolios();
  const { mutate: addTransaction } = useAddTransaction();
  const { logout } = useLogout();
  const { user } = useAuth(); // NEW: Get user context
  const queryClient = useQueryClient(); // NEW: Access the query cache

  const { data: activeAccountData } = useAccount(
    activePortfolioId,
    activeAccountId ?? "",
    { enabled: Boolean(activeAccountId) }
  );

  const isAllPortfoliosView = activePortfolioId === 'all';

  const activePortfolio = useMemo(() => {
    return portfolios?.find((p) => p.id == activePortfolioId);
  }, [portfolios, activePortfolioId]);

  const totalValue = isAllPortfoliosView ? 125000.50 : activePortfolio?.totalValue || 0;
  const cashBalance = activePortfolio?.totalValue || 5000;
  const totalGainLoss = 1240.20;
  const totalGainLossPercent = 5.2;

  // --- HANDLERS ---
  const handleOpenSettings = () => setSettingsOpen(true);

  const handleCreatePortfolio = (data: any) => createPortfolio(data);

  // --- NEW: Account Creation Logic ---
  const handleCreateAccount = async (portfolioId: string, data: CreateAccountRequest) => {
    if (!user) {
      toast.error("You must be logged in to create an account");
      return;
    }

    try {
      await createAccount(user.id, portfolioId, data);

      // Invalidate the specific query so the sidebar updates automatically
      queryClient.invalidateQueries({
        queryKey: queryKeys.accounts.all(portfolioId),
      });

      // Optional: Also invalidate portfolios if the count/total changes
      queryClient.invalidateQueries({ queryKey: queryKeys.portfolios.all() });

      toast.success("Account created successfully");
    } catch (error) {
      toast.error("Failed to create account");
      console.error(error);
    }
  };

  const handleAddTransaction = (data: any) => {
    addTransaction({
      ...data,
      portfolio_id: activePortfolioId,
    });
  };

  const handleSelectPortfolio = (id: string) => {
    setActivePortfolioId(id);
    setActiveAccountId(null);
  };

  const handleSelectAccount = (portfolioId: string, accountId: string) => {
    setActivePortfolioId(portfolioId);
    setActiveAccountId(accountId);
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
        <aside className="w-80 flex-shrink-0 border-r bg-card">
          <PortfolioSidebar
            portfolios={portfolios}
            activePortfolioId={activePortfolioId}
            activeAccountId={activeAccountId}
            onSelectPortfolio={handleSelectPortfolio}
            onSelectAccount={handleSelectAccount}
            onCreatePortfolio={handleCreatePortfolio}
            onCreateAccount={handleCreateAccount} // NEW PROB ADDED HERE
          />
        </aside>

        <main className="flex-1 overflow-auto bg-slate-50/30">
          <div className="p-4 md:p-8">
            <div className="mx-auto max-w-7xl space-y-8">
              <div className="flex items-center justify-between">
                <div>
                  <h1 className="text-3xl font-bold tracking-tight">
                    {activeAccountId
                      ? (activeAccountData?.name ?? "Account View")
                      : isAllPortfoliosView
                        ? "All Portfolios"
                        : activePortfolio?.name}
                  </h1>
                  <p className="text-muted-foreground">
                    {activeAccountId
                      ? "Manage transactions for this account"
                      : isAllPortfoliosView
                        ? "Consolidated view of all your investments"
                        : "Track your investments and cash flow"}
                  </p>
                </div>
                {activeAccountId && (
                  <AddTransactionDialog onAddTransaction={handleAddTransaction} />
                )}
              </div>

              <PortfolioOverview
                totalValue={totalValue}
                cashBalance={cashBalance}
                totalGainLoss={totalGainLoss}
                totalGainLossPercent={totalGainLossPercent}
              />

              <div className="grid gap-8 lg:grid-cols-2">
                <PerformanceChart portfolioId={activePortfolioId} />
                <AllocationChart portfolioId={activePortfolioId} />
              </div>

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