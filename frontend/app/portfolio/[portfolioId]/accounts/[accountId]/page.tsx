'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { portfolioApi } from '@/lib/api/portfolio'
import { transactionApi } from '@/lib/api/transactions'
import type { AssetHolding, Transaction } from '@/types/portfolio'

export default function AccountPage() {
  const router = useRouter()
  const { portfolioId, accountId } = useParams<{
    portfolioId: string
    accountId: string
  }>()

  const [assets, setAssets] = useState<AssetHolding[]>([])
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const loadData = async () => {
      try {
        // Assets
        // (Assumes backend aggregates holdings per account)
        const account = await portfolioApi.getPortfolioById(portfolioId)
        const currentAccount = account.accounts.find(a => a.id === accountId)

        setAssets(currentAccount?.assets ?? [])

        // Transactions
        const txs = await transactionApi.getTransactionsForAccount(
          portfolioId,
          accountId
        )
        setTransactions(txs)
      } catch (err) {
        console.error(err)
      } finally {
        setLoading(false)
      }
    }

    loadData()
  }, [portfolioId, accountId])

  if (loading) {
    return <div className="p-6">Loading account…</div>
  }

  return (
    <div className="container mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold">Account Overview</h1>
        <button
          onClick={() =>
            router.push(
              `/portfolio/${portfolioId}/accounts/${accountId}/transactions/create`
            )
          }
          className="bg-blue-600 text-white px-4 py-2 rounded"
        >
          Add Transaction
        </button>
      </div>

      {/* ---------- Holdings ---------- */}
      <section className="mb-8">
        <h2 className="text-xl font-semibold mb-3">Holdings</h2>

        {assets.length === 0 ? (
          <p className="text-gray-500 italic">
            No holdings yet. Add a transaction to get started.
          </p>
        ) : (
          <ul className="space-y-2">
            {assets.map(asset => (
              <li key={asset.id} className="border rounded p-3">
                <p className="font-medium">{asset.symbol}</p>
                <p className="text-sm text-gray-500">
                  Quantity: {asset.quantity}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>

      {/* ---------- Transactions ---------- */}
      <section>
        <h2 className="text-xl font-semibold mb-3">Transactions</h2>

        {transactions.length === 0 ? (
          <p className="text-gray-500 italic">No transactions yet.</p>
        ) : (
          <ul className="space-y-2">
            {transactions.map(tx => (
              <li key={tx.id} className="border rounded p-3">
                <p className="font-medium">
                  {tx.transactionType} {tx.symbol}
                </p>
                <p className="text-sm text-gray-500">
                  {tx.quantity} @ {tx.price} {tx.priceCurrency}
                </p>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}