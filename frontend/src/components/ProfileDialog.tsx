"use client";

import { useEffect, useState } from "react";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from "@/components/ui/dialog";

import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Separator } from "@/components/ui/separator";

import { Loader2 } from "lucide-react";
import { toast } from "sonner";

import { createClient } from "@/lib/supabase/client";

import { useCurrentUser } from "@/features/auth/hooks/useCurrentUser";
import { useUpdateUserProfile } from "@/features/auth/hooks/useUserProfile";

interface ProfileDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ProfileDialog({
  open,
  onOpenChange,
}: ProfileDialogProps) {
  const { data: currentUser } = useCurrentUser();
  const updateProfile = useUpdateUserProfile();

  const [authLoading, setAuthLoading] = useState(false);

  // Profile
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");

  // Password
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  useEffect(() => {
    if (currentUser) {
      setFullName(currentUser.fullName ?? "");
      setEmail(currentUser.email ?? "");
    }
  }, [currentUser]);

  // ---------------- PROFILE SAVE ----------------
  const handleSaveProfile = async () => {
    try {
      await updateProfile.mutateAsync({ fullName });

      toast.success("Profile updated");
    } catch {
      toast.error("Failed to update profile");
    }
  };

  // ---------------- EMAIL SAVE ----------------
  const handleUpdateEmail = async () => {
    try {
      setAuthLoading(true);

      const supabase = createClient();

      const { error } = await supabase.auth.updateUser({
        email,
      });

      if (error) throw error;

      toast.success("Verification email sent");
    } catch (err: any) {
      toast.error(err?.message || "Failed to update email");
    } finally {
      setAuthLoading(false);
    }
  };

  // ---------------- PASSWORD SAVE ----------------
  const handleUpdatePassword = async () => {
    try {
      if (newPassword !== confirmPassword) {
        toast.error("Passwords do not match");
        return;
      }

      if (newPassword.length < 6) {
        toast.error("Password too short");
        return;
      }

      setAuthLoading(true);

      const supabase = createClient();

      const {
        data: { user },
      } = await supabase.auth.getUser();

      if (!user?.email) throw new Error("User not found");

      const { error: signInError } =
        await supabase.auth.signInWithPassword({
          email: user.email,
          password: currentPassword,
        });

      if (signInError) {
        throw new Error("Current password is incorrect");
      }

      const { error } = await supabase.auth.updateUser({
        password: newPassword,
      });

      if (error) throw error;

      toast.success("Password updated");

      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
    } catch (err: any) {
      toast.error(err?.message || "Failed to update password");
    } finally {
      setAuthLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Profile Settings</DialogTitle>
          <DialogDescription>
            Manage your account and security settings
          </DialogDescription>
        </DialogHeader>

        <Tabs defaultValue="profile" className="space-y-4">
          <TabsList className="grid grid-cols-2 w-full">
            <TabsTrigger value="profile">Profile</TabsTrigger>
            <TabsTrigger value="security">Security</TabsTrigger>
          </TabsList>

          {/* ---------------- PROFILE TAB ---------------- */}
          <TabsContent value="profile" className="space-y-4">
            <div className="space-y-2">
              <Label>Full Name</Label>
              <Input value={fullName} onChange={(e) => setFullName(e.target.value)} />
            </div>

            <div className="space-y-2">
              <Label>Email</Label>
              <Input value={email} onChange={(e) => setEmail(e.target.value)} />
              <p className="text-xs text-muted-foreground">
                Email change requires verification
              </p>
            </div>

            <Separator />

            <div className="flex justify-end gap-2">
              <Button onClick={handleSaveProfile} disabled={updateProfile.isPending}>
                {updateProfile.isPending ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Saving...
                  </>
                ) : (
                  "Save Profile"
                )}
              </Button>

              <Button
                variant="outline"
                onClick={handleUpdateEmail}
                disabled={authLoading}
              >
                Update Email
              </Button>
            </div>
          </TabsContent>

          {/* ---------------- SECURITY TAB ---------------- */}
          <TabsContent value="security" className="space-y-4">
            <div className="space-y-2">
              <Label>Current Password</Label>
              <Input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label>New Password</Label>
              <Input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
            </div>

            <div className="space-y-2">
              <Label>Confirm Password</Label>
              <Input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>

            <div className="flex justify-end">
              <Button onClick={handleUpdatePassword} disabled={authLoading}>
                {authLoading ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Updating...
                  </>
                ) : (
                  "Update Password"
                )}
              </Button>
            </div>
          </TabsContent>
        </Tabs>
      </DialogContent>
    </Dialog>
  );
}