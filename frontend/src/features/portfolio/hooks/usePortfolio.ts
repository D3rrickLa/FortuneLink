import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { getPortfolio, updatePortfolio } from "@/features/portfolio/services";
import { UpdatePortfolioRequest } from "@/lib/api/types";

export function usePortfolio(portfolioId: string, options = {}) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["portfolio", portfolioId],
    queryFn: () => getPortfolio(portfolioId),
    ...options,
  });

  const updateMutation = useMutation({
    mutationFn: (data: UpdatePortfolioRequest) =>
      updatePortfolio(portfolioId, data),
    onSuccess: () => {
      // Invalidate both the list and the specific detail views so UI stays synchronized
      queryClient.invalidateQueries({ queryKey: ["portfolios"] });
      queryClient.invalidateQueries({ queryKey: ["portfolio", portfolioId] });
    },
  });

  return {
    ...query,
    updatePortfolio: updateMutation.mutate,
    isUpdating: updateMutation.isPending,
  };
}