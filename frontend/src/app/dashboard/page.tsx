"use client";

import { Navbar } from "@/components/Navbar";
import { PortfolioSidebar } from "@/components/PortfolioSidebar";
import { SettingsDialog } from "@/components/SettingsDialog";
import { useState } from "react";
import { toast } from "sonner";


export default function DashboardPage() {
  const [settingsOpen, setSettingsOpen] = useState(false);

  const handleLogout = () => {
    toast.success("Logged out successfully");
    // In a real app, you would clear auth tokens and redirect to login
    console.log("User logged out");
  };

  const handleOpenSettings = () => {
    setSettingsOpen(true);
  };

  return (
    <div className="flex min-h-screen flex-col">
      <Navbar onLogout={handleLogout} onOpenSettings={handleOpenSettings} />
      <SettingsDialog open={settingsOpen} onOpenChange={setSettingsOpen} />

      {/* MAIN CONTENT */}
      <main className="flex flex-1 flex-col items-center justify-center p-8">
        <div className="w-80 flex-shrink-0">
          {/* <PortfolioSidebar
            portfolios={null}
            activePortfolioId={null}
            onSelectPortfolio={null}
            onCreatePortfolio={null}
          /> */}
        </div>
        <h1 className="text-3xl font-bold tracking-tight">Some Dashboard here</h1>
        <p className="text-muted-foreground mt-2">Hello World!</p>
      </main>
    </div>
  );
}