import { useState } from "react";
import { createClient } from "@/lib/supabase/client";
import { toast } from "sonner";

export interface UserProfile {
  email: string;
  fullName: string;
  userId: string;
}

export interface UserPreferences {
  emailNotifications: boolean;
  priceAlerts: boolean;
  darkMode: boolean;
  alertThreshold: number;
  dateFormat: string;
}

const DEFAULT_PREFERENCES: UserPreferences = {
  emailNotifications: true,
  priceAlerts: true,
  darkMode: false,
  alertThreshold: 5,
  dateFormat: "MM/DD/YYYY",
};


export function useUserProfile() {
  const [loading, setLoading] = useState(false);

  const updateProfile = async (fullName: string) => {
    setLoading(true);
    try {
      const supabase = createClient();

      const { error } = await supabase.auth.updateUser({
        data: { full_name: fullName }
      });

      if (error) throw error;

      toast.success("Profile updated successfully");
      return true;
    } catch (error: any) {
      toast.error(error.message || "Failed to update profile");
      return false;
    } finally {
      setLoading(false);
    }
  };

  const updateEmail = async (newEmail: string) => {
    setLoading(true);
    try {
      const supabase = createClient();

      const { error } = await supabase.auth.updateUser({
        email: newEmail
      });

      if (error) throw error;

      toast.success("Verification email sent. Please check your inbox.");
      return true;
    } catch (error: any) {
      toast.error(error.message || "Failed to update email");
      return false;
    } finally {
      setLoading(false);
    }
  };

  const updatePassword = async (currentPassword: string, newPassword: string) => {
    setLoading(true);
    try {
      const supabase = createClient();

      // First verify current password by attempting to sign in
      const { data: { user } } = await supabase.auth.getUser();
      if (!user?.email) throw new Error("User not found");

      const { error: signInError } = await supabase.auth.signInWithPassword({
        email: user.email,
        password: currentPassword,
      });

      if (signInError) throw new Error("Current password is incorrect");

      // Now update to new password
      const { error } = await supabase.auth.updateUser({
        password: newPassword
      });

      if (error) throw error;

      toast.success("Password updated successfully");
      return true;
    } catch (error: any) {
      toast.error(error.message || "Failed to update password");
      return false;
    } finally {
      setLoading(false);
    }
  };

  const getPreferences = (): UserPreferences => {
    if (typeof window === "undefined") return DEFAULT_PREFERENCES;

    const stored = localStorage.getItem("user_preferences");
    if (!stored) return DEFAULT_PREFERENCES;

    try {
      return { ...DEFAULT_PREFERENCES, ...JSON.parse(stored) };
    } catch {
      return DEFAULT_PREFERENCES;
    }
  };

  const savePreferences = (preferences: UserPreferences) => {
    try {
      localStorage.setItem("user_preferences", JSON.stringify(preferences));
      toast.success("Preferences saved successfully");
      return true;
    } catch (error) {
      toast.error("Failed to save preferences");
      return false;
    }
  };

  return {
    loading,
    updateProfile,
    updateEmail,
    updatePassword,
    getPreferences,
    savePreferences,
  };
}