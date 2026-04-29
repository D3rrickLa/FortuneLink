import { useMutation, useQueryClient } from "@tanstack/react-query";
import { createClient } from "@/lib/supabase/client";
import { toast } from "sonner";

export function useAddTransaction() {
  const supabase = createClient();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (newTransaction: any) => {
      const { data, error } = await supabase
        .from("transactions") // Your table name
        .insert([newTransaction])
        .select()
        .single();

      if (error) throw new Error(error.message);
      return data;
    },
    onSuccess: () => {
      // 1. Refetch portfolios/holdings to update the charts and tables immediately
      queryClient.invalidateQueries({ queryKey: ["portfolios"] });
      queryClient.invalidateQueries({ queryKey: ["holdings"] });
      
      toast.success("Transaction added successfully");
    },
    onError: (error) => {
      toast.error(`Failed to add transaction: ${error.message}`);
    },
  });
}