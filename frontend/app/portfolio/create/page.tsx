'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { portfolioApi } from '@/lib/api/portfolio'

export default function CreatePortfolioPage() {
  const router = useRouter()

  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [currencyPreference, setCurrencyPreference] = useState('USD')
  const [createAccount, setCreateAccount] = useState(true)

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      const portfolio = await portfolioApi.createPortfolio({
        name,
        description,
        currencyPreference,
        createAccount,
      })

      // If backend auto-created an account, just go to portfolio
      router.push('/portfolio')
      router.refresh()
    } catch (err) {
      console.error(err)
      setError('Failed to create portfolio.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-xl mx-auto p-6">
      <h1 className="text-2xl font-bold mb-4">Create Portfolio</h1>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm font-medium">Portfolio Name</label>
          <input
            className="w-full border rounded px-3 py-2"
            value={name}
            onChange={e => setName(e.target.value)}
            required
          />
        </div>

        <div>
          <label className="block text-sm font-medium">Description</label>
          <textarea
            className="w-full border rounded px-3 py-2"
            value={description}
            onChange={e => setDescription(e.target.value)}
          />
        </div>

        <div>
          <label className="block text-sm font-medium">Base Currency</label>
          <select
            className="w-full border rounded px-3 py-2"
            value={currencyPreference}
            onChange={e => setCurrencyPreference(e.target.value)}
          >
            <option value="USD">USD</option>
            <option value="CAD">CAD</option>
            <option value="EUR">EUR</option>
            <option value="GBP">GBP</option>
          </select>
        </div>

        <div className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={createAccount}
            onChange={e => setCreateAccount(e.target.checked)}
          />
          <label className="text-sm">
            Create my first account now
          </label>
        </div>

        {error && <p className="text-red-500 text-sm">{error}</p>}

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-blue-600 text-white py-2 rounded"
        >
          {loading ? 'Creating…' : 'Create Portfolio'}
        </button>
      </form>
    </div>
  )
}