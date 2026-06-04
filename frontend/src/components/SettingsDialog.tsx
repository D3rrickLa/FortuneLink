"use client";

import { useEffect, useMemo, useState } from "react";

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
import { Input } from "./ui/input";

interface SettingsDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

// Lazily built once. Intl.supportedValuesOf is available in all modern browsers.
// We group by region prefix (America, Europe, Asia, etc.) for usability.
function useTimezoneOptions(filter: string) {
  const all = useMemo(() => {
    try {
      return Intl.supportedValuesOf("timeZone") as string[];
    } catch {
      // Fallback for environments that don't support it (e.g. old Node in SSR)
      return ["America/Toronto", "America/New_York", "America/Los_Angeles",
        "Europe/London", "Europe/Paris", "Asia/Tokyo", "UTC"];
    }
  }, []);

  return useMemo(() => {
    const q = filter.toLowerCase();
    if (!q) return all;
    return all.filter((tz) => tz.toLowerCase().includes(q));
  }, [all, filter]);
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

  const [timeZone, setTimeZone] =
    useState(
      // Default to the browser's current timezone — sensible first-run default
      () => Intl.DateTimeFormat().resolvedOptions().timeZone
    );

  const [tzFilter, setTzFilter] = useState("");
  const timezones = useTimezoneOptions(tzFilter);

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

    setTimeZone(
      preferences.timezone
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
          baseCurrency: selectedCurrency,
          emailNotifications,
          priceAlerts,
          dateFormat,
          timezone: ""
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
              {/* TIMEZONE */}
              <div className="space-y-2">
                <Label>Timezone</Label>
                <p className="text-sm text-muted-foreground">
                  Used for date display on charts and reports
                </p>
                {/* Filter input sits outside the Select so it doesn't close on type */}
                <Input
                  placeholder="Filter timezones…"
                  value={tzFilter}
                  onChange={(e) => setTzFilter(e.target.value)}
                  className="mb-1"
                />
                <Select value={timeZone} onValueChange={(v) => {
                  setTimeZone(v);
                  setTzFilter(""); // clear filter after selection
                }}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent className="max-h-60">
                    {timezones.map((tz) => (
                      <SelectItem key={tz} value={tz}>
                        {tz.replace(/_/g, " ")}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground">
                  Current: {timeZone}
                </p>
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