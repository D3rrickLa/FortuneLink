import { useQuery } from "@tanstack/react-query";
import { searchSymbols } from "@/features/portfolio/services";

export function useSymbolSearch(query: string) {
  return useQuery({
    queryKey: ["symbols", "search", query],
    queryFn: () => searchSymbols(query),
    enabled: query.length > 1, // Only search if user typed 2+ chars
    staleTime: 1000 * 60 * 5, // Cache results for 5 minutes
  });
}