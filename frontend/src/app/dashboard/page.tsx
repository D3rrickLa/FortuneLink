"use client";

import Link from "next/link";
import {
  NavigationMenu,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  navigationMenuTriggerStyle,
} from "@/components/ui/navigation-menu";
import { LogoutButton } from "@/features/auth/components/LogoutButton";

export default function DashboardPage() {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
        <div className="container mx-auto flex h-16 items-center justify-between px-4">

          {/* Left Side: Navigation Links */}
          <NavigationMenu>
            <NavigationMenuList>

              <NavigationMenuItem>
                {/* Use 'asChild' to let Next.js Link handle the routing */}
                <Link href="/dashboard" passHref>
                  <NavigationMenuLink className={navigationMenuTriggerStyle()}>
                    Overview
                  </NavigationMenuLink>
                </Link>
              </NavigationMenuItem>

              <NavigationMenuItem>
                <Link href="/analytics" passHref>
                  <NavigationMenuLink className={navigationMenuTriggerStyle()}>
                    History
                  </NavigationMenuLink>
                </Link>
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