import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Briefcase, Plus, TrendingUp, TrendingDown, LayoutGrid, ChevronRight, ChevronDown, Building2 } from "lucide-react";
import { cn } from "@/lib/utils";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { CreateAccountRequest, CreatePortfolioRequest } from "@/lib/api/types";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

export interface Account {
  id: string;
  name: string;
  totalValue: number;
  gainLoss: number;
  gainLossPercent: number;
}

export interface Portfolio {
  id: string;
  name: string;
  totalValue: number;
  gainLoss: number;
  gainLossPercent: number;
  accounts: Account[];
}

interface PortfolioSidebarProps {
  portfolios: Portfolio[];
  activePortfolioId: string;
  activeAccountId?: string | null;
  onSelectPortfolio: (id: string) => void;
  onSelectAccount: (portfolioId: string, accountId: string) => void;
  onCreatePortfolio: (data: CreatePortfolioRequest) => void;
  onCreateAccount: (portfolioId: string, data: CreateAccountRequest) => void;
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
  const [accountDialogOpen, setAccountDialogOpen] = useState(false);
  const [portfolioFormData, setPortfolioFormData] = useState<CreatePortfolioRequest>({name: '', description: '', currency: 'USD', createDefaultAccount: true, defaultAccountType: 'TAXABLE_INVESTMENT', defaultStrategy: 'ACB'});
  const [accountFormData, setAccountFormData] = useState<CreateAccountRequest>({accountName: '', accountType: 'CHEQUING', strategy: 'ACB', currency: 'USD' });
  const [selectedPortfolioForAccount, setSelectedPortfolioForAccount] = useState<string>('');
  const [expandedPortfolios, setExpandedPortfolios] = useState<Set<string>>(new Set(portfolios.map(p => p.id)));

  const handleCreatePortfolio = (e: React.FormEvent) => {
    e.preventDefault();
    if (portfolioFormData.name.trim()) {
      onCreatePortfolio(portfolioFormData);
      setPortfolioDialogOpen(false);
      setPortfolioFormData({ ...portfolioFormData, name: '', description: '' });
    }
  };

  const handleCreateAccount = (e: React.FormEvent) => {
    e.preventDefault();
    if (accountFormData.accountName.trim() && selectedPortfolioForAccount) {
      onCreateAccount(selectedPortfolioForAccount, accountFormData);
      setAccountDialogOpen(false);
      setAccountFormData({ ...accountFormData, accountName: '', accountType: 'CHEQUING' });
    }
  };

  const togglePortfolioExpand = (portfolioId: string) => {
    const newExpanded = new Set(expandedPortfolios);
    newExpanded.has(portfolioId) ? newExpanded.delete(portfolioId) : newExpanded.add(portfolioId);
    setExpandedPortfolios(newExpanded);
  };

  const openAccountDialog = (portfolioId: string) => {
    setSelectedPortfolioForAccount(portfolioId);
    setAccountDialogOpen(true);
  };

  // Totals Calculation
  const combinedValue = portfolios.reduce((sum, p) => sum + p.totalValue, 0);
  const combinedGainLoss = portfolios.reduce((sum, p) => sum + p.gainLoss, 0);
  const totalCostBasis = portfolios.reduce((sum, p) => sum + (p.totalValue - p.gainLoss), 0);
  const combinedGainLossPercent = totalCostBasis > 0 ? (combinedGainLoss / totalCostBasis) * 100 : 0;
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
              <Button size="sm" variant="ghost"><Plus className="h-4 w-4" /></Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>Create Portfolio</DialogTitle>
                <DialogDescription>Set up a new portfolio and default account.</DialogDescription>
              </DialogHeader>
              <form onSubmit={handleCreatePortfolio} className="space-y-4">
                <div className="space-y-2">
                  <Label>Portfolio Name</Label>
                  <Input
                    value={portfolioFormData.name}
                    onChange={e => setPortfolioFormData({ ...portfolioFormData, name: e.target.value })}
                    placeholder="e.g. Retirement"
                    required
                  />
                </div>
                <div className="space-y-2">
                  <Label>Default Account Type</Label>
                  <Select
                    value={portfolioFormData.defaultAccountType}
                    onValueChange={(val: any) => setPortfolioFormData({ ...portfolioFormData, defaultAccountType: val })}
                  >
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="TAXABLE_INVESTMENT">Taxable Investment</SelectItem>
                      <SelectItem value="TFSA">TFSA</SelectItem>
                      <SelectItem value="RRSP">RRSP</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <Button type="submit" className="w-full">Create Portfolio</Button>
              </form>
            </DialogContent>
          </Dialog>

          <Dialog open={accountDialogOpen} onOpenChange={setAccountDialogOpen}>
            <DialogContent className="sm:max-w-[425px]">
              <DialogHeader>
                <DialogTitle>Create New Account</DialogTitle>
              </DialogHeader>
              <form onSubmit={handleCreateAccount} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="account-name">Account Name</Label>
                  <Input
                    id="account-name"
                    placeholder="e.g., Roth IRA"
                    value={setAccountFormData.name}
                    onChange={(e) => setAccountFormData({...accountFormData, accountName: e.target.value})}
                    required
                  />
                </div>
                <Button type="submit" className="w-full">
                  Create Account
                </Button>
              </form>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      <div className="flex-1 overflow-auto p-2">
        <div className="space-y-1">
          {/* All Portfolios Summary Card */}
          <button
            onClick={() => onSelectPortfolio('all')}
            className={cn(
              "w-full rounded-lg border-2 border-dashed p-3 text-left transition-colors hover:bg-muted mb-2",
              activePortfolioId === 'all' && "border-blue-600 bg-muted"
            )}
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <LayoutGrid className="h-4 w-4" />
                <span className="font-medium text-sm">All Portfolios</span>
              </div>
            </div>
            <div className="mt-1 text-sm font-semibold">
              ${combinedValue.toLocaleString('en-US', { minimumFractionDigits: 2 })}
            </div>
            <div className={cn("text-xs flex items-center gap-1", isCombinedPositive ? "text-green-600" : "text-red-600")}>
              {isCombinedPositive ? <TrendingUp className="h-3 w-3" /> : <TrendingDown className="h-3 w-3" />}
              {combinedGainLossPercent.toFixed(2)}%
            </div>
          </button>

          {portfolios.map((portfolio) => {
            const isExpanded = expandedPortfolios.has(portfolio.id);
            const isPortfolioActive = activePortfolioId === portfolio.id && !activeAccountId;

            return (
              <div key={portfolio.id} className="group">
                <div className={cn("flex items-center rounded-md px-2 py-1.5 hover:bg-muted/50", isPortfolioActive && "bg-muted")}>
                  <button onClick={() => togglePortfolioExpand(portfolio.id)} className="p-1">
                    {isExpanded ? <ChevronDown className="h-3.5 w-3.5" /> : <ChevronRight className="h-3.5 w-3.5" />}
                  </button>
                  <div
                    className="flex-1 cursor-pointer"
                    onClick={() => onSelectPortfolio(portfolio.id)}
                  >
                    <div className="flex justify-between items-center px-1">
                      <span className="text-sm font-medium">{portfolio.name}</span>
                      <span className="text-xs text-muted-foreground">${(portfolio.totalValue / 1000).toFixed(1)}k</span>
                    </div>
                  </div>
                  <Button variant="ghost" size="sm" className="h-6 w-6 p-0 opacity-0 group-hover:opacity-100" onClick={() => openAccountDialog(portfolio.id)}>
                    <Plus className="h-3 w-3" />
                  </Button>
                </div>

                {isExpanded && (
                  <div className="ml-6 mt-1 space-y-1 border-l-2 border-muted pl-2">
                    {portfolio.accounts.map((account) => (
                      <button
                        key={account.id}
                        onClick={() => onSelectAccount(portfolio.id, account.id)}
                        className={cn(
                          "flex w-full items-center gap-2 rounded-md px-2 py-1.5 text-left text-xs transition-colors hover:bg-muted",
                          activeAccountId === account.id && "bg-muted font-medium text-blue-600"
                        )}
                      >
                        <Building2 className="h-3 w-3 opacity-70" />
                        <span className="flex-1 truncate">{account.name}</span>
                        <span className="text-muted-foreground">${(account.totalValue / 1000).toFixed(1)}k</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Reusable Account Creation Dialog */}
      <Dialog open={accountDialogOpen} onOpenChange={setAccountDialogOpen}>
        <DialogContent>
          <DialogHeader><DialogTitle>Add Account to Portfolio</DialogTitle></DialogHeader>
          <form onSubmit={handleCreateAccount} className="space-y-4">
            <div className="space-y-2">
              <Label>Account Name</Label>
              <Input
                value={accountFormData.accountName}
                onChange={e => setAccountFormData({ ...accountFormData, accountName: e.target.value })}
                placeholder="e.g. Wealthsimple Trade" required
              />
            </div>
            <Button type="submit" className="w-full">Add Account</Button>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}