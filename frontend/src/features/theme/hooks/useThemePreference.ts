"use client";

import { useEffect, useState } from "react";

const STORAGE_KEY = "theme";

export type ThemePreference =
  | "light"
  | "dark";

export function useThemePreference() {
  const [theme, setTheme] =
    useState<ThemePreference>("light");

  useEffect(() => {
    const stored = localStorage.getItem(
      STORAGE_KEY
    ) as ThemePreference | null;

    if (stored) {
      setTheme(stored);
      applyTheme(stored);
    }
  }, []);

  const updateTheme = (
    newTheme: ThemePreference
  ) => {
    setTheme(newTheme);

    localStorage.setItem(
      STORAGE_KEY,
      newTheme
    );

    applyTheme(newTheme);
  };

  return {
    theme,
    updateTheme,
  };
}

function applyTheme(
  theme: ThemePreference
) {
  if (theme === "dark") {
    document.documentElement.classList.add(
      "dark"
    );
  } else {
    document.documentElement.classList.remove(
      "dark"
    );
  }
}