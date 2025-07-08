# ENTITIES (Aggregate Root & Entities)
https://beatmarket.com/blog/a-complete-guide-to-portfolio-accounting-systems/
## Portfolio - Aggregate Root
**Purpose**: Main aggregate controlling all portfolio operations
**Changes**: Added portfolio analysis methods, moved asset operations here

Variables:
- UUID portfolioId
- UUID userId (FK -> supabase)
- String portfolioName
- String portfolioDescription
- Money portfolioCashBalance
- Currency currencyPreference
- List<Fee> fees
- List<Transaction> transactions
- List<AssetHolding> assetHoldings
- List<Liability> liabilities

Methods:
- recordCashflow(CashflowTransactionDetails details)
- recordAssetPurchase(AssetTransactionDetails details) // MOVED FROM AssetHolding
- recordAssetSale(AssetTransactionDetails details) // MOVED FROM AssetHolding
- recordNewLiability(LiabilityIncurrenceTransactionDetails details)
- recordLiabilityPayment(LiabilityPaymentTransactionDetails details)
- reverseTransaction(UUID transactionId, String reason)
- calculateTotalValue(Map<AssetIdentifier, MarketPrice> currentPrices)
- calculateUnrealizedGains(Map<AssetIdentifier, MarketPrice> currentPrices)
- getAssetAllocation(Map<AssetIdentifier, MarketPrice> currentPrices)
- accrueInterestOnLiabilities() // Calls liability.accrueInterest() on all liabilities

## AssetHolding - Entity
**Purpose**: Tracks quantity and cost basis of assets
**Changes**: Removed transaction methods (moved to Portfolio), simplified to pure data + calculations

Variables:
- UUID assetId
- UUID portfolioId
- AssetIdentifier assetIdentifier
- BigDecimal totalQuantity
- Money totalAdjustedCostBasis (ACB for Canada)
- Money averageCostPerUnit
- Instant createdAt
- Instant updatedAt

Methods:
- getAverageACBPerUnit() // totalAdjustedCostBasis / totalQuantity
- calculateCapitalGain(BigDecimal soldQuantity, Money salePrice)
- addToPosition(BigDecimal quantity, Money costBasis) // Called by Portfolio
- removeFromPosition(BigDecimal quantity) // Called by Portfolio
- getCurrentValue(MarketPrice currentPrice)

## Liability - Entity
**Purpose**: Tracks debt obligations
**Changes**: Added interest calculation methods

Variables:
- UUID liabilityId
- UUID portfolioId
- String liabilityName
- String liabilityDescription
- Money currentBalance
- Percentage annualInterestRate
- Instant maturityDate
- Instant lastInterestAccrualDate // NEW: Track when interest was last calculated

Methods:
- setName(String name)
- setDescription(String description)
- setInterestRate(Percentage rate)
- makePayment(Money paymentAmount, Money principalAmount, Money interestAmount)
- reversePayment(UUID paymentTransactionId)
- increaseLiabilityBalance(Money amount)
- calculateAccruedInterest() // NEW: Calculate interest since last accrual
- accrueInterest() // NEW: Add accrued interest to balance

## Transaction - Entity 🟨
**Purpose**: Immutable record of all portfolio changes
**Changes**: Added correlation and parent tracking, made truly immutable

Variables:
- UUID transactionId
- UUID portfolioId
- UUID correlationId // NEW: Group related transactions
- UUID parentTransactionId // NEW: Link reversals to originals
- TransactionType transactionType
- Money totalTransactionAmount
- Instant transactionDate
- TransactionDetails transactionDetails
- TransactionMetadata transactionMetadata
- List<Fee> fees
- Boolean hidden
- Integer version // NEW: For optimistic locking

Methods:
- isReversal() // if this transaction caused a reversal
- isReversed() // Check if this transaction has been reversed 
- getRelatedTransactions() // Find transactions with same correlationId


NOTE -> isReversed and getRelatedTransaction should NOT be in the domain, but rather the repo

# --- VALUE OBJECTS ---

## Enhanced AssetIdentifier - Value Object ✅
**Purpose**: Robust asset identification using industry standards
**Changes**: Added ISIN/CUSIP support, factory methods

- Variables
    - AssetType assetType
    - String ISIN
    - String assetName
    - String assetExchangeInformation // where the stock/asset is being bought on (TSX? NYSX?)
    - String assetDescription
- Methods
    - isCrypto
    - isStockOrEtf


## Money - Value Object ✅
**Purpose**: Represent monetary amounts with currency
**Changes**: Added utility methods for better usability

Variables:
- BigDecimal amount
- Currency currency

Methods:
- add(Money other)
- subtract(Money other)
- multiply(BigDecimal multiplier)
- multiply(Long multiplier)
- divide(BigDecimal divisor)
- divide(Long divisor)
- negate()
- abs() // NEW: Absolute value
- isZero() // NEW: Check if amount is zero
- isPositive() // NEW: Check if amount > 0
- isNegative() // NEW: Check if amount < 0
- compareTo(Money other)
- convertTo(Currency targetCurrency, ExchangeRate rate)
- round() // NEW: Round to currency's default precision
- static Money zero(Currency currency)

## ExchangeRate - Value Object (NEW) ✅
**Purpose**: Represent currency exchange rates with metadata
**Why Added**: Proper currency conversion tracking

Variables:
- Currency fromCurrency
- Currency toCurrency
- BigDecimal rate
- Instant rateDate
- String source // "Bank of Canada", "xe.com", etc.

Methods:
- getRate()
- isExpired(Duration maxAge)
- getInverseRate()

## MarketPrice - Value Object (NEW) ✅
**Purpose**: Current market price of an asset
**Why Added**: Portfolio valuation needs current prices

Variables:
- AssetIdentifier assetIdentifier
- Money price
- Instant priceDate
- String source // "Yahoo Finance", "Manual", etc.

Methods:
- isStale(Duration maxAge)
- getPriceInCurrency(Currency targetCurrency, ExchangeRate rate)

## Fee - Value Object ✅
**Purpose**: Represent transaction fees
**Changes**: None, already good

Variables:
- FeeType feeType
- Money feeAmount

## TransactionMetadata - Value Object ✅
**Purpose**: Track transaction lifecycle
**Changes**: None, already good

Variables:
- TransactionStatus transactionStatus
- TransactionSource transactionSource
- String transactionDescription
- Instant createdAt
- Instant updatedAt

Methods:
- static TransactionMetadata create(TransactionSource source, String description)
- updateStatus(TransactionStatus newStatus)

## Percentage - Value Object ✅
**Purpose**: Represent percentage values
**Changes**: None, already good

Variables:
- BigDecimal percentValue

Methods:
- static Percentage fromPercent(BigDecimal percent)
- static Percentage fromDecimal(BigDecimal decimal)
- toDecimal()
- toPercent()

#  --- TRANSACTION DETAILS (Interface Implementations) ---

## CashflowTransactionDetails - Value Object
**Purpose**: Track cash inflows/outflows
**Changes**: Enhanced currency conversion tracking

Variables:
- Money originalCashflowAmount
- Money convertedCashflowAmount
- ExchangeRate exchangeRate // CHANGED: Use ExchangeRate VO
- Money totalConversionFees // COMBINED: forex + other fees

## AssetTransactionDetails - Value Object
**Purpose**: Track asset buy/sell transactions
**Changes**: Simplified fee structure

Variables:
- AssetIdentifier assetIdentifier
- BigDecimal quantity
- Money pricePerUnit
- Money assetValueInAssetCurrency
- Money assetValueInPortfolioCurrency
- Money costBasisInPortfolioCurrency
- Money totalFeesInPortfolioCurrency // SIMPLIFIED: Combined all fees

## LiabilityIncurrenceTransactionDetails - Value Object
**Purpose**: Track new debt
**Changes**: None, already good

Variables:
- Money originalLoanAmount
- Percentage annualInterestRate
- Instant maturityDate

## LiabilityPaymentTransactionDetails - Value Object
**Purpose**: Track debt payments
**Changes**: None, already good

Variables:
- UUID liabilityId
- Money totalPaymentAmount
- Money principalAmount
- Money interestAmount
- Money feesAmount

## ReversalTransactionDetails - Value Object
**Purpose**: Track transaction reversals
**Changes**: None, already good

Variables:
- UUID originalTransactionId
- String reason

# ENUMS (No changes needed)
- AssetType
- CryptoSymbols
- DecimalPrecision
- FeeType
- TransactionSource
- TransactionStatus
- TransactionType

# CLASSES REMOVED:
- AssetTransferTransactionDetails: Too complex for DIY tool
- CorporateActionTransactionDetails: Not MVP, can add later
- TaxLot: Not needed for Canadian ACB method
- AllocationTarget: Too complex for DIY tool

# CLASSES ADDED:
- ExchangeRate: Proper currency conversion tracking
- MarketPrice: Portfolio valuation support


---
# KEY CHANGES SUMMARY:
1. **Moved asset operations** from AssetHolding to Portfolio (proper aggregate pattern)
2. **Enhanced AssetIdentifier** with ISIN/CUSIP support
3. **Added liability interest calculation** methods
4. **Improved transaction linking** with correlationId and parentTransactionId
5. **Added portfolio analysis methods** for total value and gains
6. **Simplified fee structure** by combining related fees
7. **Added currency conversion support** with ExchangeRate VO
8. **Added market price tracking** for portfolio valuation