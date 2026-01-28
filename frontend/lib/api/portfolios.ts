// lib/api/portfolio.ts
import { apiClient } from './client'
import type { Portfolio, Transaction, Account } from '@/types/portfolios'

export const portfolioApi = {
    getPortfolio: async (userId: string): Promise<Portfolio> => {
        const { data } = await apiClient.get(`/api/portfolio/${userId}`)
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
        const { data } = await apiClient.get(`/api/portfolio/${portfolioId}/net-worth`)
        return data
    },

    addAccount: async (portfolioId: string, account: Partial<Account>): Promise<Account> => {
        const { data } = await apiClient.post(`/api/portfolio/${portfolioId}/accounts`, account)
        return data
    },
}