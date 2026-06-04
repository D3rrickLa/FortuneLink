"use client";

import { Navbar } from "@/components/Navbar";
import { SettingsDialog } from "@/components/SettingsDialog";
import { useLogout } from "@/features/auth/hooks/useLogout";
import { useState } from "react";
import { Toaster } from "sonner";

export default function TransactionHistory() {
  // const { logout } = useLogout();
  // const [settingsOpen, setSettingsOpen] = useState(false);
  
  return (

    <div className="flex h-screen flex-col overflow-hidden bg-background">
      {/* <Navbar onLogout={logout} onOpenSettings={() => setSettingsOpen(true)} /> */}
      {/* <SettingsDialog open={settingsOpen} onOpenChange={setSettingsOpen} /> */}
      <Toaster />
    </div>
  )
}