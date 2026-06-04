"use client";

import { useState } from "react";
import { Navbar } from "@/components/Navbar";
import { SettingsDialog } from "@/components/SettingsDialog";
import { Toaster } from "@/components/ui/sonner";
import { useLogout } from "@/features/auth/hooks/useLogout";

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [settingsOpen, setSettingsOpen] = useState(false);
  const { logout } = useLogout();

  return (
    <div className="flex h-screen flex-col overflow-hidden bg-background">
      <Navbar onLogout={logout} onOpenSettings={() => setSettingsOpen(true)}/>
      <SettingsDialog open={settingsOpen} onOpenChange={setSettingsOpen}/>
      <Toaster />
      {children}
    </div>
  );
}