import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { ArrowUpCircle, ArrowDownCircle, TrendingUp, TrendingDown } from "lucide-react";

export interface Transaction {
  id: string;
  date: string;
  type: 'buy' | 'sell' | 'deposit' | 'withdrawal';
  symbol?: string;
  shares?: number;
  price?: number;
  amount: number;
}

interface TransactionHistoryProps {
  transactions?: Transaction[];
  portfolioId?: string;
}

export function TransactionHistory({ transactions = [] }: TransactionHistoryProps) {
  const getTypeIcon = (type: Transaction['type']) => {
    switch (type) {
      case 'buy':
        return <TrendingUp className="h-4 w-4 text-blue-600" />;
      case 'sell':
        return <TrendingDown className="h-4 w-4 text-orange-600" />;
      case 'deposit':
        return <ArrowDownCircle className="h-4 w-4 text-green-600" />;
      case 'withdrawal':
        return <ArrowUpCircle className="h-4 w-4 text-red-600" />;
    }
  };

  const getTypeBadgeVariant = (type: Transaction['type']) => {
    switch (type) {
      case 'buy':
        return 'default';
      case 'sell':
        return 'secondary';
      case 'deposit':
        return 'outline';
      case 'withdrawal':
        return 'outline';
      default:
        return 'default';
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>Transaction History</CardTitle>
      </CardHeader>
      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Date</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Symbol</TableHead>
              <TableHead className="text-right">Shares</TableHead>
              <TableHead className="text-right">Price</TableHead>
              <TableHead className="text-right">Amount</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {transactions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} className="text-center text-muted-foreground">
                  No transactions yet
                </TableCell>
              </TableRow>
            ) : (
              transactions.map((transaction) => (
                <TableRow key={transaction.id}>
                  <TableCell>{transaction.date}</TableCell>
                  <TableCell>
                    <div className="flex items-center gap-2">
                      {getTypeIcon(transaction.type)}
                      <Badge variant={getTypeBadgeVariant(transaction.type)}>
                        {transaction.type.toUpperCase()}
                      </Badge>
                    </div>
                  </TableCell>
                  <TableCell>
                    {transaction.symbol || '—'}
                  </TableCell>
                  <TableCell className="text-right">
                    {transaction.shares || '—'}
                  </TableCell>
                  <TableCell className="text-right">
                    {transaction.price ? `$${transaction.price.toFixed(2)}` : '—'}
                  </TableCell>
                  <TableCell className="text-right font-medium">
                    ${transaction.amount.toFixed(2)}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  );
}
