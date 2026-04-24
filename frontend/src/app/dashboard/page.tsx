"use client";
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  navigationMenuTriggerStyle,
} from "@/components/ui/navigation-menu";
import { LogoutButton } from "@/features/auth/components/LogoutButton";
import Link from "next/link";
import { cn } from "@/lib/utils";

export default function DashboardPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container mx-auto flex h-16 items-center justify-between px-4">

          {/* Left Side: Navigation Links */}
          <NavigationMenu>
            <NavigationMenuList>
              <NavigationMenuItem>
                <NavigationMenuLink render={<Link href="/dashboard" />} className={cn(navigationMenuTriggerStyle(), "px-[var(--spacing-navigation)]")}>
                  Overview
                </NavigationMenuLink>
              </NavigationMenuItem>

              <NavigationMenuItem>
                <NavigationMenuLink render={<Link href="/analytics" />} className={cn(navigationMenuTriggerStyle(), "px-[var(--spacing-navigation)]")}>
                  History
                </NavigationMenuLink>
              </NavigationMenuItem>
            </NavigationMenuList>
          </NavigationMenu>

          {/* Right Side: Actions */}
          <div className="flex items-center gap-4">
            <LogoutButton />
          </div>
        </div>
      </header>

      {/* MAIN CONTENT */}
      <main className="flex flex-1 flex-col items-center justify-center p-8">
        <h1 className="text-3xl font-bold tracking-tight">Some Dashboard here</h1>
        <p className="text-muted-foreground mt-2">Hello World!</p>
      </main>
    </div>
  );
}