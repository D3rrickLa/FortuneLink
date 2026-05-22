"use client";

import { useEffect, useState } from "react";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

import { Label } from "@/components/ui/label";

import { Button } from "@/components/ui/button";

import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from "@/components/ui/tabs";

import { Switch } from "@/components/ui/switch";

import { Separator } from "@/components/ui/separator";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

import {
  useThemePreference,
} from "@/features/theme/hooks/useThemePreference";

import {
  useUserPreferences,
  useUpdateUserPreferences,
} from "@/features/user/api/useUserPreferences";

interface SettingsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function SettingsDialog({
  open,
  onOpenChange,
}: SettingsDialogProps) {
  /**
   * Theme (local/browser)
   */
  const { theme, updateTheme } =
    useThemePreference();

  /**
   * Backend preferences
   */
  const { data: preferences } =
    useUserPreferences();

  const updatePreferences =
    useUpdateUserPreferences();

  /**
   * Local form state
   */
  const [
    selectedCurrency,
    setSelectedCurrency,
  ] = useState("CAD");

  const [
    emailNotifications,
    setEmailNotifications,
  ] = useState(true);

  const [priceAlerts, setPriceAlerts] =
    useState(true);

  const [dateFormat, setDateFormat] =
    useState("MM/DD/YYYY");

  /**
   * Sync backend data into form
   */
  useEffect(() => {
    if (!preferences) {
      return;
    }

    setSelectedCurrency(
      preferences.baseCurrency
    );

    setEmailNotifications(
      preferences.emailNotifications
    );

    setPriceAlerts(
      preferences.priceAlerts
    );

    setDateFormat(
      preferences.dateFormat
    );
  }, [preferences]);

  /**
   * Save
   */
  const handleSavePreferences =
    async () => {
      try {
        /**
         * Persist backend preferences
         */
        await updatePreferences.mutateAsync({
          baseCurrency:
            selectedCurrency,

          emailNotifications,

          priceAlerts,

          dateFormat,
        });

        onOpenChange(false);
      } catch {
        // handled by mutation
      }
    };

  return (
    <Dialog
      open={open}
      onOpenChange={onOpenChange}
    >
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            Settings
          </DialogTitle>

          <DialogDescription>
            Manage your application preferences
          </DialogDescription>
        </DialogHeader>

        <Tabs
          defaultValue="notifications"
          className="mt-4"
        >
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="notifications">
              Notifications
            </TabsTrigger>

            <TabsTrigger value="preferences">
              Preferences
            </TabsTrigger>
          </TabsList>

          {/* NOTIFICATIONS */}
          <TabsContent
            value="notifications"
            className="space-y-4 mt-4"
          >
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>
                    Email Notifications
                  </Label>

                  <p className="text-sm text-muted-foreground">
                    Receive portfolio updates
                    by email
                  </p>
                </div>

                <Switch
                  checked={
                    emailNotifications
                  }
                  onCheckedChange={
                    setEmailNotifications
                  }
                />
              </div>

              <Separator />

              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>
                    Price Alerts
                  </Label>

                  <p className="text-sm text-muted-foreground">
                    Get notified about major
                    price changes
                  </p>
                </div>

                <Switch
                  checked={priceAlerts}
                  onCheckedChange={
                    setPriceAlerts
                  }
                />
              </div>
            </div>
          </TabsContent>

          {/* PREFERENCES */}
          <TabsContent
            value="preferences"
            className="space-y-4 mt-4"
          >
            <div className="space-y-4">
              {/* THEME */}
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>
                    Dark Mode
                  </Label>

                  <p className="text-sm text-muted-foreground">
                    Switch between light and
                    dark themes
                  </p>
                </div>

                <Switch
                  checked={theme === "dark"}
                  onCheckedChange={(
                    checked
                  ) =>
                    updateTheme(
                      checked
                        ? "dark"
                        : "light"
                    )
                  }
                />
              </div>

              <Separator />

              {/* CURRENCY */}
              <div className="space-y-2">
                <Label>
                  Reporting Currency
                </Label>

                <Select
                  value={selectedCurrency}
                  onValueChange={
                    setSelectedCurrency
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value="CAD">
                      CAD — Canadian Dollar
                    </SelectItem>

                    <SelectItem value="USD">
                      USD — US Dollar
                    </SelectItem>

                    <SelectItem value="EUR">
                      EUR — Euro
                    </SelectItem>

                    <SelectItem value="GBP">
                      GBP — British Pound
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {/* DATE FORMAT */}
              <div className="space-y-2">
                <Label>
                  Date Format
                </Label>

                <Select
                  value={dateFormat}
                  onValueChange={
                    setDateFormat
                  }
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>

                  <SelectContent>
                    <SelectItem value="MM/DD/YYYY">
                      MM/DD/YYYY
                    </SelectItem>

                    <SelectItem value="DD/MM/YYYY">
                      DD/MM/YYYY
                    </SelectItem>

                    <SelectItem value="YYYY-MM-DD">
                      YYYY-MM-DD
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </TabsContent>
        </Tabs>

        <div className="flex justify-end gap-2 pt-4">
          <Button
            variant="outline"
            onClick={() =>
              onOpenChange(false)
            }
          >
            Cancel
          </Button>

          <Button
            onClick={handleSavePreferences}
            disabled={
              updatePreferences.isPending
            }
          >
            Save Changes
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}