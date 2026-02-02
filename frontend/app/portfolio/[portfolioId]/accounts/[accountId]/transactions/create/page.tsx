'use client'

import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { transactionApi } from '@/lib/api/transactions'
import type { Transaction } from '@/types/portfolio'
import { CreateTransactionInput } from '@/lib/api/transaction.types'

export default function AddTransactionPage() {
    const router = useRouter()
    const { portfolioId, accountId } = useParams<{
        portfolioId: string
        accountId: string
    }>()

    // --- Form state ---
    const [transactionType, setTransactionType] = useState<'BUY' | 'SELL'>('BUY')
    const [symbol, setSymbol] = useState('')         // BUY only
    const [assetId, setAssetId] = useState('')       // SELL only
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

            if (transactionType === 'BUY') {
                newTx = {
                    transactionType: 'BUY',
                    symbol,
                    quantity,
                    price: { amount: price, currency: 'USD' },
                    fee: fee > 0 ? { amount: fee, currency: 'USD' } : undefined,
                    transactionDate: new Date().toISOString(),
                    notes: notes || undefined,
                }
            } else {
                newTx = {
                    transactionType: 'SELL',
                    assetId,
                    quantity,
                    price: { amount: price, currency: 'USD' },
                    fee: fee > 0 ? { amount: fee, currency: 'USD' } : undefined,
                    transactionDate: new Date().toISOString(),
                    notes: notes || undefined,
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
                        onChange={e => setTransactionType(e.target.value as 'BUY' | 'SELL')}
                        className="w-full border px-3 py-2 rounded"
                    >
                        <option value="BUY">BUY</option>
                        <option value="SELL">SELL</option>
                    </select>
                </div>

                {/* Symbol / AssetId */}
                {transactionType === 'BUY' ? (
                    <div>
                        <label className="block mb-1 font-medium">Symbol</label>
                        <input
                            type="text"
                            value={symbol}
                            onChange={e => setSymbol(e.target.value.toUpperCase())}
                            className="w-full border px-3 py-2 rounded"
                            required
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
                        />
                    </div>
                )}

                {/* Quantity */}
                <div>
                    <label className="block mb-1 font-medium">Quantity</label>
                    <input
                        type="number"
                        value={quantity}
                        onChange={e => setQuantity(Number(e.target.value))}
                        className="w-full border px-3 py-2 rounded"
                        min={0}
                        step={0.0001}
                        required
                    />
                </div>

                {/* Price */}
                <div>
                    <label className="block mb-1 font-medium">Price</label>
                    <input
                        type="number"
                        value={price}
                        onChange={e => setPrice(Number(e.target.value))}
                        className="w-full border px-3 py-2 rounded"
                        min={0}
                        step={0.01}
                        required
                    />
                </div>

                {/* Fee */}
                <div>
                    <label className="block mb-1 font-medium">Fee (optional)</label>
                    <input
                        type="number"
                        value={fee}
                        onChange={e => setFee(Number(e.target.value))}
                        className="w-full border px-3 py-2 rounded"
                        min={0}
                        step={0.01}
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
                    />
                </div>

                {/* Submit */}
                <button
                    type="submit"
                    disabled={submitting}
                    className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-500"
                >
                    {submitting ? 'Submitting...' : 'Add Transaction'}
                </button>
            </form>
        </div>
    )
}