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

import { Input } from "@/components/ui/input";

import { toast } from "sonner";

import { useLocalPreferences } from "@/features/auth/hooks/useLocalPreferences";

import {
  useBaseCurrencyPreference,
  useUpdateBaseCurrency,
} from "@/features/auth/hooks/useUserPreferences";

interface SettingsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function SettingsDialog({
  open,
  onOpenChange,
}: SettingsDialogProps) {
  /**
   * Local browser-only preferences
   */
  const {
    getPreferences,
    savePreferences,
    applyTheme,
  } = useLocalPreferences();

  /**
   * Backend currency preference
   */
  const { data: currencyPref } =
    useBaseCurrencyPreference();

  const updateCurrency =
    useUpdateBaseCurrency();

  /**
   * Local UI state
   */
  const [selectedCurrency, setSelectedCurrency] =
    useState("CAD");

  const [
    emailNotifications,
    setEmailNotifications,
  ] = useState(true);

  const [priceAlerts, setPriceAlerts] =
    useState(true);

  const [darkMode, setDarkMode] =
    useState(false);

  const [alertThreshold, setAlertThreshold] =
    useState(5);

  const [dateFormat, setDateFormat] =
    useState("MM/DD/YYYY");

  /**
   * Load preferences
   */
  useEffect(() => {
    const prefs = getPreferences();

    setEmailNotifications(
      prefs.emailNotifications
    );

    setPriceAlerts(
      prefs.priceAlerts
    );

    setDarkMode(
      prefs.darkMode
    );

    setAlertThreshold(
      prefs.alertThreshold
    );

    setDateFormat(
      prefs.dateFormat
    );

    if (currencyPref?.currency) {
      setSelectedCurrency(
        currencyPref.currency
      );
    }
  }, [currencyPref]);

  /**
   * Save preferences
   */
  const handleSavePreferences =
    async () => {
      try {
        /**
         * Persist backend currency preference
         */
        if (
          selectedCurrency !==
          currencyPref?.currency
        ) {
          await updateCurrency.mutateAsync(
            selectedCurrency
          );
        }

        /**
         * Save local browser preferences
         */
        const success = savePreferences({
          emailNotifications,
          priceAlerts,
          darkMode,
          alertThreshold,
          dateFormat,
        });

        if (!success) {
          return;
        }

        /**
         * Apply theme immediately
         */
        applyTheme(darkMode);

        onOpenChange(false);
      } catch {
        toast.error(
          "Failed to save preferences"
        );
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

              <Separator />

              <div className="space-y-2">
                <Label htmlFor="alert-threshold">
                  Alert Threshold (%)
                </Label>

                <Input
                  id="alert-threshold"
                  type="number"
                  value={alertThreshold}
                  onChange={(e) =>
                    setAlertThreshold(
                      Number(
                        e.target.value
                      )
                    )
                  }
                  min="1"
                  max="50"
                />

                <p className="text-sm text-muted-foreground">
                  Trigger alerts when price
                  changes exceed this amount
                </p>
              </div>
            </div>
          </TabsContent>

          {/* PREFERENCES */}
          <TabsContent
            value="preferences"
            className="space-y-4 mt-4"
          >
            <div className="space-y-4">
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
                  checked={darkMode}
                  onCheckedChange={
                    setDarkMode
                  }
                />
              </div>

              <Separator />

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

                <p className="text-xs text-muted-foreground">
                  Cross-portfolio valuations
                  are normalized to this
                  currency.
                </p>
              </div>

              <div className="space-y-2">
                <Label htmlFor="date-format">
                  Date Format
                </Label>

                <select
                  id="date-format"
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  value={dateFormat}
                  onChange={(e) =>
                    setDateFormat(
                      e.target.value
                    )
                  }
                >
                  <option value="MM/DD/YYYY">
                    MM/DD/YYYY
                  </option>

                  <option value="DD/MM/YYYY">
                    DD/MM/YYYY
                  </option>

                  <option value="YYYY-MM-DD">
                    YYYY-MM-DD
                  </option>
                </select>
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
              updateCurrency.isPending
            }
          >
            Save Changes
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}