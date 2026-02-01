// app/portfolio/page.tsx
'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { portfolioApi } from '@/lib/api/portfolio'

export interface PortfolioSummary {
  id: string
  name: string | null
  description?: string | null
  totalValue: number
  currency: string
  numberOfAccounts?: number
  createdDate: string
  lastUpdated: string
}

export default function PortfolioPage() {
  const router = useRouter()
  const [portfolios, setPortfolios] = useState<PortfolioSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const loadPortfolios = async () => {
      try {
        // Directly use backend response, no mapping needed
        const data: PortfolioSummary[] = await portfolioApi.getMyPortfolios()
        setPortfolios(data)
      } catch (err: any) {
        console.error(err)
        setError('Failed to load portfolios')
      } finally {
        setLoading(false)
      }
    }

    loadPortfolios()
  }, [])

  if (loading) return <div className="p-10 text-center">Loading portfolios…</div>

  if (error)
    return (
      <div className="p-10 text-center text-red-500">
        <p>{error}</p>
        <button
          onClick={() => window.location.reload()}
          className="mt-4 underline"
        >
          Retry
        </button>
      </div>
    )

  return (
    <div className="container mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">My Portfolios</h1>
        <button
          onClick={() => router.push('/portfolio/create')}
          className="bg-blue-600 hover:bg-blue-500 px-4 py-2 rounded text-white"
        >
          Create Portfolio
        </button>
      </div>

      {portfolios.length === 0 ? (
        <p className="text-gray-500 italic">
          You haven’t created a portfolio yet.
        </p>
      ) : (
        <ul className="space-y-3">
          {portfolios.map((portfolio) => (
            <li
              key={portfolio.id}
              className="border rounded p-4 hover:bg-gray-50 cursor-pointer"
              onClick={() => router.push(`/portfolio/${portfolio.id}`)}
            >
              <p className="font-medium">
                {portfolio.name ?? 'Untitled Portfolio'}
              </p>
              {portfolio.description && (
                <p className="text-sm text-gray-500">{portfolio.description}</p>
              )}
              <p className="text-sm text-gray-500">
                Net Worth: {portfolio.totalValue.toLocaleString()} {portfolio.currency} •{' '}
                {portfolio.numberOfAccounts ?? 0} accounts
              </p>
              <p className="text-xs text-gray-400">
                Last updated: {new Date(portfolio.lastUpdated).toLocaleDateString()}
              </p>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}