// src/features/auth/hooks/useUserPreferences.ts

import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query";

import { toast } from "sonner";

import apiClient from "@/lib/api/client";

export interface CurrencyPreferenceResponse {
  currency: string;
}

/**
 * Backend-owned reporting currency preference.
 */
export function useBaseCurrencyPreference() {
  return useQuery({
    queryKey: ["user-preferences", "currency"],

    queryFn:
      async (): Promise<CurrencyPreferenceResponse> => {
        const response = await apiClient.get(
          "/api/v1/users/me/preferences/currency"
        );

        return response.data;
      },
  });
}

/**
 * Updates backend-owned reporting currency.
 */
export function useUpdateBaseCurrency() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (currency: string) => {
      await apiClient.put(
        "/api/v1/users/me/preferences/currency",
        {
          currency,
        }
      );
    },

    onSuccess: async () => {
      toast.success(
        "Currency preference updated"
      );

      await queryClient.invalidateQueries({
        queryKey: [
          "user-preferences",
          "currency",
        ],
      });
    },

    onError: () => {
      toast.error(
        "Failed to update currency preference"
      );
    },
  });
}