'use client'

import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { transactionApi } from '@/lib/api/transactions'
import type { Transaction } from '@/types/portfolio'
import { CreateTransactionInput } from '@/lib/api/transaction.types'

// Define the type locally for better state management
type TransactionType = 'BUY' | 'SELL' | 'DEPOSIT'

export default function AddTransactionPage() {
    const router = useRouter()
    const { portfolioId, accountId } = useParams<{
        portfolioId: string
        accountId: string
    }>()

    // --- Form state ---
    const [transactionType, setTransactionType] = useState<TransactionType>('BUY')
    const [symbol, setSymbol] = useState('')         // Used for BUY and DEPOSIT
    const [assetId, setAssetId] = useState('')       // Used for SELL
    const [quantity, setQuantity] = useState<number>(0)
    const [price, setPrice] = useState<number>(0)
    const [fee, setFee] = useState<number>(0)
    const [notes, setNotes] = useState('')
    const [submitting, setSubmitting] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setSubmitting(true)
        setError(null)

        try {
            let newTx: CreateTransactionInput

            // Common properties for all transactions
            const commonFields = {
                quantity,
                price: { amount: price, currency: 'USD' },
                fee: fee > 0 ? { amount: fee, currency: 'USD' } : undefined,
                transactionDate: new Date().toISOString(),
                notes: notes || undefined,
            }

            if (transactionType === 'BUY' || transactionType === 'DEPOSIT') {
                newTx = {
                    ...commonFields,
                    transactionType,
                    symbol,
                }
            } else {
                newTx = {
                    ...commonFields,
                    transactionType: 'SELL',
                    assetId,
                }
            }

            await transactionApi.addTransaction(portfolioId, accountId, newTx)
            router.push(`/portfolio/${portfolioId}/accounts/${accountId}`)
        } catch (err) {
            console.error(err)
            setError('Failed to add transaction')
        } finally {
            setSubmitting(false)
        }
    }

    return (
        <div className="container mx-auto p-6 max-w-md">
            <h1 className="text-2xl font-bold mb-4">Add Transaction</h1>

            {error && <p className="text-red-500 mb-4">{error}</p>}

            <form onSubmit={handleSubmit} className="space-y-4">
                {/* Transaction Type */}
                <div>
                    <label className="block mb-1 font-medium">Transaction Type</label>
                    <select
                        value={transactionType}
                        onChange={e => setTransactionType(e.target.value as TransactionType)}
                        className="w-full border px-3 py-2 rounded bg-white"
                    >
                        <option value="BUY">BUY</option>
                        <option value="SELL">SELL</option>
                        <option value="DEPOSIT">DEPOSIT</option>
                    </select>
                </div>

                {/* Symbol (for BUY/DEPOSIT) or AssetId (for SELL) */}
                {transactionType !== 'SELL' ? (
                    <div>
                        <label className="block mb-1 font-medium">Symbol (e.g., BTC, AAPL)</label>
                        <input
                            type="text"
                            value={symbol}
                            onChange={e => setSymbol(e.target.value.toUpperCase())}
                            className="w-full border px-3 py-2 rounded"
                            required
                            placeholder="TSLA"
                        />
                    </div>
                ) : (
                    <div>
                        <label className="block mb-1 font-medium">Asset ID</label>
                        <input
                            type="text"
                            value={assetId}
                            onChange={e => setAssetId(e.target.value)}
                            className="w-full border px-3 py-2 rounded"
                            required
                            placeholder="ID from your portfolio"
                        />
                    </div>
                )}

                {/* Quantity */}
                <div>
                    <label className="block mb-1 font-medium">Quantity</label>
                    <input
                        type="number"
                        value={quantity || ''}
                        onChange={e => setQuantity(Number(e.target.value))}
                        className="w-full border px-3 py-2 rounded"
                        min={0}
                        step="any"
                        required
                    />
                </div>

                {/* Price */}
                <div>
                    <label className="block mb-1 font-medium">Price (per unit)</label>
                    <input
                        type="number"
                        value={price || ''}
                        onChange={e => setPrice(Number(e.target.value))}
                        className="w-full border px-3 py-2 rounded"
                        min={0}
                        step="any"
                        required
                    />
                </div>

                {/* Fee */}
                <div>
                    <label className="block mb-1 font-medium">Fee (optional)</label>
                    <input
                        type="number"
                        value={fee || ''}
                        onChange={e => setFee(Number(e.target.value))}
                        className="w-full border px-3 py-2 rounded"
                        min={0}
                        step="any"
                    />
                </div>

                {/* Notes */}
                <div>
                    <label className="block mb-1 font-medium">Notes</label>
                    <textarea
                        value={notes}
                        onChange={e => setNotes(e.target.value)}
                        className="w-full border px-3 py-2 rounded"
                        rows={3}
                        placeholder="Optional details..."
                    />
                </div>

                {/* Submit */}
                <button
                    type="submit"
                    disabled={submitting}
                    className={`w-full text-white px-4 py-2 rounded font-bold transition-colors ${
                        submitting ? 'bg-gray-400' : 'bg-blue-600 hover:bg-blue-500'
                    }`}
                >
                    {submitting ? 'Submitting...' : 'Add Transaction'}
                </button>
            </form>
        </div>
    )
}