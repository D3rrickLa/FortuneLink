'use client'

import { useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { transactionApi } from '@/lib/api/transactions'
import type { Transaction } from '@/types/portfolio'

export default function AddTransactionPage() {
    const router = useRouter()
    const { portfolioId, accountId } = useParams<{
        portfolioId: string
        accountId: string
    }>()

    const [symbol, setSymbol] = useState('')
    const [transactionType, setTransactionType] = useState<'BUY' | 'SELL'>('BUY')
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
            const newTx: Omit<Transaction, 'id' | 'recordedAt'> = {
                symbol,
                transactionType,
                quantity,
                price: { amount: price, currency: 'USD' },
                fee: fee > 0 ? { amount: fee, currency: 'USD' } : undefined,
                notes: notes || undefined,
                accountId: '',
                transactionDate: ''
            }

            await transactionApi.addTransaction(portfolioId, accountId, newTx)

            router.push(`/portfolio/${portfolioId}/accounts/${accountId}`)
        } catch (err: any) {
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

                <div>
                    <label className="block mb-1 font-medium">Type</label>
                    <select
                        value={transactionType}
                        onChange={e => setTransactionType(e.target.value as 'BUY' | 'SELL')}
                        className="w-full border px-3 py-2 rounded"
                    >
                        <option value="BUY">BUY</option>
                        <option value="SELL">SELL</option>
                    </select>
                </div>

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

                <div>
                    <label className="block mb-1 font-medium">Notes</label>
                    <textarea
                        value={notes}
                        onChange={e => setNotes(e.target.value)}
                        className="w-full border px-3 py-2 rounded"
                        rows={3}
                    />
                </div>

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