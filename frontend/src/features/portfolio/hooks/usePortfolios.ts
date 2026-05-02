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
 
  // PortfolioSummaryResponse intentionally has no accounts, it's a list view.
  // Accounts are fetched lazily per portfolio inside PortfolioItem via useAccounts.
  // We seed accounts: [] here so the sidebar never hits undefined.map().
  const portfolios: Portfolio[] = (query.data || []).map((p) => ({
    id: p.id ?? "",
    name: p.name ?? "Unnamed Portfolio",
    totalValue: p.totalValue ?? 0,
    gainLoss: 0,
    gainLossPercent: 0,
    accounts: [],
  }));
 
  const createMutation = useMutation({
    mutationFn: (data: CreatePortfolioRequest) => createPortfolio(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["portfolios"] });
    },
  });
 
  return {
    portfolios,
    isLoading: query.isLoading,
    createPortfolio: createMutation.mutate,
  };
}
 