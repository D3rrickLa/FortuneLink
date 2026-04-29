import { Button } from "@/components/ui/button";
import { Dialog, DialogTrigger, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { SelectTrigger, SelectValue, SelectContent, SelectItem, Select } from "@/components/ui/select";
import { Plus } from "lucide-react";
import { useState } from "react";


interface AddTransactionDialogProps {
  onAddTransaction: (transaction: {
    type: 'buy' | 'sell' | 'deposit' | 'withdrawal';
    symbol?: string;
    shares?: number;
    price?: number;
    amount: number;
  }) => void;
}


export function AddTransactionDialog({ onAddTransaction }: AddTransactionDialogProps) {
  const [open, setOpen] = useState(false);
  const [type, setType] = useState<'buy' | 'sell' | 'deposit' | 'withdrawal'>('buy');
  const [symbol, setSymbol] = useState('');
  const [shares, setShares] = useState('');
  const [price, setPrice] = useState('');
  const [amount, setAmount] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    const transaction: {
      type: 'buy' | 'sell' | 'deposit' | 'withdrawal';
      symbol?: string;
      shares?: number;
      price?: number;
      amount: number;
    } = {
      type,
      amount: parseFloat(amount),
    };

    if (type === 'buy' || type === 'sell') {
      transaction.symbol = symbol.toUpperCase();
      transaction.shares = parseFloat(shares);
      transaction.price = parseFloat(price);
    }

    onAddTransaction(transaction);

    // Reset form
    setSymbol('');
    setShares('');
    setPrice('');
    setAmount('');
    setOpen(false);
  };

  const isStockTransaction = type === 'buy' || type === 'sell';

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          Add Transaction
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle>Add Transaction</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="type">Transaction Type</Label>
            <Select value={type} onValueChange={(value) => setType(value as any)}>
              <SelectTrigger id="type">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="buy">Buy Stock</SelectItem>
                <SelectItem value="sell">Sell Stock</SelectItem>
                <SelectItem value="deposit">Deposit Cash</SelectItem>
                <SelectItem value="withdrawal">Withdraw Cash</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {isStockTransaction && (
            <>
              <div className="space-y-2">
                <Label htmlFor="symbol">Stock Symbol</Label>
                <Input
                  id="symbol"
                  placeholder="e.g., AAPL"
                  value={symbol}
                  onChange={(e) => setSymbol(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="shares">Number of Shares</Label>
                <Input
                  id="shares"
                  type="number"
                  step="0.01"
                  placeholder="e.g., 10"
                  value={shares}
                  onChange={(e) => setShares(e.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="price">Price per Share</Label>
                <Input
                  id="price"
                  type="number"
                  step="0.01"
                  placeholder="e.g., 150.00"
                  value={price}
                  onChange={(e) => setPrice(e.target.value)}
                  required
                />
              </div>
            </>
          )}

          <div className="space-y-2">
            <Label htmlFor="amount">
              {isStockTransaction ? 'Total Amount' : 'Amount'}
            </Label>
            <Input
              id="amount"
              type="number"
              step="0.01"
              placeholder="e.g., 1500.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
            />
          </div>

          <Button type="submit" className="w-full">
            Add Transaction
          </Button>
        </form>
      </DialogContent>
    </Dialog>
  );
}