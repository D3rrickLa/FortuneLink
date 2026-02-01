// app/portfolio/[portfolioId]/page.tsx
'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { portfolioApi } from '@/lib/api/portfolio'
import type { Account } from '@/types/portfolio'

export default function PortfolioDetailPage() {
  const router = useRouter()
  const { portfolioId } = useParams<{ portfolioId: string }>()
  const [accounts, setAccounts] = useState<Account[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const loadAccounts = async () => {
      try {
        const data = await portfolioApi.listAccounts(portfolioId)
        setAccounts(data)
      } catch (err: any) {
        console.error(err)
        setError('Failed to load accounts')
      } finally {
        setLoading(false)
      }
    }
    loadAccounts()
  }, [portfolioId])

  if (loading) return <div className="p-6">Loading accounts…</div>
  if (error) return <div className="p-6 text-red-500">{error}</div>

  return (
    <div className="container mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Accounts</h1>
        <button
          className="bg-blue-600 text-white px-4 py-2 rounded"
          onClick={() => router.push(`/portfolio/${portfolioId}/accounts/create`)}
        >
          Add Account
        </button>
      </div>

      {accounts.length === 0 ? (
        <p className="text-gray-500 italic">No accounts yet. Add one to get started.</p>
      ) : (
        <ul className="space-y-3">
          {accounts.map(account => (
            <li
              key={account.id}
              className="border rounded p-4 hover:bg-gray-50 cursor-pointer"
              onClick={() =>
                router.push(`/portfolio/${portfolioId}/accounts/${account.id}`)
              }
            >
              <p className="font-medium">{account.name}</p>
              <p className="text-sm text-gray-500">
                {account.accountType} · {account.baseCurrency}
              </p>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}