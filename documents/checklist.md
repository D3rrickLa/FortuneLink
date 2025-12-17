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

---- dec 17th ----
1. Core Application Services ✓

 PortfolioManagementService - with all write operations

 - [x] createPortfolio()
 - [x] deletePortfolio()
 - [x] recordAssetPurchase()
 - [x] recordAssetSale()
 - [x] recordDeposit()
 - [x] recordWithdrawal()
 - [x] recordDividendIncome()
 - [x] recordFee()
 - [x] addAccount()
 - [x] removeAccount()
 - [] updateTransaction()
 - [] deleteTransaction()



 PortfolioQueryService - with all read operations

 - [x] getNetWorth()
 - [x] getPortfolioPerformance()
 - [x] getAssetAllocation()
 - [x] getTransactionHistory()
 - [x] getAccountSummary()
 - [x] getPortfolioSummary()


2. Command Classes (Write DTOs) ✓

 - [x] CreatePortfolioCommand
 - [x] DeletePortfolioCommand
 - [x] RecordPurchaseCommand
 - [x] RecordSaleCommand
 - [x] RecordDepositCommand
 - [x] RecordWithdrawalCommand
 - [x] RecordIncomeCommand
 - [x] RecordFeeCommand
 - [x] AddAccountCommand
 - [x] RemoveAccountCommand
 - [x] UpdateTransactionCommand
 - [x] DeleteTransactionCommand
3. Query Classes (Read DTOs) ✓

 - [x] ViewNetWorthQuery
 - [x] ViewPerformanceQuery
 - [x] AnalyzeAllocationQuery
 - [x] GetTransactionHistoryQuery
 - [x] GetAccountSummaryQuery
 - [x] GetPortfolioSummaryQuery

4. Response Classes (Output DTOs) ✓

 - [x] PortfolioResponse
 - [x] NetWorthResponse
 - [x] PerformanceResponse
 - [x] AllocationResponse (with nested AllocationDetail)
 - [x] TransactionResponse
 - [x] TransactionHistoryResponse
 - [x] AccountResponse
5. Validator Classes ✓

 CommandValidator - with validation methods for all commands

 - [x] validate(RecordPurchaseCommand)
 - [x] validate(RecordSaleCommand)
 - [x] validate(RecordDepositCommand)
 - [x] validate(RecordWithdrawalCommand)
 - [x] validate(AddAccountCommand)
 - [x] validateAmount(Money)
 - [x] validateQuantity(Quantity)
 - [x] validateDate(TransactionDate)
 - [x] validateSymbol(AssetSymbol)


 ValidationResult - holds validation outcome
6. Mapper Classes ✓

 PortfolioMapper

 - [x] toResponse(Portfolio, MarketDataService)
 - [x] toAccountResponse(Account, MarketDataService)


 TransactionMapper

 - [x] toResponse(Transaction)
 - [x] toResponseList(List<Transaction>)


 AllocationMapper

 - [x] toResponse(Map<String, Money>, Money totalValue)
 - [x] toAllocationDetail(String category, Money value, Money total)


7. Exception Classes ✓

 - [x] PortfolioNotFoundException
 - [x] AccountNotFoundException
 - [x] InsufficientFundsException
 - [x] InsufficientHoldingsException
 - [x] InvalidTransactionException
 - [x] AccountNotEmptyException
8. Utility/Helper Classes ✓

 PaginationHelper

 - [x] calculateOffset()
 - [x] validatePageParameters()
 - [x] createPageInfo()


 DateRangeCalculator

 - [x] getYearToDate()
 - [x] getLastNDays()
 - [x] getLastNMonths()
 - [x] getLastNYears()

