import { useMemo } from "react";
import { useAuth } from "./userAuth";

/**
 * Generates initials from a full name.
 */
function getInitials(name: string | undefined, email: string | undefined): string {
  if (name && name.trim()) {
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  }
  
  if (email) {
    return email.substring(0, 2).toUpperCase();
  }
  
  return "??";
}

export function useUserDisplay() {
  const { user, loading } = useAuth();

  const displayName = useMemo(() => {
    return user?.user_metadata?.full_name || user?.email?.split("@")[0] || "User";
  }, [user]);

  const displayEmail = useMemo(() => {
    return user?.email || "No email";
  }, [user]);

  const initials = useMemo(() => {
    return getInitials(user?.user_metadata?.full_name, user?.email);
  }, [user]);

  return {
    displayName,
    displayEmail,
    initials,
    loading,
    user,
  };
}