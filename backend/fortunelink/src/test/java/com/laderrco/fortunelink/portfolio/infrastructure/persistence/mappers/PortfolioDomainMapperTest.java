package com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountLifecycleState;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.HealthStatus;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.PortfolioJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.PositionJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.RealizedGainJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PortfolioDomainMapper Unit Tests")
class PortfolioDomainMapperTest {

  private static final UUID RAW_ACCOUNT_ID = UUID.randomUUID();
  private static final UUID RAW_PORTFOLIO_ID = UUID.randomUUID();
  private static final UUID RAW_USER_ID = UUID.randomUUID();
  private static final AccountId ACCOUNT_ID = AccountId.fromString(RAW_ACCOUNT_ID.toString());
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.fromString(
      RAW_PORTFOLIO_ID.toString());
  private static final UserId USER_ID = UserId.fromString(RAW_USER_ID.toString());
  private static final UUID PORTFOLIO_UUID = UUID.randomUUID();
  private static final UUID USER_UUID = UUID.randomUUID();
  private static final UUID ACCOUNT_UUID = UUID.randomUUID();
  private PortfolioDomainMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new PortfolioDomainMapper();
  }

  @Test
  @DisplayName("Positions should be fully replaced based on domain state")
  void positionsShouldBeFullyReplaced() {

    PortfolioJpaEntity existingPortfolio = createBasePortfolioEntity();
    AccountJpaEntity existingAccount = createBaseAccountEntity(existingPortfolio);
    PositionJpaEntity oldPos = PositionJpaEntity.create(UUID.randomUUID(), existingAccount,
        "MARKET", "AAPL", "STOCK", BigDecimal.TEN, BigDecimal.valueOf(100), "USD", Instant.now(),
        Instant.now());
    existingAccount.replacePositions(Set.of(oldPos));
    existingPortfolio.replaceAccounts(List.of(existingAccount));

    AcbPosition msftPos = new AcbPosition(new AssetSymbol("MSFT"), AssetType.STOCK,
        Currency.of("USD"), new Quantity(BigDecimal.ONE),
        new Money(BigDecimal.valueOf(200), Currency.of("USD")), Instant.now(), Instant.now());

    Account updatedAccount = Account.reconstitute(ACCOUNT_ID, "Trading",
        AccountType.NON_REGISTERED_INVESTMENT, Currency.of("USD"), PositionStrategy.FIFO,
        HealthStatus.HEALTHY, AccountLifecycleState.ACTIVE, null, Instant.now(), Instant.now(),
        new Money(BigDecimal.ZERO, Currency.of("USD")), Map.of(new AssetSymbol("MSFT"), msftPos),
        List.of());

    Portfolio updatedDomain = Portfolio.reconstitute(PORTFOLIO_ID, USER_ID, "Main Portfolio",
        "Desc", Map.of(updatedAccount.getAccountId(), updatedAccount), Currency.of("USD"), false,
        null, null, Instant.now(), Instant.now());

    PortfolioJpaEntity result = mapper.toEntity(updatedDomain, existingPortfolio);

    AccountJpaEntity resultAccount = result.getAccounts().stream()
        .filter(a -> a.getId().equals(RAW_ACCOUNT_ID)).findFirst().orElseThrow();

    assertThat(resultAccount.getPositions()).hasSize(1);
    PositionJpaEntity actualPos = resultAccount.getPositions().iterator().next();

    assertThat(actualPos.getSymbol()).isEqualTo("MSFT");
    assertThat(actualPos.getSymbol()).isNotEqualTo("AAPL");
  }

  private PortfolioJpaEntity createBasePortfolioEntity() {
    return PortfolioJpaEntity.create(PORTFOLIO_UUID, USER_UUID, "Main Portfolio", "Desc", "USD",
        false, null, null, Instant.now(), Instant.now());
  }

  private AccountJpaEntity createBaseAccountEntity(PortfolioJpaEntity portfolio) {
    return AccountJpaEntity.create(ACCOUNT_UUID, portfolio, "Trading", "TFSA", "USD", "FIFO",
        "HEALTHY", "ACTIVE", BigDecimal.ZERO, "USD", null, Instant.now(), Instant.now());
  }

  private Portfolio createBaseDomainPortfolio() {
    Account account = Account.reconstitute(AccountId.fromString(ACCOUNT_UUID.toString()), "Trading",
        AccountType.TAXABLE_INVESTMENT, Currency.of("USD"), PositionStrategy.FIFO,
        HealthStatus.HEALTHY, AccountLifecycleState.ACTIVE, null, Instant.now(), Instant.now(),
        new Money(BigDecimal.ZERO, Currency.of("USD")), Map.of(), List.of());

    return Portfolio.reconstitute(PortfolioId.fromString(PORTFOLIO_UUID.toString()),
        UserId.fromString(USER_UUID.toString()), "Main Portfolio", "Desc",
        Map.of(account.getAccountId(), account), Currency.of("USD"), false, null, null,
        Instant.now(), Instant.now());
  }

  @Nested
  @DisplayName("Portfolio Mapping")
  class PortfolioMapping {

    @Test
    @DisplayName("toDomain should correctly reconstitute full aggregate from entity")
    void toDomainShouldMapFullAggregate() {

      PortfolioJpaEntity portfolioEntity = createBasePortfolioEntity();
      AccountJpaEntity accountEntity = createBaseAccountEntity(portfolioEntity);
      portfolioEntity.replaceAccounts(List.of(accountEntity));

      Portfolio domain = mapper.toDomain(portfolioEntity);

      assertThat(domain.getPortfolioId().id()).isEqualTo(PORTFOLIO_UUID);
      assertThat(domain.getUserId().id()).isEqualTo(USER_UUID);
      assertThat(domain.getName()).isEqualTo("Main Portfolio");
      assertThat(domain.getAccounts()).hasSize(1);

      Account account = domain.getAccounts().iterator().next();
      assertThat(account.getAccountId().id()).isEqualTo(ACCOUNT_UUID);
    }

    @Test
    @DisplayName("toEntity (new) should create a fresh entity with accounts")
    void toEntityShouldCreateNewEntity() {

      Portfolio domain = createBaseDomainPortfolio();

      PortfolioJpaEntity entity = mapper.toEntity(domain, null);

      assertThat(entity.getId()).isEqualTo(PORTFOLIO_UUID);
      assertThat(entity.getName()).isEqualTo("Main Portfolio");

      assertThat(entity.getAccounts()).hasSize(1);
      AccountJpaEntity accountEntity = entity.getAccounts().iterator().next();
      assertThat(accountEntity.getPortfolio()).isEqualTo(entity);
      assertThat(accountEntity.getId()).isEqualTo(ACCOUNT_UUID);
    }

    @Test
    @DisplayName("toEntity (existing) should update fields and handle account diffing")
    void toEntityShouldUpdateExistingEntity() {

      PortfolioJpaEntity existing = createBasePortfolioEntity();
      Portfolio domain = createBaseDomainPortfolio();

      PortfolioJpaEntity result = mapper.toEntity(domain, existing);

      assertThat(result).isSameAs(existing);
      assertThat(result.getName()).isEqualTo("Main Portfolio");
    }
  }

  @Nested
  @DisplayName("Collection and Identity Strategy")
  class CollectionStrategy {

    @Test
    @DisplayName("Realized gains should use append-only logic based on stable UUIDs")
    void realizedGainsShouldUseAppendOnlyLogic() {

      PortfolioJpaEntity existingPortfolio = createBasePortfolioEntity();
      AccountJpaEntity existingAccount = createBaseAccountEntity(existingPortfolio);

      UUID existingGainId = UUID.randomUUID();
      RealizedGainJpaEntity existingGain = RealizedGainJpaEntity.create(existingGainId,
          existingAccount, "AAPL", BigDecimal.TEN, "USD", BigDecimal.ONE, "USD", Instant.now());

      existingAccount.addNewRealizedGains(List.of(existingGain));
      existingPortfolio.replaceAccounts(List.of(existingAccount));

      Portfolio domain = mapper.toDomain(existingPortfolio);
      Account domainAccount = domain.getAccounts().iterator().next();

      domainAccount.recordRealizedGain(new AssetSymbol("MSFT"), Money.of(50, Currency.USD),
          new Money(BigDecimal.TEN, Currency.of("USD")), Instant.now());

      PortfolioJpaEntity result = mapper.toEntity(domain, existingPortfolio);

      AccountJpaEntity resultAccount = result.getAccounts().stream()
          .filter(a -> a.getId().equals(ACCOUNT_UUID)).findFirst().orElseThrow();

      assertThat(resultAccount.getRealizedGains()).hasSize(2);
      assertThat(resultAccount.getRealizedGains()).contains(existingGain);
    }
  }
}