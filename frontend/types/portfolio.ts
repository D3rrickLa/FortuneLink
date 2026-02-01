// ===== Enums (string unions must match backend values) =====

export type Currency = 'USD' | 'CAD' | 'EUR' | 'GBP'

export type AssetType =
    | 'STOCK'
    | 'ETF'
    | 'CRYPTO'
    | 'CASH'
    | 'BOND'
    | 'REAL_ESTATE'

export type TransactionType =
    | 'BUY'
    | 'SELL'
    | 'DEPOSIT'
    | 'WITHDRAWAL'
    | 'DIVIDEND'
    | 'INTEREST'
    | 'FEE'

export type AccountType =
    | 'TFSA'
    | 'RRSP'
    | 'NON_REGISTERED'
    | 'INVESTMENT'
    | 'CHEQUING'
    | 'SAVINGS'


export interface PortfolioSummary {
    id: string
    name: string | null
    description?: string | null
    totalValue: number
    currency: string
    numberOfAccounts?: number
    createdDate: string
    lastUpdated: string
}

export interface Portfolio {
    id: string
    userId: string
    name: string
    description: string
    accounts: Account[]
    totalValue?: { amount: number; currency: Currency } // derived from currentValue of assets
    createdDate: string
    lastUpdated: string
}

export interface Account {
    id: string
    portfolioId: string
    name: string
    accountType: AccountType
    baseCurrency: Currency
    assets: AssetHolding[]
    totalValue?: { amount: number; currency: Currency } // optional
}

export interface AssetHolding {
    id: string
    symbol: string
    assetType: AssetType
    quantity: number
    costBasis: { amount: number; currency: Currency }
    currentValue?: { amount: number; currency: Currency } // optional
    acquiredDate: string
}

export interface Transaction {
    id: string
    accountId: string
    transactionType: TransactionType
    symbol: string
    quantity: number
    price: { amount: number; currency: Currency }
    fee?: { amount: number; currency: Currency }
    totalCost?: { amount: number; currency: Currency }
    netAmount?: { amount: number; currency: Currency }
    transactionDate: string
    notes?: string
    recordedAt: string
}