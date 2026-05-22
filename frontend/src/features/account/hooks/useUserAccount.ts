"use client";

import { useState } from "react";
import { createClient } from "@/lib/supabase/client";
import { toast } from "sonner";

/**
 * Authentication/account operations.
 *
 * Supabase remains the source of truth for:
 * - email
 * - password
 * - credentials
 * - sessions
 */
export function useUserAccount() {
  const [loading, setLoading] = useState(false);

  const updateEmail = async (
    newEmail: string
  ): Promise<boolean> => {
    setLoading(true);

    try {
      const supabase = createClient();

      const { error } = await supabase.auth.updateUser({
        email: newEmail,
      });

      if (error) {
        throw error;
      }

      toast.success(
        "Verification email sent. Please check your inbox."
      );

      return true;
    } catch (error: any) {
      toast.error(
        error?.message || "Failed to update email"
      );

      return false;
    } finally {
      setLoading(false);
    }
  };

  const updatePassword = async (
    currentPassword: string,
    newPassword: string
  ): Promise<boolean> => {
    setLoading(true);

    try {
      const supabase = createClient();

      const {
        data: { user },
      } = await supabase.auth.getUser();

      if (!user?.email) {
        throw new Error("User not found");
      }

      /**
       * Verify current password.
       */
      const { error: signInError } =
        await supabase.auth.signInWithPassword({
          email: user.email,
          password: currentPassword,
        });

      if (signInError) {
        throw new Error(
          "Current password is incorrect"
        );
      }

      /**
       * Apply new password.
       */
      const { error } = await supabase.auth.updateUser({
        password: newPassword,
      });

      if (error) {
        throw error;
      }

      toast.success("Password updated successfully");

      return true;
    } catch (error: any) {
      toast.error(
        error?.message || "Failed to update password"
      );

      return false;
    } finally {
      setLoading(false);
    }
  };

  return {
    loading,

    updateEmail,
    updatePassword,
  };
}