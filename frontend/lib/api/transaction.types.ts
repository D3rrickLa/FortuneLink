// transaction.types.ts
export type CreateBuyTransaction = {
    transactionType: 'BUY'
    symbol: string
    quantity: number
    price: { amount: number; currency: string }
    fee?: { amount: number; currency: string }
    transactionDate: string
    notes?: string
    isDrip?: boolean
    sharesReceived?: number
}

export type CreateSellTransaction = {
    transactionType: 'SELL'
    assetId: string
    quantity: number
    price: { amount: number; currency: string }
    fee?: { amount: number; currency: string }
    transactionDate: string
    notes?: string
}

export type CreateTransactionInput =
    | CreateBuyTransaction
    | CreateSellTransaction