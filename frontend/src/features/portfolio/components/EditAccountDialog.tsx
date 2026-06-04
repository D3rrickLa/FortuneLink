"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2, MoreHorizontal, Pencil, X } from "lucide-react";
import { toast } from "sonner";
import { useUpdateAccount, useCloseAccount } from "../queries/useAccount";
import type { AccountView } from "@/lib/api/types";

// ─── Types ────────────────────────────────────────────────────────────────────

interface EditAccountDialogProps {
  portfolioId: string;
  accountId: string;
  account: AccountView;
  /** Called after a successful close so the parent can clear the active selection. */
  onAccountClosed?: () => void;
}

type ActiveModal = "rename" | "close" | null;

// ─── Component ────────────────────────────────────────────────────────────────

export function EditAccountDialog({
  portfolioId,
  accountId,
  account,
  onAccountClosed,
}: EditAccountDialogProps) {
  const [activeModal, setActiveModal] = useState<ActiveModal>(null);
  const [newName, setNewName] = useState(account.name ?? "");

  const updateAccount = useUpdateAccount(portfolioId, accountId);
  const closeAccount = useCloseAccount(portfolioId);

  // ── Rename ────────────────────────────────────────────────────────────────

  const handleRename = async (e: React.SubmitEvent) => {
    e.preventDefault();

    const trimmed = newName.trim();
    if (!trimmed) return;
    if (trimmed === account.name) {
      setActiveModal(null);
      return;
    }

    try {
      await updateAccount.mutateAsync({ accountName: trimmed });
      toast.success("Account renamed.");
      setActiveModal(null);
    } catch {
      toast.error("Failed to rename account. Please try again.");
    }
  };

  // ── Close ─────────────────────────────────────────────────────────────────

  const handleCloseAccount = async () => {
    try {
      await closeAccount.mutateAsync(accountId);
      toast.success("Account closed.");
      setActiveModal(null);
      onAccountClosed?.();
    } catch (err: unknown) {
      // The backend returns 409 when balance or positions are non-zero.
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 409) {
        toast.error(
          "Cannot close account: clear all positions and bring the cash balance to zero first."
        );
      } else {
        toast.error("Failed to close account. Please try again.");
      }
    }
  };

  const openModal = (modal: ActiveModal) => {
    // Sync the rename input with the latest name each time the modal opens
    if (modal === "rename") setNewName(account.name ?? "");
    setActiveModal(modal);
  };

  const isClosed = account.status === "CLOSED";

  return (
    <>
      {/* ── Trigger ── */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 shrink-0"
            aria-label="Account actions"
          >
            <MoreHorizontal className="h-4 w-4" />
          </Button>
        </DropdownMenuTrigger>

        <DropdownMenuContent align="start" className="w-48">
          <DropdownMenuLabel>Account Actions</DropdownMenuLabel>
          <DropdownMenuSeparator />

          <DropdownMenuItem onClick={() => openModal("rename")}>
            <Pencil className="mr-2 h-4 w-4" />
            Rename Account
          </DropdownMenuItem>

          <DropdownMenuSeparator />

          <DropdownMenuItem
            disabled={isClosed}
            className="text-destructive focus:text-destructive disabled:opacity-50"
            onClick={() => openModal("close")}
          >
            <X className="mr-2 h-4 w-4" />
            {isClosed ? "Already Closed" : "Close Account"}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      {/* ── Rename dialog ── */}
      <Dialog
        open={activeModal === "rename"}
        onOpenChange={(open) => !open && setActiveModal(null)}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Rename Account</DialogTitle>
            <DialogDescription>
              Update the display name for this account.
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handleRename} className="space-y-4 pt-1">
            <div className="space-y-1.5">
              <Label htmlFor="account-name">Account Name</Label>
              <Input
                id="account-name"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="e.g., TFSA Growth"
                autoFocus
                required
              />
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setActiveModal(null)}
                disabled={updateAccount.isPending}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                disabled={updateAccount.isPending || !newName.trim()}
              >
                {updateAccount.isPending && (
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                )}
                Save
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* ── Close account confirmation dialog ── */}
      <Dialog
        open={activeModal === "close"}
        onOpenChange={(open) => !open && setActiveModal(null)}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>Close Account</DialogTitle>
            <DialogDescription>
              This transitions the account to a closed state. All transaction
              history is preserved. The account can be reopened later.
            </DialogDescription>
          </DialogHeader>

          {/* Prerequisites callout */}
          <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-800/40 dark:bg-amber-900/20 dark:text-amber-400">
            <p className="font-medium">Before closing:</p>
            <ul className="mt-1 list-disc pl-4 space-y-0.5 text-xs">
              <li>Sell all open positions</li>
              <li>Withdraw or transfer out the cash balance</li>
            </ul>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setActiveModal(null)}
              disabled={closeAccount.isPending}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={handleCloseAccount}
              disabled={closeAccount.isPending}
            >
              {closeAccount.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              Close Account
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}