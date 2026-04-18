// features/auth/components/LogoutButton.tsx
"use client";
import { createClient } from "@/lib/utils/supabase/client";
import { useRouter } from "next/navigation";

export function LogoutButton() {
  const router = useRouter();

  async function handleLogout() {
    const supabase = createClient();
    await supabase.auth.signOut();
    router.push("/");
    router.refresh();
  }

  return (
    <button onClick={handleLogout}>
      Sign out
    </button>
  );
}