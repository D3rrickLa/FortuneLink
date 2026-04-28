"use client";

import { useState } from "react";
import { Navbar } from "@/components/Navbar";
import { PortfolioSidebar } from "@/components/PortfolioSidebar";
import { usePortfolios } from "@/features/portfolio/hooks/usePortfolios";
import { useLogout } from "@/features/portfolio/hooks/useLogout";

export default function DashboardPage() {
  const [activePortfolioId, setActivePortfolioId] = useState('all');
  const [settingsOpen, setSettingsOpen] = useState(false);
  // ALL hooks to the top (No exceptions!)
  const { portfolios, isLoading, createPortfolio } = usePortfolios();
  const { logout } = useLogout();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        Loading Portfolios...
      </div>
    );
  }

  const handleOpenSettings = () => {
    setSettingsOpen(true);
  };

  return (
    <div className="flex min-h-screen flex-col">
      {/* 3. Pass functions correctly (removed extra braces) */}
      <Navbar onLogout={logout} onOpenSettings={handleOpenSettings} />

      <main className="flex flex-1 overflow-hidden">
        <aside className="w-80 flex-shrink-0 border-r">
          <PortfolioSidebar
            portfolios={portfolios}
            activePortfolioId={activePortfolioId}
            onSelectPortfolio={setActivePortfolioId}
            onCreatePortfolio={(data) => createPortfolio(data)}
          />
        </aside>

        <section className="flex-1 p-8 overflow-auto">
          <h1 className="text-3xl font-bold">
            {activePortfolioId === 'all'
              ? "All Portfolios Overview"
              : `Portfolio: ${portfolios?.find(p => p.id === activePortfolioId)?.name}`}
          </h1>
        </section>
      </main>
    </div>
  );
}