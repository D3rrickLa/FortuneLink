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

 Asset - id, symbol, type, quantity, cost basis, acquired date

 updateQuantity()
 updateCostBasis()
 calculateCurrentValue(Price)
 calculateUnrealizedGain(Price)


 Transaction - id, type, symbol, assetType, quantity, price, amount, fee, date, notes

 calculateTotalCost() (for BUY)
 calculateNetAmount() (for SELL)
 getAmount() (for cash transactions)


 Account - id, name, type, base currency, cash balance, assets, transactions

 recordTransaction(Transaction) - adds transaction and applies effects
 applyTransaction(Transaction) - updates cash/assets based on transaction type
 addOrUpdateAssetFromBuy(Transaction)
 reduceAssetFromSell(Transaction)
 updateTransaction(TransactionId, Transaction)
 deleteTransaction(TransactionId)
 recalculateStateAfterChange()
 getCashBalance()
 calculateTotalValue(MarketDataService)
 getTransactions()
 Asset management: addAsset(), updateAsset(), deleteAsset()



## ✅ Aggregate Root

 - [] Portfolio - id, userId, accounts, created/updated dates

 - [] addAccount(Account)
 - [] removeAccount(AccountId)
 - [] getAccount(AccountId)
 - [] recordTransaction(AccountId, Transaction) - delegates to account
 - [] updateTransaction(AccountId, TransactionId, Transaction)
 - [] deleteTransaction(AccountId, TransactionId)
 - [] getAllTransactions() - aggregates across accounts
 - [] getTransactionHistory(startDate, endDate) - filtered by date range
 - [] calculateNetWorth(MarketDataService) - aggregates across accounts
 - [] updateMetadata() - updates lastUpdated timestamp



## ✅ Domain Services

 - [x] PortfolioValuationService

    - [x] calculateTotalValue(Portfolio, MarketDataService)
    - [x] calculateAccountValue(Account, MarketDataService)
    - [x] calculateAssetValue(Asset, MarketDataService)

 - PerformanceCalculationService

    - calculateTotalReturn(Portfolio, MarketDataService)
    - calculateRealizedGains(List<Transaction>)
    - calculateUnrealizedGains(Portfolio, MarketDataService)
    - calculateTimeWeightedReturn(Portfolio) (optional for MVP)

 - AssetAllocationService

    - calculateAllocationByType(Portfolio, MarketDataService)
    - calculateAllocationByAccount(Portfolio, MarketDataService)
    - calculateAllocationByCurrency(Portfolio, MarketDataService)



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

 - [] PortfolioNotFoundException
 - [] AccountNotFoundException
 - [] AssetNotFoundException
 - [] TransactionNotFoundException
 - [] InsufficientFundsException
 - [] CurrencyMismatchException
 - [] UnsupportedTransactionTypeException
 - [] InvalidAssetSymbolException

## ✅ IDs (Value Objects)

 - [x] PortfolioId
 - [x] AccountId
 - [x] AssetId
 - [x] TransactionId
 - [x] UserId (or reference from Auth context)