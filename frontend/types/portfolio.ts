 export type Currency = 'USD' | 'CAD' | 'EUR' | 'GBP'
   export type AssetType = 'STOCK' | 'ETF' | 'CRYPTO' | 'CASH' | 'BOND' | 'REAL_ESTATE'
   export type TransactionType = 'BUY' | 'SELL' | 'DEPOSIT' | 'WITHDRAWAL' | 'DIVIDEND' | 'INTEREST' | 'FEE'
   export type AccountType = 'TFSA' | 'RRSP' | 'NON_REGISTERED' | 'INVESTMENT' | 'CHEQUING' | 'SAVINGS'

   export interface Money {
     amount: number
     currency: Currency
   }

   export interface Asset {
     id: string
     symbol: string
     type: AssetType
     quantity: number
     costBasis: Money
     acquiredDate: string
     currentValue?: Money
   }

   export interface Transaction {
     id?: string
     type: TransactionType
     symbol: string
     quantity: number
     price: Money
     fee?: Money
     date: string
     notes?: string
   }

   export interface Account {
     id: string
     name: string
     type: AccountType
     baseCurrency: Currency
     assets: Asset[]
   }

   export interface Portfolio {
     id: string
     userId: string
     accounts: Account[]
     netWorth?: Money
     createdDate: string
     lastUpdated: string
   }