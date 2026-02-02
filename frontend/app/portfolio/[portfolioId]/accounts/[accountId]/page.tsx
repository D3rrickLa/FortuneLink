'use client'

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { portfolioApi } from '@/lib/api/portfolio'
import { transactionApi } from '@/lib/api/transactions'
import type { Account, AssetHolding, Transaction } from '@/types/portfolio'

export default function AccountPage() {
    const router = useRouter()
    const { portfolioId, accountId } = useParams<{
        portfolioId: string
        accountId: string
    }>()

    const [account, setAccount] = useState<Account | null>(null)
    const [assets, setAssets] = useState<AssetHolding[]>([])
    const [transactions, setTransactions] = useState<Transaction[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        const loadData = async () => {
            if (!portfolioId || !accountId) return; // Guard against undefined params

            try {
                setLoading(true);
                setError(null);

                const [acc, txs] = await Promise.all([
                    portfolioApi.getAccount(portfolioId, accountId),
                    transactionApi.getTransactionsForAccount(portfolioId, accountId)
                ]);

                setAccount(acc);
                setAssets(acc?.assets ?? []);
                
                // Ensure txs is handled correctly even if the API returns a single object instead of array
                const txArray = Array.isArray(txs) ? txs : txs ? [txs] : [];
                
                // Sort by date (newest first) to ensure they show up in a logical order
                const sortedTxs = [...txArray].sort((a, b) => 
                    new Date(b.transactionDate).getTime() - new Date(a.transactionDate).getTime()
                );
                
                setTransactions(sortedTxs);

            } catch (err: any) {
                console.error("Fetch Error:", err);
                setError('Failed to load account data');
            } finally {
                setLoading(false);
            }
        }

        loadData()
    }, [portfolioId, accountId])

    if (loading) return <div className="p-6">Loading account…</div>
    if (error) return <div className="p-6 text-red-500">{error}</div>

    return (
        <div className="container mx-auto p-6">
            {/* Header */}
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-2xl font-bold">{account?.name || 'Account Overview'}</h1>
                    <p className="text-gray-500">ID: {accountId}</p>
                </div>
                <button
                    onClick={() =>
                        router.push(`/portfolio/${portfolioId}/accounts/${accountId}/transactions/create`)
                    }
                    className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-500 transition-colors"
                >
                    Add Transaction
                </button>
            </div>

            {/* Holdings */}
            <section className="mb-8">
                <h2 className="text-xl font-semibold mb-3">Holdings</h2>
                {assets.length === 0 ? (
                    <p className="text-gray-500 italic border p-4 rounded bg-gray-50">
                        No holdings yet. Add a transaction to get started.
                    </p>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {assets.map(asset => (
                            <div key={asset.id} className="border rounded-lg p-4 shadow-sm">
                                <p className="font-bold text-lg">{asset.symbol}</p>
                                <div className="mt-2 space-y-1">
                                    <p className="text-sm text-gray-600">Quantity: <span className="font-medium text-black">{asset.quantity}</span></p>
                                    <p className="text-sm text-gray-600">
                                        Cost Basis: <span className="font-medium text-black">{asset.costBasis.amount.toLocaleString()} {asset.costBasis.currency}</span>
                                    </p>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </section>

            {/* Transactions */}
            <section>
                <h2 className="text-xl font-semibold mb-3">Transactions</h2>
                {transactions.length === 0 ? (
                    <p className="text-gray-500 italic border p-4 rounded bg-gray-50">No transactions found for this account.</p>
                ) : (
                    <ul className="space-y-3">
                        {transactions.map((tx, index) => (
                            <li key={tx.id || `${tx.assetId}-${index}`} className="border rounded-lg p-4 hover:bg-gray-50 transition-colors">
                                <div className="flex justify-between items-start">
                                    <div>
                                        <span className={`inline-block px-2 py-0.5 rounded text-xs font-bold mr-2 ${
                                            tx.transactionType === 'BUY' ? 'bg-green-100 text-green-700' : 
                                            tx.transactionType === 'DEPOSIT' ? 'bg-blue-100 text-blue-700' : 'bg-red-100 text-red-700'
                                        }`}>
                                            {tx.transactionType}
                                        </span>
                                        <span className="font-bold">{tx.symbol || 'Cash'}</span>
                                        <p className="text-sm text-gray-500 mt-1">
                                            {tx.quantity} units @ {tx.price?.amount?.toLocaleString()} {tx.price?.currency}
                                        </p>
                                    </div>
                                    <div className="text-right">
                                        <p className="text-xs text-gray-400">
                                            {tx.transactionDate ? new Date(tx.transactionDate).toLocaleDateString() : 'No Date'}
                                        </p>
                                        {tx.fee && (
                                            <p className="text-xs text-gray-400 mt-1">
                                                Fee: {tx.fee.amount.toLocaleString()} {tx.fee.currency}
                                            </p>
                                        )}
                                    </div>
                                </div>
                                {tx.notes && (
                                    <p className="text-sm text-gray-500 mt-2 italic border-t pt-2">
                                        Note: {tx.notes}
                                    </p>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </section>
        </div>
    )
}