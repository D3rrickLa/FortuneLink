import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { TrendingUp, TrendingDown } from "lucide-react";

export interface Stock {
  symbol: string;
  name: string;
  shares: number;
  avgPrice: number;
  currentPrice: number;
}

interface StockHoldingsProps {
  stocks?: Stock[];
  portfolioId?: string;
}

export function StockHoldings({ stocks = [] }: StockHoldingsProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>Stock Holdings</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Symbol</TableHead>
              <TableHead>Name</TableHead>
              <TableHead className="text-right">Shares</TableHead>
              <TableHead className="text-right">Avg Price</TableHead>
              <TableHead className="text-right">Current Price</TableHead>
              <TableHead className="text-right">Total Value</TableHead>
              <TableHead className="text-right">Gain/Loss</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {stocks.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} className="text-center text-muted-foreground">
                  No stocks in portfolio
                </TableCell>
              </TableRow>
            ) : (
              stocks.map((stock) => {
                const totalValue = stock.shares * stock.currentPrice;
                const totalCost = stock.shares * stock.avgPrice;
                const gainLoss = totalValue - totalCost;
                const gainLossPercent = (gainLoss / totalCost) * 100;
                const isPositive = gainLoss >= 0;

                return (
                  <TableRow key={stock.symbol}>
                    <TableCell className="font-medium">{stock.symbol}</TableCell>
                    <TableCell>{stock.name}</TableCell>
                    <TableCell className="text-right">{stock.shares}</TableCell>
                    <TableCell className="text-right">
                      ${stock.avgPrice.toFixed(2)}
                    </TableCell>
                    <TableCell className="text-right">
                      ${stock.currentPrice.toFixed(2)}
                    </TableCell>
                    <TableCell className="text-right">
                      ${totalValue.toFixed(2)}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className={`flex items-center justify-end gap-1 ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
                        {isPositive ? (
                          <TrendingUp className="h-3 w-3" />
                        ) : (
                          <TrendingDown className="h-3 w-3" />
                        )}
                        <span>
                          {isPositive ? '+' : ''}${gainLoss.toFixed(2)}
                        </span>
                        <span className="text-xs">
                          ({isPositive ? '+' : ''}{gainLossPercent.toFixed(2)}%)
                        </span>
                      </div>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
