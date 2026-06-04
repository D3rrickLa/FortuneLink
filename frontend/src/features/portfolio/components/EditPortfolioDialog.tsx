import { Button } from "@/components/ui/button";
import { DialogTrigger, DialogContent, DialogHeader, DialogTitle, DialogFooter, Dialog } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { PencilIcon } from "lucide-react";
import { useState, useEffect } from "react";
import { usePortfolio } from "../hooks/usePortfolio";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";

interface EditPortfolioDialogProps {
  portfolioId: string,
  initialData: {
    name: string,
    description: string,
    currency: string
  };
}

const CURRENCIES = ["USD", "CAD", "EUR", "GBP"];

export function EditPortfolioDialog({ portfolioId, initialData }: EditPortfolioDialogProps) {
  const [open, setOpen] = useState(false);
  const [name, setName] = useState(initialData.name);
  const [description, setDescription] = useState(initialData.description ?? "");
  const [currency, setCurrency] = useState(initialData.currency ?? "CAD");

  const { updatePortfolio, isUpdating } = usePortfolio(portfolioId);

  // Keep state in sync if initial data updates in the background
  useEffect(() => {
    setName(initialData.name);
    setDescription(initialData.description ?? "");
  }, [initialData]);

  const handleSubmit = (e: React.SubmitEvent) => {
    e.preventDefault();
    updatePortfolio(
      { name, description, currency },
      {
        onSuccess: () => setOpen(false),
      }
    );
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="ghost" size="icon" className="h-6 w-6">
          <PencilIcon className="h-4 w-4 text-muted-foreground" />
        </Button>
      </DialogTrigger>
      <DialogContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Edit Portfolio</DialogTitle>
          </DialogHeader>

          <div className="space-y-2">
            <Label className="text-sm font-medium">Name</Label>
            <Input value={name} onChange={(e) => setName(e.target.value)} required />
          </div>

          <div className="space-y-1.5">
            <Label>Display Currency</Label>
            <Select value={currency} onValueChange={setCurrency}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {CURRENCIES.map((c) => (
                  <SelectItem key={c} value={c}>
                    {c}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label className="text-sm font-medium">Description</Label>
            <Textarea value={description} onChange={(e) => setDescription(e.target.value)} />
          </div>

          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={isUpdating}>
              {isUpdating ? "Saving..." : "Save Changes"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}