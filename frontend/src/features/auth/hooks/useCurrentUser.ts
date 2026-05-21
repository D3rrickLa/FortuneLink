"use client";

import { useQuery } from "@tanstack/react-query";
import apiClient from "@/lib/api/client";

export interface CurrentUser {
  fullName: string;
  email: string;
}

export function useCurrentUser() {
  return useQuery({
    queryKey: ["current-user"],

    queryFn: async (): Promise<CurrentUser> => {
      const response = await apiClient.get(
        "/api/v1/users/me/profile"
      );

      return response.data;
    },
  });
}