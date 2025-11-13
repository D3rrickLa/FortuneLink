# Domain Layer Checklist
## ✅ Value Objects

 - [x] Money - amount, currency, arithmetic operations
 - [x] Currency - enumeration (USD, CAD, EUR, GBP)
 - [x] Percentage - value, arithmetic operations
 - [x] Fee - wraps Money, apply() method
 - [x] ExchangeRate - from/to currencies, rate, convert()
 - [x] AssetSymbol (or AssetIdentifier) - symbol string, validation
 - ~~[x] Quantity - amount, arithmetic operations~~
 - ~~[x] Price - price per unit, calculateValue()~~
 - ~~[x] TransactionDate - timestamp, comparison methods~~
 - [x] AccountType - enumeration (TFSA, RRSP, NON_REGISTERED, etc.)
 - [x] AssetType - enumeration (STOCK, ETF, CRYPTO, BOND, REAL_ESTATE)
 - [x] TransactionType - enumeration (BUY, SELL, DEPOSIT, WITHDRAWAL, DIVIDEND, INTEREST, FEE, TRANSFER_IN, TRANSFER_OUT)

## ✅ Entities

 - [x] Asset - id, symbol, type, quantity, cost basis, acquired date

 - [x] updateQuantity()
 - [x] updateCostBasis()
 - [x] calculateCurrentValue(Price)
 - [x] calculateUnrealizedGain(Price)


 - [x] Transaction - id, type, symbol, assetType, quantity, price, amount, fee, date, notes

 - [x] calculateTotalCost() (for BUY)
 - [x] calculateNetAmount() (for SELL)
 - [x] getAmount() (for cash transactions)


 - [x] Account - id, name, type, base currency, cash balance, assets, transactions

 - [x] recordTransaction(Transaction) - adds transaction and applies effects
 - [x] applyTransaction(Transaction) - updates cash/assets based on transaction type
 - [x] addOrUpdateAssetFromBuy(Transaction)
 - [x] reduceAssetFromSell(Transaction)
 - [x] updateTransaction(TransactionId, Transaction)
 - [x] deleteTransaction(TransactionId)
 - [x] recalculateStateAfterChange()
 - [x] getCashBalance()
 - [x] calculateTotalValue(MarketDataService)
 - [x] getTransactions()
 - [x] Asset management: addAsset(), updateAsset(), deleteAsset()



## ✅ Aggregate Root

 - [x] Portfolio - id, userId, accounts, created/updated dates

 - [x] addAccount(Account)
 - [x] removeAccount(AccountId)
 - [x] getAccount(AccountId)
 - [x] recordTransaction(AccountId, Transaction) - delegates to account
 - [x] updateTransaction(AccountId, TransactionId, Transaction)
 - [x] deleteTransaction(AccountId, TransactionId)
 - [x] getAllTransactions() - aggregates across accounts
 - [x] getTransactionHistory(startDate, endDate) - filtered by date range
 - [x] calculateNetWorth(MarketDataService) - aggregates across accounts
 - [x] updateMetadata() - updates lastUpdated timestamp



## ✅ Domain Services

 - [x] PortfolioValuationService

    - [x] calculateTotalValue(Portfolio, MarketDataService)
    - [x] calculateAccountValue(Account, MarketDataService)
    - [x] calculateAssetValue(Asset, MarketDataService)

 - [x] PerformanceCalculationService

    - [x] calculateTotalReturn(Portfolio, MarketDataService)
    - [x] calculateRealizedGains(Portfolio, List<Transaction>)
    - [x] calculateUnrealizedGains(Portfolio, MarketDataService)
    - [x] calculateTimeWeightedReturn(Portfolio) (optional for MVP)

 - [x] AssetAllocationService

    - [x] calculateAllocationByType(Portfolio, MarketDataService)
    - [x] calculateAllocationByAccount(Portfolio, MarketDataService)
    - [x] calculateAllocationByCurrency(Portfolio, MarketDataService)



## ✅ Repository Interfaces (Domain Layer)

 - [x] PortfolioRepository

    - [x] save(Portfolio)
    - [x] findById(PortfolioId)
    - [x] findByUserId(UserId)
    - [x] delete(PortfolioId)


 - [x] MarketDataService (interface)

    - [x] getCurrentPrice(AssetSymbol)
    - [x] getHistoricalPrice(AssetSymbol, LocalDateTime)
    - [x] getBatchPrices(List<AssetSymbol>)



## ✅ Domain Exceptions

 - [x] PortfolioNotFoundException
 - [x] AccountNotFoundException
 - [x] AssetNotFoundException
 - [x] TransactionNotFoundException
 - [x] InsufficientFundsException
 - [x] CurrencyMismatchException
 - [x] UnsupportedTransactionTypeException
 - [x] InvalidAssetSymbolException

## ✅ IDs (Value Objects)

 - [x] PortfolioId
 - [x] AccountId
 - [x] AssetId
 - [x] TransactionId
 - [x] UserId (or reference from Auth context)