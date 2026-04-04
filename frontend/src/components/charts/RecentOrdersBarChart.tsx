import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'

type RecentOrdersBarChartProps = {
  data: Array<{ id: string; amount: number }>
  formatCurrency: (value?: number) => string
}

export default function RecentOrdersBarChart({ data, formatCurrency }: RecentOrdersBarChartProps) {
  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={data}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e7e5e4" />
        <XAxis dataKey="id" tick={{ fontSize: 12, fill: '#57534e' }} />
        <YAxis tick={{ fontSize: 12, fill: '#57534e' }} tickFormatter={(v) => `${(v / 1000).toFixed(0)}k`} />
        <Tooltip formatter={(value) => formatCurrency(typeof value === 'number' ? value : undefined)} />
        <Bar dataKey="amount" fill="#0f766e" radius={[8, 8, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  )
}
