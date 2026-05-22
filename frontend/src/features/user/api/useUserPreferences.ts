"use client";

import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

import { toast } from "sonner";

import apiClient from "@/lib/api/client";

export interface UserPreferences {
  baseCurrency: string;
  emailNotifications: boolean;
  priceAlerts: boolean;
  dateFormat: string;
}

export function useUserPreferences() {
  return useQuery({
    queryKey: ["user-preferences"],

    queryFn:
      async (): Promise<UserPreferences> => {
        const response = await apiClient.get(
          "/api/v1/users/me/preferences"
        );

        return response.data;
      },
  });
}

export function useUpdateUserPreferences() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (
      preferences: UserPreferences
    ) => {
      await apiClient.put(
        "/api/v1/users/me/preferences",
        preferences
      );
    },

    onSuccess: async () => {
      toast.success(
        "Preferences updated"
      );

      await queryClient.invalidateQueries({
        queryKey: ["user-preferences"],
      });
    },

    onError: () => {
      toast.error(
        "Failed to update preferences"
      );
    },
  });
}