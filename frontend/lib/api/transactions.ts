// lib/api/transaction.ts
import { apiClient } from './client'
import type { Transaction } from '@/types/portfolio'


export const transactionApi = {
  addTransaction: async (portfolioId: string, accountId: string, request: Omit<Transaction, 'id' | 'recordedAt'>): Promise<Transaction> => {
    const { data } = await apiClient.post(`/api/portfolios/${portfolioId}/accounts/${accountId}/transactions`, request)
    return data
  },

  getTransactionsForAccount: async (portfolioId: string, accountId: string): Promise<Transaction[]> => {
    const { data } = await apiClient.get(`/api/portfolios/${portfolioId}/accounts/${accountId}/transactions`)
    return data
  },
}