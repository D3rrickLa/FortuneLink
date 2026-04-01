# Project Issue Tracker

**1. Wrong @Id and @Version import in four JPA entities**
This is the most insidious bug in the whole file. Four entities import the Spring Data annotation instead of the JPA one:
java// WRONG — Spring Data annotation, Hibernate ignores it
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
java// CORRECT
import jakarta.persistence.Id;
import jakarta.persistence.Version;
```

Affected files: `FeeJpaEntity`, `PositionJpaEntity`, `RealizedGainJpaEntity`, `MarketAssetInfoJpaEntity`. Hibernate will not recognize the primary key on these entities. You'll get `IdentifierGenerationException` or `org.hibernate.MappingException` at startup. This is fatal.

---

```
**2. V3 migration will never run — filename uses single underscore**
V3_alighn_schema_with_domain_model.sql   ← wrong, plus a typo
Flyway's naming convention requires double underscores: V{version}__{description}.sql. With a single underscore Flyway cannot parse the version number and silently skips the file. Every column added in V3 — lifecycle_state, health_status, position_strategy, cash_delta_amount, cash_delta_currency, excluded, realized_gains table — does not exist in the database. Your application will throw column-not-found errors the first time any of those paths execute.
Rename to: V3__align_schema_with_domain_model.sql

**3. account_amount and account_amount_currency columns exist in FeeJpaEntity but in no migration**
```
java@Column(name = "account_amount", precision = 20, scale = 10)
private BigDecimal accountAmount;

@Column(name = "account_amount_currency", length = 3)
private String accountAmountCurrency;
```
These columns appear in none of your three migration files. With ddl-auto: validate, Hibernate will throw SchemaManagementException at startup because it cannot find these columns in transaction_fees. Add them to V3 (or a V4).

**4. Broken JPQL in JpaTransactionRepository — two queries that will never work**
```
java// WRONG — entity is named AccountJpaEntity, not AccountEntity
@Query("SELECT a.portfolio.id FROM AccountEntity a WHERE a.id = :accountId")
UUID findPortfolioIdByAccountId(@Param("accountId") UUID accountId);
java// WRONG — multiple problems (see below)
@Query("""
    SELECT t.account.id as accountId,
           t.execution.asset as symbol,
           ...
    FROM Transaction t
    JOIN t.fees f
    WHERE t.account.id IN :accountIds
      AND t.transactionType = 'BUY'
      AND t.metadata.exclusion IS NULL
    """)
List<FeeAggregationResult> sumBuyFeesByAccountAndSymbol(...);
```
The second query has five separate problems:

Entity is TransactionJpaEntity, not Transaction
t.account — TransactionJpaEntity has no account relationship, only a UUID accountId column
t.execution.asset — there is no execution embedded object; it's a flat executionSymbol String column
f.accountAmount.amount — FeeJpaEntity.accountAmount is a BigDecimal, not an embedded object
t.metadata.exclusion IS NULL — metadata is not a mapped field; exclusion is flat columns

The delete query has the same entity name problem:
java@Query("DELETE FROM Transaction t WHERE t.account.id = :accountId ...")
Both of these need to be completely rewritten as native SQL or corrected JPQL against the actual column names.

5. Jackson 2 vs Jackson 3 are mixed in RedisCacheConfig and the serializers
The serializers (CurrencySerializer, MarketAssetInfoSerializer, etc.) use tools.jackson — that's Jackson 3, which moved its packages from com.fasterxml.jackson to tools.jackson. Spring Boot 3.x ships with Jackson 2 (com.fasterxml.jackson). Meanwhile RedisCacheConfig imports:
javaimport com.fasterxml.jackson.annotation.JsonAutoDetect;  // Jackson 2
import tools.jackson.databind.ObjectMapper;              // Jackson 3
These are from different, incompatible library versions. GenericJacksonJsonRedisSerializer and JacksonJsonRedisSerializer from Spring Data Redis use Jackson 2. This will either fail to compile or throw ClassCastException / NoSuchMethodError at startup. Pick one version — Spring Boot's managed Jackson 2 is the right choice here.

6. MarketDataServiceImpl has no Spring stereotype — the bean doesn't exist
java@RequiredArgsConstructor
public class MarketDataServiceImpl implements MarketDataService {
Missing @Service. This class is never registered in the application context. Any injection point that wants MarketDataService will fail with NoSuchBeanDefinitionException. Add @Service.
Also, the @Value-annotated final fields with @RequiredArgsConstructor won't inject correctly:
java@Value("${fortunelink.cache.key-prefix.prices}")
private final String cacheKeyPrefix;
Lombok generates a constructor with these as parameters, but @Value is field injection — it doesn't get applied to constructor-injected fields through Lombok. The field will be whatever the constructor argument is (likely an empty string or zero). Use a @ConfigurationProperties bean instead, or remove final and use field injection without Lombok.

7. deleteAllExpiredTransactions deletes the wrong rows
java@Modifying
@Query("DELETE FROM TransactionJpaEntity t WHERE t.occurredAt < :cutoff")
int deleteAllExpiredTransactions(@Param("cutoff") Instant cutoff);
This deletes every transaction before the cutoff date, not just excluded ones. The domain contract (and TransactionPurgeService) intends to purge only excluded transactions past their retention window. This would silently destroy all historical transaction data older than a year for every user. The query should be:
java@Query("DELETE FROM TransactionJpaEntity t WHERE t.excluded = true AND t.excludedAt < :cutoff")

8. FortunelinkApplication — main is not public
javastatic void main(String[] args) {   // won't launch as a standard Java entry point
Must be public static void main(String[] args). The JVM requires it.

9. @Cacheable cache names don't match configured cache names
java@Cacheable(value = "${fortuneline.cache.key-prefix.asset-info}", ...)
// Note: "fortuneline" — missing the 'k'
Beyond the typo, @Cacheable's value attribute does not resolve ${...} property placeholders at runtime the way @Value does. The configured cache names in RedisCacheConfig are literal strings: "current-prices", "historical-prices", "asset-info", "trading-currency". These need to match exactly. Use:
java@Cacheable(cacheNames = "asset-info", key = "#symbol.symbol()")

🟡 Significant — Wrong Behavior or Serious Performance Problems

10. Realized gains get new UUIDs on every portfolio save — mass DELETE/INSERT on every write
javafor (RealizedGainRecord rg : domain.getRealizedGains()) {
    gainEntities.add(realizedGainToEntity(UUID.randomUUID(), entity, rg));  // new UUID every time
}
entity.replaceRealizedGains(gainEntities);  // clears and re-adds
replaceRealizedGains calls this.realizedGains.clear() then re-adds everything. Because orphanRemoval = true, Hibernate issues a DELETE for every existing row, then an INSERT for every record — with freshly randomized UUIDs. A user with 3 years of dividend history triggers hundreds of DELETE+INSERT pairs on every single portfolio save. The RealizedGainRecord value object needs a stable identifier, either added to the domain record or managed at the infrastructure level by matching on (accountId, symbol, occurredAt).

11. cash_balance_amount has scale 2 in V1 schema, scale 10 in the entity — you're silently rounding financial data
V1 migration:
sqlcash_balance_amount NUMERIC(20, 2) NOT NULL DEFAULT 0,
AccountJpaEntity:
java@Column(name = "cash_balance_amount", nullable = false, precision = 20, scale = 10)
private BigDecimal cashBalanceAmount;
PostgreSQL enforces the column's declared scale on write. Any precision beyond 2 decimal places is rounded silently. For assets like crypto where you might have $0.000001234 of cash from a dividend, you lose all meaningful precision. This needs to be NUMERIC(20, 10) in a new migration.
Same problem exists for cost_basis_amount NUMERIC(20, 2) in the assets table.

12. toDomain in TransactionDomainMapper will NPE on pre-V3 rows
javaCurrency cashCurrency = Currency.of(entity.getCashDeltaCurrency());  // null → NPE
Money cashDelta = new Money(entity.getCashDeltaAmount(), cashCurrency);
cash_delta_amount and cash_delta_currency were added nullable in V3. Any transaction recorded before V3 runs (or if V3 never runs due to the naming bug above) will have null values here. Currency.of(null) will throw. Add null guards with a sensible fallback.

13. TransactionRecordingServiceImpl.recordInterest takes a non-null symbol but the interface says nullable
The interface contract:
java/**
 * Records interest earned on an asset position.
 * For cash/account-level interest, symbol may be null.
 */
Transaction recordInterest(Account account, AssetSymbol symbol, Money amount, ...);
The implementation:
javaObjects.requireNonNull(symbol, "symbol cannot be null");
This contradicts the domain contract, breaks cash interest (RecordInterestCommand.cashInterest()), and would throw every time someone records savings account interest. Remove that null check and guard downstream instead.

14. PortfolioDomainMapper.positionToEntity throws UnsupportedOperationException for anything other than ACB
javaif (!(position instanceof AcbPosition acb)) {
    throw new UnsupportedOperationException(
        "Only AcbPosition supported at this time. Got: " + position.getClass().getSimpleName());
}
FifoPosition is a real sealed permit of Position and is instantiated in FifoPositionProjector. If a FifoPosition ever appears in an account's positionBook (even through a bug or future path), the mapper throws an unchecked exception during a portfolio save, and the transaction rolls back. This should be a proper domain exception, logged clearly, and documented as a known limitation, not a raw UnsupportedOperationException.

15. PortfolioRepositoryImpl.save fires an extra findWithAccountsByIdAndUserId on every write
javaOptional<PortfolioJpaEntity> existing = jpaRepository.findWithAccountsByIdAndUserId(
    id, UUID.fromString(domain.getUserId().toString()));
This @EntityGraph query eagerly loads all accounts, positions, and realized gains — on every save, including simple metadata updates like renaming the portfolio. For users with large portfolios this is unnecessary load. A simpler approach: only load the existing entity for update paths, skip it for new inserts (you can determine this by whether the portfolio was just created or not, or use existsById as a cheaper check).

Notes Worth Tracking

V1 schema assets table vs domain: the assets table was originally designed for a polymorphic asset model that no longer matches the domain. The table is now repurposed for positions but retains dead columns (secondary_ids, display_name, unit_of_trade, name, metadata, identifier_type beyond the discriminator use). These should be cleaned up in a migration.
transactions table columns from V1 that are unmapped: identifier_type, secondary_ids, display_name, unit_of_trade, dividend_amount, dividend_currency, is_drip exist in the DB but nothing reads or writes them. They're dead weight.
PositionBook package-private access: the design is solid — Account controls all mutations through PositionBook. Just make sure no future infrastructure code tries to reach PositionBook directly; the Account.reconstitute() factory is the correct seam.
Striped<Lock> in PositionRecalculationService: the striped lock on accountId is correct and well-thought-out. No issues there.


Summary Table
#SeverityFileIssue1🔴 Critical4 JPA entitiesWrong @Id/@Version import (Spring Data vs JPA)2🔴 CriticalV3 migrationSingle underscore — migration never runs3🔴 CriticalFeeJpaEntityaccount_amount* columns missing from all migrations4🔴 CriticalJpaTransactionRepositoryJPQL uses wrong entity names and non-existent relationships5🔴 CriticalRedisCacheConfig + serializersJackson 2 vs Jackson 3 mixing6🔴 CriticalMarketDataServiceImplMissing @Service, @Value on final fields broken7🔴 CriticalJpaTransactionRepositorydeleteAllExpiredTransactions deletes all transactions8🔴 CriticalFortunelinkApplicationmain not public9🔴 CriticalMarketDataServiceImpl@Cacheable names broken, typo in property key10🟡 SignificantPortfolioDomainMapperUUID churn on realized gains — mass DELETE/INSERT every save11🟡 SignificantV1 migration + entitiesNUMERIC(20,2) vs scale 10 — silent financial rounding12🟡 SignificantTransactionDomainMapperNPE on cashDelta for pre-V3 rows13🟡 SignificantTransactionRecordingServiceImplrecordInterest rejects null symbol, contradicts interface14🟡 SignificantPortfolioDomainMapperUnsupportedOperationException on FifoPosition during save15🟡 SignificantPortfolioRepositoryImplUnnecessary eager load on every save
Fix the 9 critical ones before touching anything else — half of them will prevent the application from starting at all.