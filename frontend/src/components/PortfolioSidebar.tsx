
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Briefcase, Plus, TrendingUp, TrendingDown, LayoutGrid } from "lucide-react";
import { cn } from "@/lib/utils";
import { RadioGroup, RadioGroupItem } from "./ui/radio-group";
import { CreateAccountRequest, CreatePortfolioRequest } from "@/lib/api/types";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";

export interface Portfolio {
  id: string;
  name: string;
  totalValue: number;
  gainLoss: number;
  gainLossPercent: number;
}

export interface Account {
  id: string;
  name: string;
  totalValue: number;
  gainLoss: number;
  gainLossPercent: number;
}

interface PortfolioSidebarProps {
  portfolios: Portfolio[];
  activePortfolioId: string;
  activeAccountId?: string | null;
  onSelectPortfolio: (id: string) => void;
  onSelectAccount: (portfolioId: string, accountId: string) => void;
  onCreatePortfolio: (data: CreatePortfolioRequest) => void;
  onCreateAccount: (data: CreateAccountRequest) => void;
}

export function PortfolioSidebar({
  portfolios,
  activePortfolioId,
  activeAccountId,
  onSelectPortfolio,
  onSelectAccount,
  onCreatePortfolio,
  onCreateAccount,
}: PortfolioSidebarProps) {
  const [portfolioDialogOpen, setPortfolioDialogOpen] = useState(false);
  const [portfolioFormData, setPortfolioFormData] = useState<CreatePortfolioRequest>({
    name: '',
    description: '',
    currency: 'USD',
    createDefaultAccount: true,
    defaultAccountType: 'TAXABLE_INVESTMENT',
    defaultStrategy: 'ACB'
  });
  const [accountDialogOpen, setAccountDialogOpen] = useState(false);
  const [accountFormData, setAccountFormData] = useState<CreateAccountRequest>({
    accountName: '',
    accountType: 'CHEQUING',
    strategy: 'ACB',
    currency: 'USD'
  });
  const [selectedPortfolioForAccount, setSelectedPortfolioForAccount] = useState<string>('');
  const [expandedPortfolios, setExpandedPortfolios] = useState<Set<string>>(new Set(portfolios.map(p => p.id)));

  const handleCreatePortfolio = (e: React.SubmitEvent) => {
    e.preventDefault();
    if (portfolioFormData.name.trim()) {
      onCreatePortfolio(portfolioFormData); // Pass the whole object
      setPortfolioDialogOpen(false);
      // Reset form
      setPortfolioFormData({ ...portfolioFormData, name: '', description: '' });
    }
  };

  const handleCreateAccount = (e: React.SubmitEvent) => {
    e.preventDefault();
    if (accountFormData.accountName.trim() && selectedPortfolioForAccount) {
      onCreateAccount(accountFormData);
      setAccountDialogOpen(false);
      setAccountFormData({ ...accountFormData, accountName: '', accountType: 'CHEQUING', strategy: 'ACB', currency: '' });
    }
  };

  const togglePortfolioExpand = (portfolioId: string) => {
    const newExpanded = new Set(expandedPortfolios);
    if (newExpanded.has(portfolioId)) {
      newExpanded.delete(portfolioId);
    } else {
      newExpanded.add(portfolioId);
    }
    setExpandedPortfolios(newExpanded);
  };

  const openAccountDialog = (portfolioId: string) => {
    setSelectedPortfolioForAccount(portfolioId);
    setAccountDialogOpen(true);
  };

  // Calculate combined totals
  const combinedValue = portfolios.reduce((sum, p) => sum + p.totalValue, 0);
  const combinedGainLoss = portfolios.reduce((sum, p) => sum + p.gainLoss, 0);
  const combinedGainLossPercent = portfolios.reduce((sum, p) => sum + (p.gainLoss), 0) /
    portfolios.reduce((sum, p) => sum + (p.totalValue - p.gainLoss), 0) * 100;
  const isCombinedPositive = combinedGainLoss >= 0;

  return (
    <div className="flex h-full flex-col border-r bg-muted/10">
      <div className="border-b p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Briefcase className="h-5 w-5" />
            <h2 className="font-semibold">Portfolios</h2>
          </div>

          <Dialog open={portfolioDialogOpen} onOpenChange={setPortfolioDialogOpen}>
            <DialogTrigger asChild>
              <Button size="sm" variant="ghost">
                <Plus className="h-4 w-4" />
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-[425px]">
              <DialogHeader>
                <DialogTitle>Create New Portfolio</DialogTitle>
                <DialogDescription>Set up a new investment portfolio and default account.</DialogDescription>
              </DialogHeader>

              <form onSubmit={handleCreatePortfolio} className="space-y-4">
                {/* Portfolio Name */}
                <div className="space-y-2">
                  <Label htmlFor="name">Portfolio Name</Label>
                  <Input
                    id="name"
                    value={portfolioFormData.name}
                    onChange={(e) => setPortfolioFormData({ ...portfolioFormData, name: e.target.value })}
                    placeholder="e.g., Retirement"
                    required
                  />
                </div>

                {/* Account Type Selection */}
                <div className="space-y-2">
                  <Label htmlFor="account-type">Default Account Type</Label>
                  <Select
                    value={portfolioFormData.defaultAccountType}
                    onValueChange={(val: any) => setPortfolioFormData({ ...portfolioFormData, defaultAccountType: val })}
                  >
                    <SelectTrigger id="account-type">
                      <SelectValue placeholder="Select type" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="TAXABLE_INVESTMENT">Taxable Investment</SelectItem>
                      <SelectItem value="TFSA">TFSA</SelectItem>
                      <SelectItem value="RRSP">RRSP</SelectItem>
                      <SelectItem value="MARGIN">Margin</SelectItem>
                      <SelectItem value="ROTH_IRA">Roth IRA</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {/* Strategy Radio Group */}
                <div className="space-y-2">
                  <Label>Position Strategy</Label>
                  <RadioGroup
                    value={portfolioFormData.defaultStrategy}
                    onValueChange={(val: any) => setPortfolioFormData({ ...portfolioFormData, defaultStrategy: val })}
                    className="flex flex-col gap-2"
                  >
                    <div className="flex items-center gap-3">
                      <RadioGroupItem value="ACB" id="acb" />
                      <Label htmlFor="acb" className="font-normal">Adjusted Cost Basis (ACB)</Label>
                    </div>
                    <div className="flex items-center gap-3">
                      <RadioGroupItem value="FIFO" id="fifo" />
                      <Label htmlFor="fifo" className="font-normal">First-In, First-Out (FIFO)</Label>
                    </div>
                  </RadioGroup>
                </div>

                <Button type="submit" className="w-full">
                  Create Portfolio
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-2">
        <div className="space-y-1">
          {/* All Portfolios Overview */}
          <button
            onClick={() => onSelectPortfolio('all')}
            className={cn(
              "w-full rounded-lg border-2 border-dashed p-3 text-left transition-colors hover:bg-muted",
              activePortfolioId === 'all' && "border-blue-600 bg-muted"
            )}
          >
            <div className="space-y-1">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <LayoutGrid className="h-4 w-4" />
                  <span className="font-medium">All Portfolios</span>
                </div>
                {activePortfolioId === 'all' && (
                  <div className="h-2 w-2 rounded-full bg-blue-600" />
                )}
              </div>
              <div className="text-sm text-muted-foreground">
                ${combinedValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
              </div>
              <div className={cn(
                "flex items-center gap-1 text-xs",
                isCombinedPositive ? "text-green-600" : "text-red-600"
              )}>
                {isCombinedPositive ? (
                  <TrendingUp className="h-3 w-3" />
                ) : (
                  <TrendingDown className="h-3 w-3" />
                )}
                <span>
                  {isCombinedPositive ? '+' : ''}${Math.abs(combinedGainLoss).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
                <span className="text-muted-foreground">
                  ({isCombinedPositive ? '+' : ''}{combinedGainLossPercent.toFixed(2)}%)
                </span>
              </div>
            </div>
          </button>

          <div className="my-2 border-t" />

          {portfolios.map((portfolio) => {
            const isActive = portfolio.id === activePortfolioId;
            const isPositive = portfolio.gainLoss >= 0;

            return (
              <button
                key={portfolio.id}
                onClick={() => onSelectPortfolio(portfolio.id)}
                className={cn(
                  "w-full rounded-lg p-3 text-left transition-colors hover:bg-muted",
                  isActive && "bg-muted"
                )}
              >
                <div className="space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-medium">{portfolio.name}</span>
                    {isActive && (
                      <div className="h-2 w-2 rounded-full bg-blue-600" />
                    )}
                  </div>
                  <div className="text-sm text-muted-foreground">
                    ${portfolio.totalValue.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </div>
                  <div className={cn(
                    "flex items-center gap-1 text-xs",
                    isPositive ? "text-green-600" : "text-red-600"
                  )}>
                    {isPositive ? (
                      <TrendingUp className="h-3 w-3" />
                    ) : (
                      <TrendingDown className="h-3 w-3" />
                    )}
                    <span>
                      {isPositive ? '+' : ''}${Math.abs(portfolio.gainLoss).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </span>
                    <span className="text-muted-foreground">
                      ({isPositive ? '+' : ''}{portfolio.gainLossPercent.toFixed(2)}%)
                    </span>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      <div className="border-t p-4">
        <div className="space-y-1 text-sm">
          <div className="flex justify-between text-muted-foreground">
            <span>Total Accounts:</span>
            <span className="font-medium text-foreground">{portfolios.length}</span>
          </div>
          <div className="flex justify-between text-muted-foreground">
            <span>Combined Value:</span>
            <span className="font-medium text-foreground">
              ${portfolios.reduce((sum, p) => sum + p.totalValue, 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}