// app/portfolio/page.tsx
'use client'
import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { createClient } from '@/lib/utils/supabase/client'
import { portfolioApi } from '@/lib/api/portfolio'
import type { Portfolio } from '@/types/portfolio'

export default function PortfolioPage() {
    const [portfolio, setPortfolio] = useState<Portfolio | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const router = useRouter()
    const supabase = createClient()

    // Handle Logout
    const handleLogout = async () => {
        await supabase.auth.signOut()
        router.push('/auth/login')
        router.refresh()
    }

    useEffect(() => {
        const loadPortfolio = async () => {
            try {
                // 1. Check if we have a user session
                const { data: { user }, error: authError } = await supabase.auth.getUser()

                if (authError || !user) {
                    router.push('/auth/login')
                    return
                }

                // 2. Fetch data from Spring Boot via Axios
                // Your Axios interceptor will automatically attach the Bearer token here
                const data = await portfolioApi.checkHealth()
                setPortfolio(data)
            } catch (err: any) {
                console.error("Failed to load portfolio:", err)
                setError(err.response?.data?.message || "Could not connect to the backend server.")
            } finally {
                setLoading(false)
            }
        }

        loadPortfolio()
    }, [router, supabase.auth])

    if (loading) return <div className="p-10 text-center">Loading your financial data...</div>

    if (error) return (
        <div className="p-10 text-center text-red-500">
            <p>{error}</p>
            <button onClick={() => window.location.reload()} className="mt-4 underline">Retry</button>
        </div>
    )

    return (
        <div className="container mx-auto p-6">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold">My Portfolio</h1>
                <button
                    onClick={handleLogout}
                    className="bg-blue-600 hover:bg-blue-500 px-4 py-2 rounded text-sm transition"
                >
                    Log Out
                </button>
            </div>

            {/* Net Worth Card */}
            <div className="bg-white rounded-lg shadow-md p-6 mb-6 border border-gray-100">
                <h2 className="text-sm font-medium text-gray-500 uppercase tracking-wider">Estimated Net Worth</h2>
                <p className="text-4xl font-bold text-green-600 mt-2">
                    ${portfolio?.netWorth?.amount.toLocaleString() ?? '0'}
                </p>
            </div>

            <div className="mt-8">
                <h3 className="text-xl font-semibold mb-4">Accounts</h3>
                {/* Once you uncomment your AccountCard, it goes here */}
                <p className="text-gray-400 italic">No accounts linked yet.</p>
            </div>
        </div>
    )
}