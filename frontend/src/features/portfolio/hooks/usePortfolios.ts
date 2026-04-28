// @/features/portfolio/hooks/usePortfolios.ts
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getPortfolios, createPortfolio } from "@/features/portfolio/services"; 
import { components } from "@/lib/api/schema"; // Import types from your schema
import { Portfolio } from "@/components/PortfolioSidebar";

type CreatePortfolioRequest = components["schemas"]["CreatePortfolioRequest"];

export function usePortfolios() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["portfolios"],
    queryFn: getPortfolios,
  });

  // TRANSFORM: Map the backend objects to UI objects
  const portfolios: Portfolio[] = (query.data || []).map((p) => ({
    id: p.id ?? "",
    name: p.name ?? "New Portfolio",
    totalValue: p.totalValue ?? 0,
    // Add the missing properties required by your Figma UI
    gainLoss: 0, 
    gainLossPercent: 0,
  }));

  const createMutation = useMutation({
    mutationFn: (data: CreatePortfolioRequest) => createPortfolio(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["portfolios"] });
    },
  });

  return {
    portfolios, // This is now a clean Portfolio[] array
    isLoading: query.isLoading,
    createPortfolio: createMutation.mutate,
  };
}