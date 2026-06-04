"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import apiClient from "@/lib/api/client";

export function useUpdateUserProfile() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (fullName: string) => {
      await apiClient.put("/api/v1/users/me/profile", {
        fullName,
      });
    },

    onSuccess: async () => {
      toast.success("Profile updated");

      await queryClient.invalidateQueries({
        queryKey: ["current-user"],
      });
    },

    onError: () => {
      toast.error("Failed to update profile");
    },
  });
}