'use client'

import { useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { portfolioApi } from '@/lib/api/portfolio'

export default function CreateAccountPage() {
  const router = useRouter()
  const { portfolioId } = useParams<{ portfolioId: string }>()

  const [name, setName] = useState('')
  const [accountType, setAccountType] = useState('TFSA')
  const [baseCurrency, setBaseCurrency] = useState('USD')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      await portfolioApi.addAccount(portfolioId, { 
        name,
        accountType,
        baseCurrency,
      })

      router.push('/portfolio')
      router.refresh()
    } catch (err) {
      console.error(err)
      setError('Failed to create account.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-lg mx-auto p-6">
      <h1 className="text-2xl font-bold mb-4">Create Account</h1>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium">Account Name</label>
          <input
            className="w-full border rounded px-3 py-2"
            value={name}
            onChange={e => setName(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium">Account Type</label>
          <select
            className="w-full border rounded px-3 py-2"
            value={accountType}
            onChange={e => setAccountType(e.target.value)}
          >
            <option value="TFSA">TFSA</option>
            <option value="RRSP">RRSP</option>
            <option value="NON_REGISTERED">Non-Registered</option>
            <option value="INVESTMENT">Investment</option>
          </select>
        </div>

        <div>
          <label className="block text-sm font-medium">Base Currency</label>
          <select
            className="w-full border rounded px-3 py-2"
            value={baseCurrency}
            onChange={e => setBaseCurrency(e.target.value)}
          >
            <option value="USD">USD</option>
            <option value="CAD">CAD</option>
            <option value="EUR">EUR</option>
          </select>
        </div>

        {error && <p className="text-red-500 text-sm">{error}</p>}

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-blue-600 text-white py-2 rounded"
        >
          {loading ? 'Creating…' : 'Create Account'}
        </button>
      </form>
    </div>
  )
}