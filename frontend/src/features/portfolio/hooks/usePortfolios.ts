// @/features/portfolio/hooks/usePortfolios.ts
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getPortfolios, createPortfolio, updatePortfolio } from "@/features/portfolio/services"; 
import { Portfolio } from "@/components/PortfolioSidebar";
import { CreatePortfolioRequest, UpdatePortfolioRequest } from "@/lib/api/types";

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
    description: "Loading overview...",
    totalValue: p.totalValue ?? 0,
    gainLoss: 0,
    gainLossPercent: 0,
    currency: p.currency ?? "USD",
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
 