import { useSymbolSearch } from "@/features/portfolio/hooks/useSymbolSearch";
import { useState } from "react";

export function SymbolSearchBar() {
  const [searchTerm, setSearchTerm] = useState("");
  const { data: results, isLoading } = useSymbolSearch(searchTerm);

  return (
    <div>
      <input 
        value={searchTerm} 
        onChange={(e) => setSearchTerm(e.target.value)} 
        placeholder="Search stocks..." 
      />
      {isLoading && <p>Searching...</p>}
      <ul>
        {results?.map((res) => (
          <li key={res.symbol}>{res.name} ({res.symbol})</li>
        ))}
      </ul>
    </div>
  );
}