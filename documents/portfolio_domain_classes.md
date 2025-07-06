because I keep on messing up, I am mapping out all the mapping out all the classes and value objects

things to note: 
- we can never delete, we just 'reverse' a transaction. to the user it will look like it was deleted, but acutally it was marked as 'hidden' and can't be seen to the user

# Entities
Portfolio - Aggregate root 
- Variables
    - UUID portfolioId
    - UUID UserId (FK -> supabase)
    - String portfolio Name
    - String portfolioDescription
    - Money portfolioCashBalance
    - Currency currencyPreference
    - List<Fees> fees 
    - List<Transaction> transaction
    - List<AssetHoldings> assetHoldings
    - List<Liability> liabilities

- Methods
    - recordCashflow
    - recordAssetHoldingPurchase 
    - recordAssetHoldingSale 
    - recordNewLiability 
    - recorrdLiabilityPayment
    - reverseTransaction

Liability - entity 
- Variables
    - UUID liabilityId
    - UUID portfolioId
    - String liabilityName
    - String liabilityDescription
    - Money currentBalance
    - Percentage interestRate
    - Instant maturityDate

- Methods
    - Setters for name, description, and InterestRate
    - makePayment
    - reversePayment
    - increaseLiabilityBalance

AssetHolding - entity
- Variables
    - UUID assetId
    - UUID portfolioId
    - Assetidentifier assetIdentifier
    - BigDecimal totalQuantity
    - Money totalAdjustedCostBasis
    - Money averageCostPerUnit
    - Instant createdAt
    - Instant updatedAt

- Methods
    - recordAdditionalAssetHoldingPurchase
    - recordSaleOfAssetHolding
    - reverseAssetHolding

Transaction - entity
- Variables
    - transactionId
    - portfolioId
    - TransactionType transactionType
    - Money totalTransaction Amount (including fees, normalized to portfolio currency)
    - Instant transactionDate
    - TransactionDetails transactionDetails
    - TransactionMetadata transactionMetadata
    - List<Fee> fees
    - Boolean hidden


# Value Objects
## Enums
- AssetType
- CryptoSymbols
- DecimalPrecision
- FeeType
- TransactionSource
- TransactionStatus
- TransactionType

## interface/abstract
- TransactionDetails

## others
AssetIdentifier - VO
- Variables
    - AssetType assetType
    - String assetName
    - String primaryIdentifier
    - String secondaryIdentifier
    - String assetDescription
- Methods
    - isCrypto
    - isStockOrEtf

FEE - VO
- Variables
    - FeeType feeType
    - Money feeAmount

TransactionMetadata - VO
- Variables
    - TransactionStatus transactionStatus
    - TransactionSource transactionSource
    - String transactionDescription
    - Instant createdAt 
    - Instant updatedAt
- Methods
    - createMetadata (a static method, don't need to do new ....)
    - updateStatus

## impelments of TransactionDetails
AssetTransactionDetails
- Variables
    - AssetIdentifier assetIdentifier
    - BigDecimal quantity
    - Money pricePerUnit
    - TransactionType transactionType // we might not need this if we have it in the Transaction class already
    - Money assetValueInAssetCurrency 
    - Money assetValueInPortfolioCurrency
    - Money costBasisOfSoldQuantityInPortfolioCurrency
    - Money totalForexConversionFeeInPortfolioCurrency
    - Money totalotherFeesInPortfolioCurrency // we should really just combined these 2 into 1 item

AssetTransferTransactionDetails // shouldn't this just be inside the AssetTransactionDetails, seems kind of werid to have this
- Variables
    - UUID sourceAccountId
    - UUID destinationAccountId;
    - AssetIdentifier assetIdentifier
    - BigDecimal quantity
    - Money costBasisPerUnit

CorporateActionTransactionDetails // handles things like 'splits', 'mergers', etc. tbh, might not do this, not a MVP item
- Variables
    - AssetIdentifier assetIdentifier
    - BigDeicmal splitRatio


LiabilityIncurrenceTransactionDetails // for when you have a liability initially, say you first record a student loan
- Variables 
    - Money originalLoanAmount
    - Percentage interestPercentage
    - Instant maturityDate

LiabilityPaymentTransactionDetails // for making a loan payment
- Variables
    - UUID liabilityId 
    - Money totoalOriginalPaymentAmount
    - Money principalPaidAmount
    - Money interestPaidAmount
    - Money feesPaid Amount
    - Money cashflowOutOfPortfolioCurrency

ReversalTransactionDetails // for reversing any of the above transactions
- Vaariables
    - UUID originalTransactionid
    - TransactionType <- should really just be implied as reversal
    - String reason

# shared kernel
Money
- Variables
    - BigDecimal amount
    - PortfolioCurrency currency // need a better name for this

- Methods
    - Add
    - Subtract
    - Multiple // multiple either by Long value or BigDecimal
    - Divide
    - ZERO
    - compareTo
    - negate
    - convert // converting currency (i.e. USD -> CAD)

Percentage
- Variables
    - BigDecimal percentValue

- Methods
    - Percentage fromPercent // (using this instead of constructor, we pass in a BigDecimal as the % and converts it to 0.NNNN)


PortfolioCurrency
- Variables
    - Java.util.Currency currency

- Methods
    - code
    - getDefaultScale
    - isFiat