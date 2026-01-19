for historical look ups we either we are doing 2 ways
- API
- manual fallabck

the interface would look like this



    public interface ExchangeRateService {
        Optional<BigDecimal> getHistoricalRate(Currency from, Currency to, LocalDate date);
        void saveManualRate(Currency from, Currency to, LocalDate date, BigDecimal rate); // Fixed typos
        BigDecimal getOrCreateRate(Currency from, Currency to, LocalDate date); // Fixed typo
    }


for the MonetaryAmount, we could make a @Service factory

    @Service
    public class TransactionService {
        private final ExchangeRateService exchangeRateService;
        
        public Transaction createTransaction(...) {
            BigDecimal rate = exchangeRateService.getOrCreateRate(fromCurrency, toCurrency, date);
            MonetaryAmount amount = MonetaryAmount.of(nativeAmount, portfolioCurrency, rate, date);
            // ... continue with transaction creation
        }
    }



1. User creates transaction -> TransactionService.createTransaction()
2. service gets exchange rate -> exchangeRateSErvice.getOrCreateRate()
3. service creates pure value object -> MonetaryAmoutn.of()
4. Transaction aggregate uses purse domain obejcts




----- 
this is the new design

    Transaction (Aggregate Root)
    ├── details: TransactionDetails
    ├── fees: List<Fee>
    └── getTotalCashImpact() // Combines details.getNet() - totalFees()

    TransactionDetails (Abstract)
    ├── AccountTransactionDetails
    │   └── getNetAmount() // gross - withholding/taxes
    ├── TradeTransactionDetails  
    │   └── getNetProceeds() // shares * price - commission built into price
    └── ReversalTransactionDetails
        └── getReversalAmount() // amount being reversed


to get the 'cashImpact' we subtract the fees from 'net'

-----

So to sum this up because you are going to forget
Every Fee has a conversion object, which is typically used for fee's native currency to portfolio currency conversion
This is for calcualting net cash impact and general conversion safety

the philosophy with Fee and AccountEffect + others is "store the conversion at creatoin time so later we don't need to recalc FX"
so that means:
- always construct MonetaryAmoutn with native + conversion-to-portfolio
getConversionAmount just returns the portfolio value, excatly what you want for accounting and net cash impact

Example

    Gross Value (USD 1000)
        │
        ▼
    CurrencyConversion → 1360 CAD
        │
        ▼
    ┌─────────────┐      ┌─────────────┐
    │ Broker Fee  │      │ Reg Fee     │
    │ 5 USD       │      │ 1.25 CAD    │
    │ → 6.80 CAD  │      │ → 1.25 CAD  │
    └─────────────┘      └─────────────┘
        │                    │
        ▼                    ▼
    Total Fees in CAD = 8.05 CAD
        │
        ▼
    Net Cash Impact = 1351.95 CAD


-----
Domain Layer

Portfolio (Aggregate Root)
Transaction (Aggregate Root)
Holdings (Value Object)
AssetPosition (Value Object)
Money (Value Object)
Quantity (Value Object)
AssetId (Value Object)
PortfolioId (Value Object)
TransactionId (Value Object)
UserId (Value Object)
TransactionType (Enum)
TransactionStatus (Enum)
AssetType (Enum)
TransactionSource (Enum)
PricingService (Domain Service)
CurrencyConversionService (Domain Service)
PortfolioValuationService (Domain Service)
SecurityPurchasedEvent (Domain Event)
SecuritySoldEvent (Domain Event)
CashDepositedEvent (Domain Event)
CashWithdrawnEvent (Domain Event)
DividendReceivedEvent (Domain Event)
TransactionReversedEvent (Domain Event)
InsufficientFundsException (Domain Exception)
AssetNotHeldException (Domain Exception)
InvalidQuantityException (Domain Exception)
TransactionAlreadyReversedException (Domain Exception)
CurrencyMismatchException (Domain Exception)

Application Layer

BuySecurityCommand (Command)
SellSecurityCommand (Command)
DepositCashCommand (Command)
WithdrawCashCommand (Command)
RecordDividendCommand (Command)
PortfolioApplicationService (Application Service)
TransactionApplicationService (Application Service)
PortfolioRepository (Interface)
TransactionRepository (Interface)

Infrastructure Layer

JpaPortfolioRepository (Repository Implementation)
JpaTransactionRepository (Repository Implementation)
ExternalPricingService (Domain Service Implementation)
CurrencyExchangeApiService (Domain Service Implementation)
EventPublisher (Event Publishing)
DatabaseConfiguration
EventBusConfiguration


# Portfolio Management Domain Model

## Aggregates

### Portfolio (Aggregate Root)
**Fields:**
- `portfolioId: PortfolioId`
- `userId: UserId`
- `name: String`
- `description: String`
- `cashBalance: Money`
- `holdings: Holdings`
- `version: long`
- `createdAt: Instant`
- `lastModifiedAt: Instant`

**Methods:**
- `buySecurity(BuySecurityCommand): void`
- `sellSecurity(SellSecurityCommand): void`
- `depositCash(DepositCashCommand): void`
- `withdrawCash(WithdrawCashCommand): void`
- `recordDividend(RecordDividendCommand): void`
- `updateDetails(String name, String description): void`
- `getCashBalance(): Money`
- `getHoldings(): Holdings`
- `getTotalValue(PricingService): Money`
- `getUncommittedEvents(): List<DomainEvent>`
- `markEventsAsCommitted(): void`

### Transaction (Aggregate Root)
**Fields:**
- `transactionId: TransactionId`
- `portfolioId: PortfolioId`
- `type: TransactionType`
- `status: TransactionStatus`
- `amount: Money`
- `details: TransactionDetails`
- `executedAt: Instant`
- `reversedAt: Instant`
- `reverseReason: String`

**Methods:**
- `reverse(String reason): void`
- `isReversed(): boolean`
- `canBeReversed(): boolean`

## Value Objects

### Holdings
**Fields:**
- `positions: Map<AssetId, AssetPosition>`

**Methods:**
- `addPosition(AssetId, Quantity, Money): void`
- `removePosition(AssetId, Quantity): void`
- `getPosition(AssetId): Optional<AssetPosition>`
- `getAllPositions(): List<AssetPosition>`
- `isEmpty(): boolean`

### AssetPosition
**Fields:**
- `assetId: AssetId`
- `quantity: Quantity`
- `averageCostBasis: Money`
- `totalCostBasis: Money`
- `acquiredAt: Instant`

**Methods:**
- `addToPosition(Quantity, Money): AssetPosition`
- `removeFromPosition(Quantity): AssetPosition`
- `getUnrealizedGainLoss(Money currentPrice): Money`
- `isEmpty(): boolean`

### Money
**Fields:**
- `amount: BigDecimal`
- `currency: Currency`

**Methods:**
- `add(Money): Money`
- `subtract(Money): Money`
- `multiply(BigDecimal): Money`
- `divide(BigDecimal): Money`
- `negate(): Money`
- `isPositive(): boolean`
- `isNegative(): boolean`
- `isZero(): boolean`
- `compareTo(Money): int`
- `ZERO(Currency): Money`

### Quantity
**Fields:**
- `value: BigDecimal`

**Methods:**
- `add(Quantity): Quantity`
- `subtract(Quantity): Quantity`
- `multiply(BigDecimal): Quantity`
- `isPositive(): boolean`
- `isZero(): boolean`
- `compareTo(Quantity): int`

### AssetId
**Fields:**
- `symbol: String`
- `exchange: String`
- `assetType: AssetType`
- `currency: Currency`

**Methods:**
- `toString(): String`
- `equals(Object): boolean`
- `hashCode(): int`

## Identifiers

### PortfolioId
**Fields:**
- `value: UUID`

**Methods:**
- `createRandom(): PortfolioId`
- `fromString(String): PortfolioId`
- `toString(): String`

### TransactionId
**Fields:**
- `value: UUID`

**Methods:**
- `createRandom(): TransactionId`
- `fromString(String): TransactionId`
- `toString(): String`

### UserId
**Fields:**
- `value: UUID`

**Methods:**
- `createRandom(): UserId`
- `fromString(String): UserId`
- `toString(): String`

## Commands

### BuySecurityCommand
**Fields:**
- `portfolioId: PortfolioId`
- `assetId: AssetId`
- `quantity: Quantity`
- `pricePerUnit: Money`
- `totalCost: Money`
- `fees: Money`
- `transactionDate: TransactionDate`
- `source: TransactionSource`
- `description: String`

### SellSecurityCommand
**Fields:**
- `portfolioId: PortfolioId`
- `assetId: AssetId`
- `quantity: Quantity`
- `pricePerUnit: Money`
- `totalProceeds: Money`
- `fees: Money`
- `transactionDate: TransactionDate`
- `source: TransactionSource`
- `description: String`

### DepositCashCommand
**Fields:**
- `portfolioId: PortfolioId`
- `amount: Money`
- `transactionDate: TransactionDate`
- `source: TransactionSource`
- `description: String`

### WithdrawCashCommand
**Fields:**
- `portfolioId: PortfolioId`
- `amount: Money`
- `transactionDate: TransactionDate`
- `source: TransactionSource`
- `description: String`

### RecordDividendCommand
**Fields:**
- `portfolioId: PortfolioId`
- `assetId: AssetId`
- `amount: Money`
- `paymentDate: TransactionDate`
- `source: TransactionSource`
- `description: String`

## Events

### SecurityPurchasedEvent
**Fields:**
- `portfolioId: PortfolioId`
- `assetId: AssetId`
- `quantity: Quantity`
- `totalCost: Money`
- `pricePerUnit: Money`
- `occurredAt: Instant`

### SecuritySoldEvent
**Fields:**
- `portfolioId: PortfolioId`
- `assetId: AssetId`
- `quantity: Quantity`
- `totalProceeds: Money`
- `realizedGainLoss: Money`
- `occurredAt: Instant`

### CashDepositedEvent
**Fields:**
- `portfolioId: PortfolioId`
- `amount: Money`
- `occurredAt: Instant`

### CashWithdrawnEvent
**Fields:**
- `portfolioId: PortfolioId`
- `amount: Money`
- `occurredAt: Instant`

### DividendReceivedEvent
**Fields:**
- `portfolioId: PortfolioId`
- `assetId: AssetId`
- `amount: Money`
- `occurredAt: Instant`

### TransactionReversedEvent
**Fields:**
- `transactionId: TransactionId`
- `portfolioId: PortfolioId`
- `reverseReason: String`
- `occurredAt: Instant`

## Enums

### TransactionType
- `BUY`
- `SELL` 
- `DEPOSIT`
- `WITHDRAWAL`
- `DIVIDEND`
- `REVERSAL`

### TransactionStatus
- `PENDING`
- `COMPLETED`
- `REVERSED`
- `FAILED`

### AssetType
- `STOCK`
- `BOND`
- `ETF`
- `MUTUAL_FUND`
- `OPTION`
- `FUTURE`

### TransactionSource
- `MANUAL_ENTRY`
- `BROKER_IMPORT`
- `API_INTEGRATION`
- `DIVIDEND_REINVESTMENT`

## Domain Services

### PricingService
**Methods:**
- `getCurrentPrice(AssetId): Money`
- `getHistoricalPrice(AssetId, LocalDate): Money`
- `getPricesForPortfolio(Portfolio): Map<AssetId, Money>`

### CurrencyConversionService
**Methods:**
- `convert(Money, Currency): Money`
- `getExchangeRate(Currency from, Currency to): BigDecimal`

### PortfolioValuationService
**Methods:**
- `calculateTotalValue(Portfolio, PricingService): Money`
- `calculateUnrealizedGainLoss(Portfolio, PricingService): Money`
- `calculatePositionValue(AssetPosition, Money price): Money`

## Exceptions

### InsufficientFundsException
### AssetNotHeldException
### InvalidQuantityException
### TransactionAlreadyReversedException
### CurrencyMismatchException

## Application Services

### PortfolioApplicationService
**Methods:**
- `createPortfolio(CreatePortfolioCommand): PortfolioId`
- `buySecurity(BuySecurityCommand): void`
- `sellSecurity(SellSecurityCommand): void`
- `depositCash(DepositCashCommand): void`
- `withdrawCash(WithdrawCashCommand): void`
- `recordDividend(RecordDividendCommand): void`

### TransactionApplicationService
**Methods:**
- `reverseTransaction(ReverseTransactionCommand): void`
- `getTransactionHistory(PortfolioId, DateRange): List<Transaction>`

## Repositories

### PortfolioRepository
**Methods:**
- `findById(PortfolioId): Optional<Portfolio>`
- `save(Portfolio): void`
- `findByUserId(UserId): List<Portfolio>`

### TransactionRepository
**Methods:**
- `findById(TransactionId): Optional<Transaction>`
- `save(Transaction): void`
- `findByPortfolioId(PortfolioId): List<Transaction>`
- `findByPortfolioIdAndDateRange(PortfolioId, DateRange): List<Transaction>`


Rule of thumb:

If it’s a core invariant of the aggregate (like status, type, netImpact), it belongs in Transaction.

If it’s detail-specific (like realized G/L), keep it in the detail — unless you need a façade for convenience.



example of revesring a transaction

// in portfolio class
d in Portfolio:

public Transaction reverseTransaction(Transaction original, String reason, Instant at) {
    // Create new reversal transaction
    Transaction reversal = new Transaction(
        TransactionId.createRandom(),
        original.getTransactionId(), // parent link
        original.getCorrelationId(),
        this.portfolioId,
        CashTransactionType.REVERSAL, // type
        TransactionStatus.COMPLETED,
        new ReversalDetails(reason, at),
        at,
        original.getTransactionNetImpact().negate(), // undo impact
        null, // no gain/loss
        at,
        at
    );

    // Mark the original as reversed
    original.markReversed(reversal.getTransactionId(), at);

    // Add reversal to portfolio
    this.transactions.add(reversal);

    return reversal;
}


so for the portfolio and transaction class interactions, instead of portfolio holding Transaction objects,
they hold TransactionIds 



-----

Currency conversion stuff

This is a complex scenario that involves multi-currency ACB calculations. Here's how it works for Canadian tax purposes:
Canadian ACB Rules for Foreign Currency:
For Canadian tax purposes, everything must be converted to CAD at the time of transaction, including fees.
The Process:

Convert USD stock price to CAD (using Bank of Canada rate on transaction date)
Add CAD fees directly (no conversion needed)
Calculate ACB in CAD

Example:
Buy 100 shares of AAPL at $150 USD
- Exchange rate: 1 USD = 1.35 CAD
- Stock cost: $150 USD × 1.35 = $202.50 CAD per share
- CAD fees: $9.95 CAD
- Total cost per share: ($202.50 × 100 + $9.95) ÷ 100 = $202.60 CAD per share
Implementation Considerations:
Your Current Design Challenge:
Your AssetHolding assumes single currency (USD for US stocks), but ACB requires CAD calculations.
Potential Solutions:
Option 1: ACB-Specific Calculation Layer
java// Keep AssetHolding in native currency (USD)
// Have separate ACB calculator that converts to CAD
public class ACBCalculator {
    public Money calculateACBInCAD(AssetHolding holding, ExchangeRateService rates) {
        // Convert USD holding to CAD for tax purposes
    }
}
Option 2: Dual Currency Holdings
javapublic class AssetHolding {
    private Money averageCostBasis; // Native currency (USD)
    private Money acbCostBasis;     // Tax currency (CAD)
    // Keep both for different purposes
}
Option 3: Tax Jurisdiction Strategy
javapublic interface TaxCalculationStrategy {
    Money calculateCostBasis(List<Transaction> transactions, Currency reportingCurrency);
}

public class CanadianACBStrategy implements TaxCalculationStrategy {
    // Handles CAD conversion logic
}
My Recommendation:
For your MVP, keep it simple:

Store holdings in native currency (USD for US stocks)
Handle ACB conversion at the application layer when needed for tax reports
Convert fees to stock currency before calling increasePosition()

java// Application layer handles currency conversion
Money usdFees = fxService.convert(cadFees, CAD, USD, transactionDate);
Money totalUSDPricePerShare = usdPrice.add(usdFees.divide(quantity));
holding.increasePosition(quantity, totalUSDPricePerShare, date);
This keeps your domain model clean while handling the complexity at the right layer. You can add proper multi-currency ACB support later when you have more Canadian users who need detailed tax reporting.
The key insight: ACB is a tax reporting concern, not necessarily a portfolio tracking concern. Your core holdings can be in native currency, with ACB calculations happening when generating tax reports.



going down the app layer route for conversion

The Problems with Your Current Approach:
1. Redundant Conversions
You're doing the same conversion twice:

Once in MonetaryAmount (native → portfolio currency)
Again in app layer (native → asset currency)

2. Tight Coupling
Your Fee is tightly coupled to portfolio currency concepts, making it less reusable.
3. Complexity Without Clear Benefit
The portfolio currency conversion might not even be needed if you're tracking cash balances differently.
Simpler Alternatives:
Option 1: Just Use Money
javapublic record Fee(
    FeeType type,
    Money amount,        // Just the native amount (CAD $9.95)
    String description,
    Map<String, String> metadata,
    Instant time
) {}
Convert at the application layer when needed:
javaMoney portfolioImpact = currencyService.convert(fee.amount(), portfolioCurrency, date);
Money acbImpact = currencyService.convert(fee.amount(), assetCurrency, date);
Option 2: Conversion on Demand
javapublic record Fee(
    FeeType type,
    Money nativeAmount,
    String description,
    Map<String, String> metadata,
    Instant time
) {
    public Money convertTo(Currency targetCurrency, CurrencyService service, Instant date) {
        return service.convert(nativeAmount, targetCurrency, date);
    }
}
When Your Approach Makes Sense:
Only if you have a specific need to cache the portfolio conversion for performance reasons, or if the portfolio currency conversion happens at a different exchange rate than real-time (like end-of-day rates vs transaction-time rates).
My Recommendation:
Drop the MonetaryAmount complexity and just use Money nativeAmount. Convert as needed at the application layer. This is simpler, more flexible, and follows the principle of doing conversions where they're needed rather than preemptively.
Your current design is over-engineering a solution to a problem that might not exist.

-----
October 28th, 2025 goals
- Build the app


~~~
    type Transaction = {
        date: Date;
        ticker: string;      // "AAPL" or "CASH"
        shares: number;      // positive = buy, negative = sell
        price: number;       // price per share
        notes: string;       // "dividend", "sold half position", etc.
        }

That's it. You can derive EVERYTHING else from this:

 - Current holdings? Sum up shares by ticker
 - Cost basis? Calculate from historical buys
 - Realized gains? Compare buy/sell prices
 - Portfolio value? Multiply current shares × current price


Phase 1: The Spike (1 week)
Build the absolute simplest thing that works:

Hardcode your actual portfolio data in a TypeScript file
Create a single page that displays it
Deploy it
USE IT for tracking your portfolio for 1 week

Phase 2: Learn From Reality (1 week)
After using it, you'll discover:

"Damn, I need to edit transactions because I made a typo"
"I want to see my gains on this position"
"I need to track dividends separately"

These are REAL problems, not hypothetical ones.
Phase 3: Solve Real Problems (2 weeks)
Now add features based on what you actually needed:

Add edit functionality
Add the gains calculation you actually wanted
Add that dividend tracking

Repeat
Each cycle, use the app for a week, find the most annoying missing feature, add ONLY that.


if ever in teh ftureu we want brokerage integration
1. mark transation as soruce: manual vs source: brokerage and only allow editing/deleting for manual, brokerage is read-only

~~~

Data flow

USER CONTEXT
- A 'user' has authentication credentials and profile info

PORTFOLIO MANAGEMENT CONTEXT  
- A 'user' has ONE 'portfolio'
- A 'portfolio' contains multiple 'accounts'
- An 'account' holds multiple 'assets'
- A 'portfolio' records 'transactions'
- A 'transaction' modifies 'assets' within an 'account'
- A 'portfolio' can have 'liabilities'

GOALS & FINANCIAL PLANNING CONTEXT
- A 'user' defines multiple 'goals'
- A 'goal' can have multiple 'milestones'
- A 'goal' tracks progress against 'portfolio' value (integration point)

COMMUNITY & SOCIAL CONTEXT
- A 'member' (user in social context) creates 'posts'
- A 'post' can have multiple 'comments'
- 'comments' can be nested (replies)


----------- 
we can probably break the portfolio_management into smaller parts and work on those each... probably just going to leave it


  const [formData, setFormData] = useState({
    userId: "f8db6ee5-561e-4631-ba62-c4944a9ff983",
    creationDateTime: "",
    type: "BUY",
    ticker: "",
    quantity: "",
    amount: ""
  });

above is a Transaction, we need to make that
userId -> User
creationDateTime -> Instant, use that for everything
type -> Enum Type
Ticker -> some class that has that as the UUID and probably their identitifer (now thinking about it... if this is for the future,
need to be flexible in some ways we should call this identifier and branch off from there, but portfolio usually means stock so...
we should use that number -> ISIN),
quantity -> BigDecimal
Amount -> Money


Transaction V2
    AccountId (which has PortfolioId which has UserId)
    TransactionId
    creationTimestamp
    transctionType 
    AssetIdentifier
    Quantity
    PricePerUnit
    Fee (should be a list)
    Amount (this can be implied, which is QTY * PPU + FEE), Crypto might be an issue... with how 100 worth of BTC isn't really thanks to NETWORK
        for FIAT do AssetValue (QTY * PPU) + FEE
        for Crypto, we would need the original amount spent... or okay we do this, we first get price with no fees, then subtract fees
            and from there we do the subtract amount / original amount to get the QTY


probably not going to do domain events, don't really get them, seems simple to add if needed


-----
## Summary: Integration Flow
```
Controller/API Layer
    ↓
Application Service (orchestrates use cases)
    ↓
Domain Service (complex business logic)
    ↓
Portfolio Aggregate (domain model)
    ↓
Repository (persistence)

## The Flow
```
Application Service
    ↓ (calls)
Domain Service (PortfolioValuationService)
    ↓ (calls)
Portfolio.getAccounts()
    ↓ (iterates and calls)
Account.calculateTotalValue()
    ↓ (calls)
Asset.calculateCurrentValue()

```
@Configuration
public class DomainServiceConfiguration {
    
    @Bean
    public PortfolioValuationService portfolioValuationService() {
        return new PortfolioValuationService();
    }
    
    @Bean
    public PerformanceCalculationService performanceCalculationService() {
        return new PerformanceCalculationService();
    }
    
    @Bean
    public AssetAllocationService assetAllocationService(
        PortfolioValuationService valuationService
    ) {
        return new AssetAllocationService(valuationService);
    }
}
```
How Domain Services Work
Domain Services are called by either:

Application Services (use case orchestration)
Portfolio itself (when Portfolio needs complex calculations)
Other parts of the domain layer

They don't have their own lifecycle - they're just utility classes with business logic that doesn't naturally belong to a single entity.


-----
for the application layer, we use DTOS for input and output

## What to build in the application layer
1. Use Case Services (Command Handlers)
Portfolio Management Use Cases

- RecordAssetPurchaseUseCase - Handles buying new assets; orchestrates creating a BUY transaction, updating portfolio holdings, and persisting changes.
- RecordAssetSaleUseCase - Handles selling assets; creates a SELL transaction, calculates realized gains/losses, updates holdings, and persists.
- RecordDepositUseCase - Handles cash deposits; creates a DEPOSIT transaction, updates cash holdings in the specified account.
- RecordWithdrawalUseCase - Handles cash withdrawals; creates a WITHDRAWAL transaction, reduces cash holdings, ensures sufficient balance.
- RecordDividendIncomeUseCase - Handles dividend/interest income; creates DIVIDEND/INTEREST transaction, increases cash holdings.
- RecordFeeUseCase - Handles transaction fees; creates FEE transaction, deducts from cash holdings.
- AddNewAccountUseCase - Creates new accounts (TFSA, RRSP, etc.); validates account type and adds to portfolio.
- RemoveAccountUseCase - Removes empty accounts; validates account has no assets before deletion.

Query Services (Read Operations)

- ViewNetWorthQuery - Retrieves current net worth; fetches portfolio, queries market data service for current prices, calculates total value.
- ViewPortfolioPerformanceQuery - Displays performance metrics; uses PerformanceCalculationService to calculate returns, gains/losses over specified period.
- AnalyzeAssetAllocationQuery - Shows asset distribution; uses AssetAllocationService to breakdown by type, account, or currency.
- GetTransactionHistoryQuery - Retrieves transaction list; fetches and filters transactions by date range or type.
- GetAccountSummaryQuery - Shows individual account details; retrieves specific account with holdings and calculated values.

2. Command DTOs (Input Objects)

- RecordPurchaseCommand - Contains userId, accountId, symbol, quantity, price, fee, date, notes; represents all data needed to record a purchase.
- RecordSaleCommand - Contains userId, accountId, symbol, quantity, price, fee, date, notes; represents all data needed to record a sale.
- RecordDepositCommand - Contains userId, accountId, amount, currency, date, notes; represents deposit transaction data.
- RecordWithdrawalCommand - Contains userId, accountId, amount, currency, date, notes; represents withdrawal transaction data.
- RecordIncomeCommand - Contains userId, accountId, symbol, amount, incomeType (dividend/interest), date; represents passive income.
- AddAccountCommand - Contains userId, accountName, accountType, baseCurrency; represents new account creation.
- RemoveAccountCommand - Contains userId, accountId; represents account deletion request.

3. Query DTOs (Input for Queries)

- ViewNetWorthQuery - Contains userId, optional asOfDate; specifies which portfolio and point in time.
- ViewPerformanceQuery - Contains userId, startDate, endDate, optional accountId; defines performance calculation parameters.
- AnalyzeAllocationQuery - Contains userId, allocationType (by type/account/currency); specifies how to breakdown allocation.
- GetTransactionHistoryQuery - Contains userId, optional startDate, endDate, transactionType, accountId; filters transaction list.
- GetAccountSummaryQuery - Contains userId, accountId; identifies which account to retrieve.

4. Response DTOs (Output Objects)

- PortfolioResponse - Contains portfolioId, userId, accounts[], transactionCount, lastUpdated; represents complete portfolio state.
- NetWorthResponse - Contains totalAssets, totalLiabilities, netWorth, asOfDate, currency; represents calculated net worth.
- PerformanceResponse - Contains totalReturn, realizedGains, unrealizedGains, timeWeightedReturn, period; represents performance metrics.
- AllocationResponse - Contains Map of category → percentage/value; represents asset distribution breakdown.
- TransactionResponse - Contains transactionId, type, symbol, quantity, price, fee, date, notes; represents single transaction.
- TransactionHistoryResponse - Contains List<TransactionResponse>, totalCount, dateRange; represents paginated transaction list.
- AccountResponse - Contains accountId, name, type, assets[], totalValue, currency; represents single account with holdings.

5. Exception Classes

- PortfolioNotFoundException - Thrown when portfolio doesn't exist for given userId; used in all use cases.
- AccountNotFoundException - Thrown when specified account doesn't exist; used in transaction recording.
- InsufficientFundsException - Thrown when withdrawal/purchase exceeds available cash; used in withdrawal and purchase use cases.
- InsufficientHoldingsException - Thrown when selling more shares than owned; used in RecordAssetSaleUseCase.
- InvalidTransactionException - Thrown when transaction data is invalid; used for business rule violations.
- AccountNotEmptyException - Thrown when trying to delete account with assets; used in RemoveAccountUseCase.

6. Validation Classes

- CommandValidator - Validates command DTOs before processing; checks for null values, valid dates, positive amounts, valid symbols.
- QuantityValidator - Validates quantity values; ensures positive non-zero values for purchases/sales.
- DateValidator - Validates transaction dates; ensures not in future, within reasonable historical range.

7. Mapper Classes

- PortfolioMapper - Converts Portfolio entity → PortfolioResponse DTO; handles nested account and asset conversions.
- TransactionMapper - Converts Transaction entity → TransactionResponse DTO; maps all transaction fields.
- AccountMapper - Converts Account entity → AccountResponse DTO; includes asset summaries.
- AllocationMapper - Converts allocation calculations → AllocationResponse DTO; formats percentages and categories.

8. Application Service Interfaces (Optional but Recommended)

- IPortfolioManagementService - Interface defining all portfolio use cases; allows for easier testing and dependency injection.
- IPortfolioQueryService - Interface defining all query operations; separates read from write operations (CQRS pattern).



--- Dec 14th --- th DRIP issue
TLDR: we can't drip when we record a dividend in the application layer
The solution is to add a isDrip feild in the Transaction.java and impelment logic in the Account.java 


--- dec 17th --- updates
- fixed drip
- did both the services layer code, need to check if i impelmented the other stuff
- unit test after

- THEN ONTO INFRA

--- finished testing on dec 30th --- 
we will now work on infra

phase 1 -> db/presistence layer (db config) ✅
step 1 -> create the db
step 2 -> set up the Entities
step 4-> set up JPA repo interfaces
step 4 -> create an entity mapper (domain to jpa entity)
step 5 -> implement repo interfaces (from domain layer)
step 6 -> integration test with full persitence layer


^ this is all under step one

new note, we will be using flyway, basically from what i read it is a dev tool used in conjuction with JPA.
Flyway will run first to ensure the tables exist, this is the standard table, as in this is what we expect.
Then JPA will look for an entity, and see that if it matches or not.

But what is Flyway? -> git for DB, we don't manually run SQL commands to creating or updating tables, we
 write 'migrations script' and Flyway auto executes them in order when your app starts


phase 2: External API integration (MarketDataService) ✅ 🟨
phase 3: REST Controllers (HTTP endpoints) 🟨
phase 4: Authentication (Supabase integration)


--- jan 02  2026
Repositories	@DataJpaTest	SQL/HQL correctness, Filters, Pagination.
Mappers	        JUnit 5	        Field matching, List conversion, Null handling.
Entities	    JUnit 5	        Validation rules, ID equality, Basic constraints



----
TODO: Look into how we can add AI agents into the applications


----- jan 16 2026 ---
known issue, i think i was confusing how hte web controller interacts with the data 
coming from the api. from what i see we are doing some duplicates sets, mainly the
market data dto mapper and assetinfo response...


also, if we want 'chart info' we need to include the yahoo chart stuff, might put that off for a minute though

we should also 'redis' the information as well as long term store that history data

[
	{
		"symbol": "AAPL",
		"name": "Apple Inc.",
		"price": 232.8,
		"changePercentage": 2.1008,
		"change": 4.79,
		"volume": 44489128,
		"dayLow": 226.65,
		"dayHigh": 233.13,
		"yearHigh": 260.1,
		"yearLow": 164.08,
		"marketCap": 3500823120000,
		"priceAvg50": 240.2278,
		"priceAvg200": 219.98755,
		"exchange": "NASDAQ",
		"open": 227.2,
		"previousClose": 228.01,
		"timestamp": 1738702801
	}
]


[
  {
    "symbol": "AAPL",
    "price": 255.53,
    "marketCap": 3775801225282,
    "beta": 1.093,
    "lastDividend": 1.03,
    "range": "169.21-288.62",
    "change": -2.68,
    "changePercentage": -1.03792,
    "volume": 70054453,
    "averageVolume": 45960616,
    "companyName": "Apple Inc.",
    "currency": "USD",
    "cik": "0000320193",
    "isin": "US0378331005",
    "cusip": "037833100",
    "exchangeFullName": "NASDAQ Global Select",
    "exchange": "NASDAQ",
    "industry": "Consumer Electronics",
    "website": "https://www.apple.com",
    "description": "Apple Inc. designs, manufactures, and markets smartphones, personal computers, tablets, wearables, and accessories worldwide. The company offers iPhone, a line of smartphones; Mac, a line of personal computers; iPad, a line of multi-purpose tablets; and wearables, home, and accessories comprising AirPods, Apple TV, Apple Watch, Beats products, and HomePod. It also provides AppleCare support and cloud services; and operates various platforms, including the App Store that allow customers to discover and download applications and digital content, such as books, music, video, games, and podcasts, as well as advertising services include third-party licensing arrangements and its own advertising platforms. In addition, the company offers various subscription-based services, such as Apple Arcade, a game subscription service; Apple Fitness+, a personalized fitness service; Apple Music, which offers users a curated listening experience with on-demand radio stations; Apple News+, a subscription news and magazine service; Apple TV+, which offers exclusive original content; Apple Card, a co-branded credit card; and Apple Pay, a cashless payment service, as well as licenses its intellectual property. The company serves consumers, and small and mid-sized businesses; and the education, enterprise, and government markets. It distributes third-party applications for its products through the App Store. The company also sells its products through its retail and online stores, and direct sales force; and third-party cellular network carriers, wholesalers, retailers, and resellers. Apple Inc. was founded in 1976 and is headquartered in Cupertino, California.",
    "ceo": "Timothy D. Cook",
    "sector": "Technology",
    "country": "US",
    "fullTimeEmployees": "164000",
    "phone": "(408) 996-1010",
    "address": "One Apple Park Way",
    "city": "Cupertino",
    "state": "CA",
    "zip": "95014",
    "image": "https://images.financialmodelingprep.com/symbol/AAPL.png",
    "ipoDate": "1980-12-12",
    "defaultImage": false,
    "isEtf": false,
    "isActivelyTrading": true,
    "isAdr": false,
    "isFund": false
  }
]


[
  {
    "symbol": "VFV.TO",
    "price": 171.34,
    "marketCap": 27584495386,
    "beta": 0.96,
    "lastDividend": 1.52767,
    "range": "121.61-172.18",
    "change": 0.14,
    "changePercentage": 0.0817757,
    "volume": 284321,
    "averageVolume": 280626,
    "companyName": "Vanguard S&P 500 Index ETF",
    "currency": "CAD",
    "cik": null,
    "isin": "CA92205Y1051",
    "cusip": "92205Y105",
    "exchangeFullName": "Toronto Stock Exchange",
    "exchange": "TSX",
    "industry": "Asset Management",
    "website": null,
    "description": "Vanguard S&P 500 Index ETF seeks to track, to the extent reasonably possible and before fees and expenses, the performance of a broad U.S. equity index that measures the investment return of large-capitalization U.S. stocks. Currently, this Vanguard ETF seeks to track the S&P 500 Index (or any successor thereto). It invests directly or indirectly primarily in stocks of U.S. companies.",
    "ceo": "",
    "sector": "Financial Services",
    "country": "CA",
    "fullTimeEmployees": null,
    "phone": null,
    "address": null,
    "city": null,
    "state": null,
    "zip": null,
    "image": "https://images.financialmodelingprep.com/symbol/VFV.TO.png",
    "ipoDate": "2012-11-08",
    "defaultImage": false,
    "isEtf": true,
    "isActivelyTrading": true,
    "isAdr": false,
    "isFund": false
  }
]


{
    "symbol": "VFV.TO",
    "name": "Vanguard S&P 500 Index ETF",
    "assetType": "ETF",
    "currency": "$",
    "exchange": "TSX",
    "currentPrice": null,
    "sector": "Financial Services",
    "marketCap": null,
    "peRatio": null,
    "fiftyTwoWeekHigh": null,
    "fiftyTwoWeekLow": null,
    "averageVolume": null,
    "source": "API CALL"
}
this is wrong, also the source is hard coded, we need to change that in the MarketAssetInfo in domain layer

---- jan 17th 26--- 
this is how the data flows from endpoint and back, or atleast how it should operate

endpoint/asset-info/example -> MarketDataController -> marketDataService (get AssetInfo) -> mapper.toProviderSymbol() (this is kind of a useless step), UPDATE ON THAT: we removed it, it was stupid -> providerFetchAssetInfo(...) -> our implementation of the MarketDataProvider int (i.e. FmpProvider) -> apiClient (fmpApiClient).fetchAssetInfo(...) -> a response:
    @Override
    public Optional<ProviderAssetInfo> fetchAssetInfo(String symbol) {
        FmpProfileResponse response = fmpApiClient.getProfile(symbol);

        return Optional.of(mapper.toProviderAssetInfo(response));
    }
    OR
    @Override
    public Optional<ProviderAssetInfo> fetchAssetInfo(String symbol) {
        FmpProfileResponse response = fmpApiClient.getProfile(symbol);

        return Optional.of(mapper.toProviderAssetInfo(response));
    }

-> conversion back to the intenral provider structure or either ProviderQuote or ProviderAssetInfo
-> back to the market data serivce impl -> converts to MarketAssetInfo (domain value object) via mapper.toAssetInfo(providerInfo)
-> MarketDataController -> AssetInfoResponse -> response entity


infrastructure/
├── config/
├── persistence/ (Repositories)
├── external/
│   ├── fmp/ (Financial Modeling Prep - specific provider)
│   │   ├── FmpApiClient.java
│   │   ├── FmpResponseMapper.java
│   │   ├── FmpProvider.java (Implements MarketDataProvider)
│   │   └── dtos/ (Provider-specific JSON/XML objects)
│   │       ├── FmpProfileResponse.java
│   │       └── FmpQuoteResponse.java
│   └── common/ (If you have multiple providers)
│       ├── ProviderQuote.java
│       └── ProviderAssetInfo.java
├── marketdata/
│   └── MarketDataServiceImpl.java (The orchestration logic)
└── exceptions/


infrastructure/
└── external/
    ├── marketdata/       <-- Capability 1
    │   ├── fmp/          (Implementation)
    │   └── MarketDataServiceImpl.java
    └── exchangerate/     <-- Capability 2
        ├── fixio/        (Implementation)
        └── ExchangeRateServiceImpl.java

TAG/OUTPUT[
  {
    "symbol": "AAPL",
    "name": "Apple Inc.",
    "price": 255.517,
    "changePercentage": -1.04295,
    "change": -2.693,
    "volume": 72142773,
    "dayLow": 254.93,
    "dayHigh": 258.9,
    "yearHigh": 288.62,
    "yearLow": 169.21,
    "marketCap": 3775609234912.9995,
    "priceAvg50": 271.5098,
    "priceAvg200": 234.05525,
    "exchange": "NASDAQ",
    "open": 257.88,
    "previousClose": 258.21,
    "timestamp": 1768597201
  }
]


[
  {
    "symbol": "AAPL",
    "name": "Apple Inc.",
    "price": 255.517,
    "changePercentage": -1.04295,
    "change": -2.693,
    "volume": 72142773,
    "dayLow": 254.93,
    "dayHigh": 258.9,
    "yearHigh": 288.62,
    "yearLow": 169.21,
    "marketCap": 3775609234912.9995,
    "priceAvg50": 271.5098,
    "priceAvg200": 234.05525,
    "exchange": "NASDAQ",
    "open": 257.88,
    "previousClose": 258.21,
    "timestamp": 1768597201
  }
]


2026-01-18T22:39:13.968-05:00 ERROR 23524 --- [fortunelink] [nio-8080-exec-5] c.l.f.p.i.e.marketdata.fmp.FmpProvider   : Failed to fetch current quote for symbol: AAPL from FMP

com.laderrco.fortunelink.portfolio_management.infrastructure.exceptions.FmpApiException: Failed to parse FMP quote response for AAPL
        at com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.FmpApiClient.getQuote(FmpApiClient.java:96) ~[classes/:na]
        at com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.FmpProvider.fetchCurrentQuote(FmpProvider.java:53) ~[classes/:na]
        at com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.MarketDataServiceImpl.getCurrentPrice(MarketDataServiceImpl.java:73) ~[classes/:na]
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
        at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
        at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:359) ~[spring-aop-6.2.7.jar:6.2.7]
        at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196) ~[spring-aop-6.2.7.jar:6.2.7]
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163) ~[spring-aop-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheInterceptor.lambda$invoke$0(CacheInterceptor.java:55) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheAspectSupport.invokeOperation(CacheAspectSupport.java:431) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheAspectSupport.evaluate(CacheAspectSupport.java:601) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheAspectSupport.execute(CacheAspectSupport.java:448) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheAspectSupport.execute(CacheAspectSupport.java:410) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheInterceptor.invoke(CacheInterceptor.java:65) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184) ~[spring-aop-6.2.7.jar:6.2.7]
        at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:728) ~[spring-aop-6.2.7.jar:6.2.7]
        at com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.MarketDataServiceImpl$$SpringCGLIB$$0.getCurrentPrice(<generated>) ~[classes/:na]
        at com.laderrco.fortunelink.portfolio_management.infrastructure.web.controllers.MarketDataController.getCurrentPrice(MarketDataController.java:70) ~[classes/:na]
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
        at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
        at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:258) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:191) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:118) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:986) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:891) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:979) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1014) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:903) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:564) ~[tomcat-embed-core-10.1.41.jar:6.0]
        at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:885) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:658) ~[tomcat-embed-core-10.1.41.jar:6.0]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:195) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:51) ~[tomcat-embed-websocket-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:108) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:108) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.web.FilterChainProxy.lambda$doFilterInternal$3(FilterChainProxy.java:231) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$FilterObservation$SimpleFilterObservation.lambda$wrap$1(ObservationFilterChainDecorator.java:479) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$AroundFilterObservation$SimpleAroundFilterObservation.lambda$wrap$1(ObservationFilterChainDecorator.java:340) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator.lambda$wrapSecured$0(ObservationFilterChainDecorator.java:82) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:128) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.access.intercept.AuthorizationFilter.doFilter(AuthorizationFilter.java:101) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.access.ExceptionTranslationFilter.doFilter(ExceptionTranslationFilter.java:125) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.access.ExceptionTranslationFilter.doFilter(ExceptionTranslationFilter.java:119) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.authentication.AnonymousAuthenticationFilter.doFilter(AnonymousAuthenticationFilter.java:100) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter.doFilter(SecurityContextHolderAwareRequestFilter.java:179) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.savedrequest.RequestCacheAwareFilter.doFilter(RequestCacheAwareFilter.java:63) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:107) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:93) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.header.HeaderWriterFilter.doHeadersAfter(HeaderWriterFilter.java:90) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal(HeaderWriterFilter.java:75) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.context.SecurityContextHolderFilter.doFilter(SecurityContextHolderFilter.java:82) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.context.SecurityContextHolderFilter.doFilter(SecurityContextHolderFilter.java:69) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal(WebAsyncManagerIntegrationFilter.java:62) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.session.DisableEncodeUrlFilter.doFilterInternal(DisableEncodeUrlFilter.java:42) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$AroundFilterObservation$SimpleAroundFilterObservation.lambda$wrap$0(ObservationFilterChainDecorator.java:323) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:224) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.FilterChainProxy.doFilterInternal(FilterChainProxy.java:233) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.FilterChainProxy.doFilter(FilterChainProxy.java:191) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.ServletRequestPathFilter.doFilter(ServletRequestPathFilter.java:52) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.CompositeFilter.doFilter(CompositeFilter.java:74) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration$CompositeFilterChainProxy.doFilter(WebSecurityConfiguration.java:319) ~[spring-security-config-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.handler.HandlerMappingIntrospector.lambda$createCacheFilter$3(HandlerMappingIntrospector.java:243) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.CompositeFilter.doFilter(CompositeFilter.java:74) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.config.annotation.web.configuration.WebMvcSecurityConfiguration$CompositeFilterChainProxy.doFilter(WebMvcSecurityConfiguration.java:240) ~[spring-security-config-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:362) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:278) ~[spring-web-6.2.7.jar:6.2.7]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.springframework.web.filter.ServerHttpObservationFilter.doFilterInternal(ServerHttpObservationFilter.java:114) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:167) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:90) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:483) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:116) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:344) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:398) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:903) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1740) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1189) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:658) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:63) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at java.base/java.lang.Thread.run(Thread.java:1474) ~[na:na]
Caused by: com.fasterxml.jackson.databind.exc.MismatchedInputException: Unexpected token (START_OBJECT), expected VALUE_STRING: need String, Number of Boolean value that contains type id (for subtype of java.util.List)
 at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 2, column: 3]
        at com.fasterxml.jackson.databind.exc.MismatchedInputException.from(MismatchedInputException.java:59) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.DeserializationContext.wrongTokenException(DeserializationContext.java:1941) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.DeserializationContext.reportWrongTokenException(DeserializationContext.java:1726) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.jsontype.impl.AsArrayTypeDeserializer._locateTypeId(AsArrayTypeDeserializer.java:174) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.jsontype.impl.AsArrayTypeDeserializer._deserialize(AsArrayTypeDeserializer.java:98) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.jsontype.impl.AsArrayTypeDeserializer.deserializeTypedFromArray(AsArrayTypeDeserializer.java:55) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.deser.std.CollectionDeserializer.deserializeWithType(CollectionDeserializer.java:284) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.deser.impl.TypeWrappedDeserializer.deserialize(TypeWrappedDeserializer.java:74) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.deser.DefaultDeserializationContext.readRootValue(DefaultDeserializationContext.java:342) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.ObjectMapper._readMapAndClose(ObjectMapper.java:4971) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.ObjectMapper.readValue(ObjectMapper.java:3887) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.fasterxml.jackson.databind.ObjectMapper.readValue(ObjectMapper.java:3870) ~[jackson-databind-2.19.0.jar:2.19.0]
        at com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.fmp.FmpApiClient.getQuote(FmpApiClient.java:85) ~[classes/:na]
        ... 140 common frames omitted

2026-01-18T22:39:13.977-05:00 ERROR 23524 --- [fortunelink] [nio-8080-exec-5] c.l.f.p.i.e.GlobalExceptionHandler       : Market data service error: Symbol 'AAPL' not found

com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException: Symbol 'AAPL' not found
        at com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException.symbolNotFound(MarketDataException.java:51) ~[classes/:na]
        at com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.MarketDataServiceImpl.getCurrentPrice(MarketDataServiceImpl.java:76) ~[classes/:na]
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
        at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
        at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(AopUtils.java:359) ~[spring-aop-6.2.7.jar:6.2.7]
        at org.springframework.aop.framework.ReflectiveMethodInvocation.invokeJoinpoint(ReflectiveMethodInvocation.java:196) ~[spring-aop-6.2.7.jar:6.2.7]
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:163) ~[spring-aop-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheInterceptor.lambda$invoke$0(CacheInterceptor.java:55) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheAspectSupport.invokeOperation(CacheAspectSupport.java:431) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheAspectSupport.evaluate(CacheAspectSupport.java:601) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheAspectSupport.execute(CacheAspectSupport.java:448) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheAspectSupport.execute(CacheAspectSupport.java:410) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.cache.interceptor.CacheInterceptor.invoke(CacheInterceptor.java:65) ~[spring-context-6.2.7.jar:6.2.7]
        at org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:184) ~[spring-aop-6.2.7.jar:6.2.7]
        at org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor.intercept(CglibAopProxy.java:728) ~[spring-aop-6.2.7.jar:6.2.7]
        at com.laderrco.fortunelink.portfolio_management.infrastructure.external.marketdata.MarketDataServiceImpl$$SpringCGLIB$$0.getCurrentPrice(<generated>) ~[classes/:na]
        at com.laderrco.fortunelink.portfolio_management.infrastructure.web.controllers.MarketDataController.getCurrentPrice(MarketDataController.java:70) ~[classes/:na]
        at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104) ~[na:na]
        at java.base/java.lang.reflect.Method.invoke(Method.java:565) ~[na:na]
        at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:258) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:191) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:118) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:986) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:891) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:979) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1014) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:903) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:564) ~[tomcat-embed-core-10.1.41.jar:6.0]
        at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:885) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:658) ~[tomcat-embed-core-10.1.41.jar:6.0]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:195) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:51) ~[tomcat-embed-websocket-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:108) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:108) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.web.FilterChainProxy.lambda$doFilterInternal$3(FilterChainProxy.java:231) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$FilterObservation$SimpleFilterObservation.lambda$wrap$1(ObservationFilterChainDecorator.java:479) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$AroundFilterObservation$SimpleAroundFilterObservation.lambda$wrap$1(ObservationFilterChainDecorator.java:340) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator.lambda$wrapSecured$0(ObservationFilterChainDecorator.java:82) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:128) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.access.intercept.AuthorizationFilter.doFilter(AuthorizationFilter.java:101) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.access.ExceptionTranslationFilter.doFilter(ExceptionTranslationFilter.java:125) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.access.ExceptionTranslationFilter.doFilter(ExceptionTranslationFilter.java:119) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.authentication.AnonymousAuthenticationFilter.doFilter(AnonymousAuthenticationFilter.java:100) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter.doFilter(SecurityContextHolderAwareRequestFilter.java:179) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.savedrequest.RequestCacheAwareFilter.doFilter(RequestCacheAwareFilter.java:63) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:107) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.authentication.logout.LogoutFilter.doFilter(LogoutFilter.java:93) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.header.HeaderWriterFilter.doHeadersAfter(HeaderWriterFilter.java:90) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.header.HeaderWriterFilter.doFilterInternal(HeaderWriterFilter.java:75) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.context.SecurityContextHolderFilter.doFilter(SecurityContextHolderFilter.java:82) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.context.SecurityContextHolderFilter.doFilter(SecurityContextHolderFilter.java:69) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter.doFilterInternal(WebAsyncManagerIntegrationFilter.java:62) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:227) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.session.DisableEncodeUrlFilter.doFilterInternal(DisableEncodeUrlFilter.java:42) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.wrapFilter(ObservationFilterChainDecorator.java:240) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$AroundFilterObservation$SimpleAroundFilterObservation.lambda$wrap$0(ObservationFilterChainDecorator.java:323) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$ObservationFilter.doFilter(ObservationFilterChainDecorator.java:224) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.ObservationFilterChainDecorator$VirtualFilterChain.doFilter(ObservationFilterChainDecorator.java:137) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.FilterChainProxy.doFilterInternal(FilterChainProxy.java:233) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.security.web.FilterChainProxy.doFilter(FilterChainProxy.java:191) ~[spring-security-web-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.ServletRequestPathFilter.doFilter(ServletRequestPathFilter.java:52) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.CompositeFilter.doFilter(CompositeFilter.java:74) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration$CompositeFilterChainProxy.doFilter(WebSecurityConfiguration.java:319) ~[spring-security-config-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.servlet.handler.HandlerMappingIntrospector.lambda$createCacheFilter$3(HandlerMappingIntrospector.java:243) ~[spring-webmvc-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.CompositeFilter$VirtualFilterChain.doFilter(CompositeFilter.java:113) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.CompositeFilter.doFilter(CompositeFilter.java:74) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.security.config.annotation.web.configuration.WebMvcSecurityConfiguration$CompositeFilterChainProxy.doFilter(WebMvcSecurityConfiguration.java:240) ~[spring-security-config-6.5.0.jar:6.5.0]
        at org.springframework.web.filter.DelegatingFilterProxy.invokeDelegate(DelegatingFilterProxy.java:362) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.DelegatingFilterProxy.doFilter(DelegatingFilterProxy.java:278) ~[spring-web-6.2.7.jar:6.2.7]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.springframework.web.filter.ServerHttpObservationFilter.doFilterInternal(ServerHttpObservationFilter.java:114) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-6.2.7.jar:6.2.7]
        at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.2.7.jar:6.2.7]
        at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:164) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:140) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:167) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:90) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:483) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:116) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:344) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:398) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:903) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1740) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1189) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:658) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:63) ~[tomcat-embed-core-10.1.41.jar:10.1.41]
        at java.base/java.lang.Thread.run(Thread.java:1474) ~[na:na]

2026-01-18T22:39:14.012-05:00  WARN 23524 --- [fortunelink] [nio-8080-exec-5] .m.m.a.ExceptionHandlerExceptionResolver : Resolved [com.laderrco.fortunelink.portfolio_management.domain.exceptions.MarketDataException: Symbol 'AAPL' not found]