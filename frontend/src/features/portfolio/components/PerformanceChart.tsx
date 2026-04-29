import { useState, useMemo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { TrendingUp, TrendingDown } from "lucide-react";

export interface PerformanceData {
  date: string;
  value: number;
}

interface PerformanceChartProps {
  data?: PerformanceData[];
  portfolioId?: string;
}

type TimePeriod = 'all' | 'ytd' | '3m' | '1m';

const defaultPerformanceData: PerformanceData[] = [
  { date: '01/01', value: 100000 },
  { date: '02/01', value: 102500 },
  { date: '03/01', value: 104500 },
  { date: '04/01', value: 107000 },
  { date: '05/01', value: 109500 },
];

export function PerformanceChart({ data = [] }: PerformanceChartProps) {
  const [selectedPeriod, setSelectedPeriod] = useState<TimePeriod>('all');

  const chartData = data.length > 0 ? data : defaultPerformanceData;

  // Filter data based on selected time period
  const filteredData = useMemo(() => {
    if (chartData.length === 0) return [];

    const now = new Date('2026-02-01'); // Using latest date from mock data
    let cutoffDate: Date;

    switch (selectedPeriod) {
      case '1m':
        cutoffDate = new Date(now);
        cutoffDate.setMonth(cutoffDate.getMonth() - 1);
        break;
      case '3m':
        cutoffDate = new Date(now);
        cutoffDate.setMonth(cutoffDate.getMonth() - 3);
        break;
      case 'ytd':
        cutoffDate = new Date(now.getFullYear(), 0, 1); // January 1st of current year
        break;
      case 'all':
      default:
        return chartData;
    }

    return chartData.filter(item => {
      const [month, day] = item.date.split('/').map(Number);
      const itemDate = new Date(now.getFullYear(), month - 1, day);
      return itemDate >= cutoffDate;
    });
  }, [chartData, selectedPeriod]);

  // Calculate performance metrics
  const metrics = useMemo(() => {
    if (filteredData.length === 0) {
      return { valueChange: 0, percentChange: 0, isPositive: true };
    }

    const startValue = filteredData[0].value;
    const endValue = filteredData[filteredData.length - 1].value;
    const valueChange = endValue - startValue;
    const percentChange = startValue > 0 ? (valueChange / startValue) * 100 : 0;

    return {
      valueChange,
      percentChange,
      isPositive: valueChange >= 0,
    };
  }, [filteredData]);

  const periodButtons: { value: TimePeriod; label: string }[] = [
    { value: 'all', label: 'All Time' },
    { value: 'ytd', label: 'YTD' },
    { value: '3m', label: '3M' },
    { value: '1m', label: '1M' },
  ];

  return (
    <Card>
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <CardTitle>Portfolio Performance</CardTitle>
            <div className="flex items-center gap-3 mt-2">
              <div className={`flex items-center gap-1 ${metrics.isPositive ? 'text-green-600' : 'text-red-600'}`}>
                {metrics.isPositive ? (
                  <TrendingUp className="h-4 w-4" />
                ) : (
                  <TrendingDown className="h-4 w-4" />
                )}
                <span className="font-semibold">
                  {metrics.isPositive ? '+' : ''}${metrics.valueChange.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
              </div>
              <span className={`font-semibold ${metrics.isPositive ? 'text-green-600' : 'text-red-600'}`}>
                ({metrics.isPositive ? '+' : ''}{metrics.percentChange.toFixed(2)}%)
              </span>
            </div>
          </div>
          <div className="flex gap-1">
            {periodButtons.map((button) => (
              <button
                key={button.value}
                onClick={() => setSelectedPeriod(button.value)}
                className={`px-3 py-1 text-sm font-medium rounded-md transition-colors ${selectedPeriod === button.value
                  ? 'bg-primary text-primary-foreground'
                  : 'bg-secondary text-secondary-foreground hover:bg-secondary/80'
                  }`}
              >
                {button.label}
              </button>
            ))}
          </div>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-[300px]">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={filteredData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis
                dataKey="date"
                tick={{ fontSize: 12 }}
              />
              <YAxis
                tick={{ fontSize: 12 }}
                tickFormatter={(value) => `$${value.toLocaleString()}`}
              />
              <Tooltip
                formatter={(value) => [`$${Number(value ?? 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`, 'Value']}
              />
              <Line
                type="monotone"
                dataKey="value"
                stroke={metrics.isPositive ? "#10b981" : "#ef4444"}
                strokeWidth={2}
                dot={{ fill: metrics.isPositive ? "#10b981" : "#ef4444", r: 3 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
