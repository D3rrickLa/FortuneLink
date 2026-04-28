// hooks/useLogout.ts
import { useRouter } from "next/navigation"; // Ensure you use next/navigation for App Router
import { createClient } from "@/lib/supabase/client";
import { toast } from "sonner";

export const useLogout = () => {
  // 1. CALL HOOKS AT THE TOP LEVEL
  const router = useRouter(); 
  const supabase = createClient();

  // 2. DEFINE THE LOGIC FUNCTION
  const logout = async () => {
    try {
      const { error } = await supabase.auth.signOut();
      
      if (error) throw error;

      toast.success("Logged out successfully");
      
      // Use the router instance defined above
      router.push("/");
      router.refresh();
    } catch (error: any) {
      toast.error(error.message || "Logout failed");
      console.error("Logout error:", error);
    }
  };

  return { logout };
};