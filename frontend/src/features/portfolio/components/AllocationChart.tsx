import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import { ValueType } from 'recharts/types/component/DefaultTooltipContent';

interface AllocationData {
  name: string;
  value: number;
}

interface AllocationChartProps {
  data?: AllocationData[];
  portfolioId?: string;
}

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316'];

export function AllocationChart({ data = [], portfolioId = 'all' }: AllocationChartProps) {

  // A robust formatter that won't make TypeScript angry
  const formatCurrency = (value: ValueType | undefined) => {
    const num = Number(value ?? 0);
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(num);
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>
          {portfolioId === 'all' ? 'Total Allocation' : 'Portfolio Allocation'}
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-[300px]">
          {/* Ensure there is data to render to avoid Recharts errors */}
          {data && data.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={data}
                  cx="50%"
                  cy="50%"
                  labelLine={false}
                  label={(props) => {
                    const name = typeof props.name === 'string' ? props.name : 'Unknown';
                    const percent = typeof props.percent === 'number' ? props.percent : 0;
                    return `${name} ${(percent * 100).toFixed(0)}%`;
                  }}
                  outerRadius={80}
                  dataKey="value"
                >
                  {data.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip formatter={formatCurrency} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex h-full items-center justify-center text-muted-foreground">
              No holdings found for this portfolio.
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}