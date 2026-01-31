// lib/api/portfolio.ts
import { apiClient } from './client'
import type { Portfolio, Transaction, Account } from '@/types/portfolio'

// TODO: CONFIRM THE ROUTES
export const portfolioApi = {
    checkHealth: async () => {
        try {
            const response = await apiClient.get('/api/portfolios/health-check')
            return response.data
        } catch (error: any) {
            console.error("Health check failed. Check if Spring is running on 8080 and CORS is set.");
            throw error;
        }
    },
    getPortfolio: async (userId: string): Promise<Portfolio> => {
        const { data } = await apiClient.get(`/api/portfolios/${userId}`)
        return data
    },

    addTransaction: async (portfolioId: string, transaction: Transaction): Promise<Transaction> => {
        const { data } = await apiClient.post(
            `/api/portfolio/${portfolioId}/transactions`,
            transaction
        )
        return data
    },

    getNetWorth: async (portfolioId: string): Promise<{ netWorth: number }> => {
        const { data } = await apiClient.get(`/api/portfolios/${portfolioId}/net-worth`)
        return data
    },

    addAccount: async (portfolioId: string, account: Partial<Account>): Promise<Account> => {
        const { data } = await apiClient.post(`/api/portfolios/${portfolioId}/accounts`, account)
        return data
    },
}