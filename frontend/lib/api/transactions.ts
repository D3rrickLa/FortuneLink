// lib/api/transaction.ts
import { apiClient } from './client'
import type { Transaction } from '@/types/portfolio'
import { CreateTransactionInput } from './transaction.types'

export const mapTransactionToRequest = (tx: CreateTransactionInput) => {
    return {
        transactionType: tx.transactionType,
        symbol: tx.transactionType === 'BUY' ? tx.symbol : undefined,
        quantity: tx.quantity,
        price: tx.price.amount,
        priceCurrency: tx.price.currency,
        fees: tx.fee ? [{ amount: tx.fee.amount, currency: tx.fee.currency }] : [],
        transactionDate: tx.transactionDate,          // ISO string
        notes: tx.notes,
        isDrip: tx.transactionType === 'BUY' ? tx.isDrip ?? false : undefined,
        sharesReceived: tx.transactionType === 'BUY' ? tx.sharesReceived ?? 0 : undefined,
    }
}

export const transactionApi = {
    addTransaction: async (portfolioId: string, accountId: string, tx: CreateTransactionInput): Promise<Transaction> => {
        const request = mapTransactionToRequest(tx)

        const path = tx.transactionType.toLowerCase()
        const { data } = await apiClient.post(
            `/api/portfolios/${portfolioId}/accounts/${accountId}/transactions/${path}`,
            request
        )

        return data
    },

    getTransactionsForAccount: async (portfolioId: string, accountId: string): Promise<Transaction[]> => {
        const { data } = await apiClient.get(`/api/portfolios/${portfolioId}/accounts/${accountId}/transactions`)
        return data ?? []
    },
}