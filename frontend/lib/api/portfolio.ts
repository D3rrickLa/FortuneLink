// lib/api/portfolio.ts
import { apiClient } from './client'
import type { Portfolio, Transaction, Account } from '@/types/portfolio'


export interface CreatePortfolioRequest {
    name: string;
    currencyPreference: string;
    description?: string;
    createAccount?: boolean;
}

export interface DeletePortfolioRequest {
    confirmed: boolean;
    softDelete: boolean;
}

export interface CreateAccountRequest {
    name: string;
    accountType: string;
    baseCurrency: string;
}

export interface AssetHoldingRequest {
    id: string;
    symbol: string;
    assetType: string;
    quantity: number;
    costBasis: number;
    acquiredDate: string;
}

/*
/portfolio
  → GET portfolios (summary)

/portfolio/[portfolioId]
  → GET accounts for portfolio
  → Create Account lives here

/portfolio/[portfolioId]/accounts/[accountId]
  → GET account details
  → GET transactions
  → Add Transaction
*/
export const portfolioApi = {
    checkHealth: async () => {
        try {
            const { data } = await apiClient.get('/api/portfolios/health-check')
            return data;
        } catch (error: any) {
            console.error("Health check failed. Check if Spring is running on 8080 and CORS is set.");
            throw error;
        }
    },

    createPortfolio: async (request: CreatePortfolioRequest): Promise<Portfolio> => {
        const { data } = await apiClient.post('/api/portfolios', request)
        return data
    },

    getPortfolioById: async (portfolioId: string): Promise<Portfolio> => {
        const { data } = await apiClient.get(`/api/portfolios/${portfolioId}`)
        return data
    },

    // THIS RETURNS A SUMMARY OF THE PORTOFLIOS NOT THE ACOCUNTS
    getMyPortfolios: async (): Promise<Portfolio[]> => {
        const { data } = await apiClient.get('/api/portfolios/user/me')
        console.log(data)
        return data
    },

    updatePortfolio: async (portfolioId: string, request: CreatePortfolioRequest): Promise<Portfolio> => {
        const { data } = await apiClient.put(`/api/portfolios/${portfolioId}`, request)
        return data
    },

    deletePortfolio: async (portfolioId: string, request: DeletePortfolioRequest): Promise<void> => {
        await apiClient.delete(`/api/portfolios/${portfolioId}`, { data: request })
    },

    // ---------- Accounts ----------

    listAccounts: async (portfolioId: string): Promise<Account[]> => {
        const { data } = await apiClient.get(`/api/portfolios/${portfolioId}/accounts`)
        return data
    },

    // NEW: get a single account by ID
    getAccount: async (portfolioId: string, accountId: string): Promise<Account> => {
        const { data } = await apiClient.get(`/api/portfolios/${portfolioId}/accounts/${accountId}`)
        return data
    },

    addAccount: async (portfolioId: string, request: CreateAccountRequest): Promise<Account> => {
        const { data } = await apiClient.post(`/api/portfolios/${portfolioId}/accounts`, request)
        return data
    },

    removeAccount: async (portfolioId: string, accountId: string): Promise<void> => {
        await apiClient.delete(`/api/portfolios/${portfolioId}/accounts/${accountId}`)
    },

    // ---------- Assets ----------

    getAsset: async (portfolioId: string, accountId: string, assetId: string): Promise<AssetHoldingRequest> => {
        const { data } = await apiClient.get(`/api/portfolios/${portfolioId}/accounts/${accountId}/assets/${assetId}`)
        return data
    },

}