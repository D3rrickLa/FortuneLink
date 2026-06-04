// src/features/auth/hooks/useUserDisplay.ts

import { useMemo } from "react";

import { useAuth } from "../../auth/hooks/useAuth";
import { useCurrentUser } from "../../user/api/useCurrentUser";

/**
 * Generates initials from a full name.
 */
function getInitials(
  name: string | undefined,
  email: string | undefined
): string {
  if (name && name.trim()) {
    const parts = name.trim().split(/\s+/);

    if (parts.length >= 2) {
      return (
        parts[0][0] +
        parts[parts.length - 1][0]
      ).toUpperCase();
    }

    return name.substring(0, 2).toUpperCase();
  }

  if (email) {
    return email.substring(0, 2).toUpperCase();
  }

  return "??";
}

/**
 * Presentation-layer user display hook.
 *
 * IMPORTANT:
 * User profile data now comes from backend APIs,
 * not Supabase metadata.
 */
export function useUserDisplay() {
  const { user, loading: authLoading } =
    useAuth();

  const {
    data: currentUser,
    isLoading: profileLoading,
  } = useCurrentUser();

  /**
   * Email still comes from Supabase Auth.
   */
  const displayEmail = useMemo(() => {
    return user?.email || "No email";
  }, [user]);

  /**
   * Full name now comes from backend profile API.
   */
  const displayName = useMemo(() => {
    return (
      currentUser?.fullName ||
      user?.email?.split("@")[0] ||
      "User"
    );
  }, [currentUser, user]);

  /**
   * Initials derive from backend profile name.
   */
  const initials = useMemo(() => {
    return getInitials(
      currentUser?.fullName,
      user?.email
    );
  }, [currentUser, user]);

  return {
    displayName,
    displayEmail,
    initials,

    loading:
      authLoading || profileLoading,
  };
}