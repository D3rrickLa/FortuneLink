// hooks/useLogout.ts
import { useRouter } from "next/navigation"; // Ensure you use next/navigation for App Router
import { createClient } from "@/lib/supabase/client";
import { toast } from "sonner";
import { useQueryClient } from "@tanstack/react-query";

export const useLogout = () => {
  // 1. CALL HOOKS AT THE TOP LEVEL
  const router = useRouter(); 
  const supabase = createClient();
  const queryClient = useQueryClient();

  // 2. DEFINE THE LOGIC FUNCTION
  const logout = async () => {
    try {
      const { error } = await supabase.auth.signOut();
      
      if (error) throw error;

      queryClient.clear();

      // Use the router instance defined above
      toast.success("Logged out successfully");
      router.push("/");
      router.refresh();
    } catch (error: any) {
      toast.error(error.message || "Logout failed");
      console.error("Logout error:", error);
    }
  };

  return { logout };
};