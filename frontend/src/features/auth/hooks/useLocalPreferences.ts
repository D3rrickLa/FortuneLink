"use client";

import { toast } from "sonner";

export interface LocalUserPreferences {
  emailNotifications: boolean;
  priceAlerts: boolean;
  darkMode: boolean;
  alertThreshold: number;
  dateFormat: string;
}

const STORAGE_KEY = "user_preferences";

const DEFAULT_PREFERENCES: LocalUserPreferences = {
  emailNotifications: true,
  priceAlerts: true,
  darkMode: false,
  alertThreshold: 5,
  dateFormat: "MM/DD/YYYY",
};

/**
 * Browser-only UI preferences.
 */
export function useLocalPreferences() {
  const getPreferences =
    (): LocalUserPreferences => {
      if (typeof window === "undefined") {
        return DEFAULT_PREFERENCES;
      }

      const stored =
        localStorage.getItem(STORAGE_KEY);

      if (!stored) {
        return DEFAULT_PREFERENCES;
      }

      try {
        return {
          ...DEFAULT_PREFERENCES,
          ...JSON.parse(stored),
        };
      } catch {
        return DEFAULT_PREFERENCES;
      }
    };

  const savePreferences = (
    preferences: LocalUserPreferences
  ) => {
    try {
      localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify(preferences)
      );

      toast.success(
        "Preferences saved successfully"
      );

      return true;
    } catch {
      toast.error(
        "Failed to save preferences"
      );

      return false;
    }
  };

  const applyTheme = (
    darkMode: boolean
  ) => {
    if (typeof document === "undefined") {
      return;
    }

    if (darkMode) {
      document.documentElement.classList.add(
        "dark"
      );
    } else {
      document.documentElement.classList.remove(
        "dark"
      );
    }
  };

  return {
    getPreferences,
    savePreferences,
    applyTheme,
  };
}