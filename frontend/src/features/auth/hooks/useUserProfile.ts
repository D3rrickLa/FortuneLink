"use client";

import {
  useMutation,
  useQueryClient,
} from "@tanstack/react-query";

import { toast } from "sonner";
import apiClient from "@/lib/api/client";

interface UpdateProfileRequest {
  fullName: string;
}

/**
 * Backend-owned profile mutations.
 */
export function useUpdateUserProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (
      request: UpdateProfileRequest
    ) => {
      await apiClient.put(
        "/api/v1/users/me/profile",
        request
      );
    },

    /**
     * IMPORTANT:
     * Immediately refresh navbar/profile UI.
     */
    onSuccess: async (_, variables) => {
      queryClient.setQueryData(
        ["current-user"],
        {
          fullName: variables.fullName,
        }
      );

      await queryClient.invalidateQueries({
        queryKey: ["current-user"],
      });

      toast.success(
        "Profile updated successfully"
      );
    },

    onError: () => {
      toast.error("Failed to update profile");
    },
  });
}