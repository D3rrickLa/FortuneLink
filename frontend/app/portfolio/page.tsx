'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { createClient } from '@/lib/utils/supabase/client'
import { portfolioApi } from '@/lib/api/portfolio'
import type { Portfolio } from '@/types/portfolio'

export default function PortfolioPage() {
    const [portfolios, setPortfolios] = useState<Portfolio[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const router = useRouter()
    const supabase = createClient()

    const activePortfolio = portfolios[0]
    const hasPortfolios = portfolios.length > 0
    const hasAccounts = activePortfolio?.accounts?.length > 0

    useEffect(() => {
        const loadData = async () => {
            try {
                const {
                    data: { user },
                    error: authError,
                } = await supabase.auth.getUser()

                if (authError || !user) {
                    router.push('/auth/login')
                    return
                }

                const data = await portfolioApi.getMyPortfolios()
                setPortfolios(data)
            } catch (err: any) {
                console.error(err)
                setError('Failed to load portfolio data.')
            } finally {
                setLoading(false)
            }
        }

        loadData()
    }, [router, supabase.auth])

    if (loading) {
        return <div className="p-10 text-center">Loading your financial data…</div>
    }

    if (error) {
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
    }

    return (
        <div className="container mx-auto p-6">
            <h1 className="text-3xl font-bold mb-6">My Portfolio</h1>

            {/* -------- No Portfolio -------- */}
            {!hasPortfolios && (
                <EmptyState
                    title="Welcome 👋"
                    description="Let’s start by creating your first portfolio."
                    actionLabel="Create Portfolio"
                    onAction={() => router.push('/portfolio/create')}
                />
            )}

            {/* -------- Portfolio, No Accounts -------- */}
            {hasPortfolios && !hasAccounts && (
                <EmptyState
                    title="Your portfolio is ready"
                    description="You don’t have any accounts yet."
                    actionLabel="Add Account"
                    onAction={() =>
                        router.push(`/portfolio/${activePortfolio.id}/accounts/create`)
                    }
                />
            )}

            {/* -------- Portfolio + Accounts -------- */}
            {hasAccounts && (
                <>
                    <div className="bg-white rounded-lg shadow p-6 mb-6">
                        <h2 className="text-sm text-gray-500 uppercase tracking-wide">
                            Net Worth
                        </h2>
                        <p className="text-3xl font-bold mt-2">
                            {activePortfolio.totalValue.toLocaleString()}{' '}
                            {activePortfolio.totalValueCurrency}
                        </p>
                    </div>

                    <div>
                        <h3 className="text-xl font-semibold mb-4">Accounts</h3>
                        <ul className="space-y-3">
                            {activePortfolio.accounts.map(account => (
                                <li
                                    key={account.id}
                                    className="border rounded p-4 hover:bg-gray-50 cursor-pointer"
                                    onClick={() =>
                                        router.push(
                                            `/portfolio/${activePortfolio.id}/accounts/${account.id}`
                                        )
                                    }
                                >
                                    <p className="font-medium">{account.name}</p>
                                    <p className="text-sm text-gray-500">
                                        {account.accountType} · {account.baseCurrency}
                                    </p>
                                </li>
                            ))}
                        </ul>
                    </div>
                </>
            )}
        </div>
    )
}

function EmptyState({
    title,
    description,
    actionLabel,
    onAction,
}: {
    title: string
    description: string
    actionLabel: string
    onAction: () => void
}) {
    return (
        <div className="text-center border rounded-lg p-10 bg-white">
            <h2 className="text-xl font-semibold">{title}</h2>
            <p className="text-gray-500 mt-2">{description}</p>
            <button
                onClick={onAction}
                className="mt-4 px-4 py-2 bg-blue-600 text-white rounded"
            >
                {actionLabel}
            </button>
        </div>
    )
}