// lib/api/transaction.ts
import { apiClient } from './client'
import type { Transaction } from '@/types/portfolio'

export const mapTransactionToRequest = (tx: Omit<Transaction, 'id' | 'recordedAt'>) => {
    return {
        transactionType: tx.transactionType,           // 'BUY' | 'SELL' | etc
        symbol: tx.symbol,                             // for BUY
        assetId: tx.id,                           // for SELL / DIVIDEND
        quantity: tx.quantity,
        price: tx.price.amount,
        priceCurrency: tx.price.currency,
        fees: tx.fee ? [{ amount: tx.fee.amount, currency: tx.fee.currency }] : [],
        transactionDate: tx.transactionDate,          // ISO string
        notes: tx.notes,
        isDrip: tx.isDrip ?? false,
        sharesReceived: tx.sharesReceived ?? 0,
    }
}

export const transactionApi = {
    addTransaction: async (
        portfolioId: string,
        accountId: string,
        tx: Omit<Transaction, 'id' | 'recordedAt'>,
    ): Promise<Transaction> => {
        const request = mapTransactionToRequest(tx)

        const path = tx.transactionType === 'BUY' ? '/buy' : '/sell'

        const { data } = await apiClient.post(
            `/api/portfolios/${portfolioId}/accounts/${accountId}/transactions${path}`,
            request
        )

        return data
    },

    getTransactionsForAccount: async (
        portfolioId: string,
        accountId: string
    ): Promise<Transaction[]> => {
        const { data } = await apiClient.get(
            `/api/portfolios/${portfolioId}/accounts/${accountId}/transactions`
        )
        return data ?? []
    },
}