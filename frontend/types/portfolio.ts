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

// ===== Portfolio =====

export interface Portfolio {
  id: string
  userId: string
  name: string
  description: string
  accounts: Account[]
  totalValue: number
  totalValueCurrency: Currency
  createdDate: string // ISO string from LocalDateTime
  lastUpdated: string // ISO string from LocalDateTime
}

// ===== Account =====

export interface Account {
  id: string
  portfolioId: string
  name: string
  accountType: AccountType
  baseCurrency: Currency
  assets: AssetHolding[]
}

// ===== Asset =====

export interface AssetHolding {
  id: string
  symbol: string
  assetType: AssetType
  quantity: number
  costBasis: number
  acquiredDate: string // ISO string
}

// ===== Transaction =====

export interface Transaction {
  id: string
  accountId: string
  transactionType: TransactionType
  symbol: string
  quantity: number
  price: number
  priceCurrency: Currency
  fee: number
  totalCost: number
  netAmount: number
  transactionDate: string // ISO string
  notes: string
  recordedAt: string // ISO string
}