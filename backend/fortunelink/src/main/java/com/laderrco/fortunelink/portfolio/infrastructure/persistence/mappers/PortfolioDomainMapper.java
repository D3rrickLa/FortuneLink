package com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers;

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
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.AcbPosition;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions.Position;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.PortfolioJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.PositionJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.RealizedGainJpaEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Bidirectional mapper between the {@code Portfolio} aggregate and its JPA entities.
 * <p>
 * Mapping direction conventions:
 * <ul>
 * <li>{@code toDomain} -> JPA entity graph -> domain object (used during
 * reads)</li>
 * <li>{@code toEntity} -> domain object -> JPA entity graph (used during
 * writes)</li>
 * </ul>
 * <p>
 * This class is the only place in the codebase that knows about both layers.
 * It must not contain business logic. If you find yourself adding an {@code if}
 * that reflects a domain rule here, that rule belongs in the domain instead.
 */
@Component
public class PortfolioDomainMapper {

  // =========================================================================
  // Portfolio — toDomain
  // =========================================================================

  private static String resolveIdentifierType(AssetType type) {
    return switch (type) {
      case CRYPTO -> "CRYPTO";
      case CASH -> "CASH";
      default -> "MARKET";
    };
  }

  // =========================================================================
  // Portfolio — toEntity (new or update)
  // =========================================================================

  private static UUID findExistingPositionId(AccountJpaEntity existing, String symbol) {
    if (existing == null) {
      return null;
    }
    return existing.getPositions().stream().filter(p -> p.getSymbol().equals(symbol))
        .map(PositionJpaEntity::getId).findFirst().orElse(null);
  }

  // =========================================================================
  // Account — toDomain
  // =========================================================================

  public Portfolio toDomain(PortfolioJpaEntity entity) {
    Objects.requireNonNull(entity, "PortfolioJpaEntity cannot be null");

    Map<AccountId, Account> accountMap = new LinkedHashMap<>();
    for (AccountJpaEntity ae : entity.getAccounts()) {
      Account account = accountToDomain(ae);
      accountMap.put(account.getAccountId(), account);
    }

    UserId deletedBy =
        entity.getDeletedBy() != null ? UserId.fromString(entity.getDeletedBy().toString()) : null;

    return Portfolio.reconstitute(PortfolioId.fromString(entity.getId().toString()),
        UserId.fromString(entity.getUserId().toString()), entity.getName(), entity.getDescription(),
        accountMap, Currency.of(entity.getDisplayCurrencyCode()), entity.isDeleted(),
        entity.getDeletedAt(), deletedBy, entity.getCreatedAt(), entity.getUpdatedAt());
  }

  // =========================================================================
  // Account — toEntity
  // =========================================================================

  public PortfolioJpaEntity toEntity(Portfolio domain, PortfolioJpaEntity existing) {
    Objects.requireNonNull(domain, "Portfolio domain object cannot be null");

    UUID deletedBy =
        domain.getDeletedBy() != null ? UUID.fromString(domain.getDeletedBy().toString()) : null;

    PortfolioJpaEntity entity;
    if (existing == null) {
      entity = PortfolioJpaEntity.create(UUID.fromString(domain.getPortfolioId().toString()),
          UUID.fromString(domain.getUserId().toString()), domain.getName(), domain.getDescription(),
          domain.getDisplayCurrency().getCode(), domain.isDeleted(), domain.getDeletedOn(),
          deletedBy, domain.getCreatedAt(), domain.getLastUpdatedAt());
    } else {
      existing.update(domain.getName(), domain.getDescription(),
          domain.getDisplayCurrency().getCode(), domain.isDeleted(), domain.getDeletedOn(),
          deletedBy, domain.getLastUpdatedAt());
      entity = existing;
    }

    List<AccountJpaEntity> accountEntities = new ArrayList<>();
    for (Account account : domain.getAccounts()) {
      AccountJpaEntity existingAccount = existing == null ? null : existing.getAccounts().stream()
          .filter(ae -> ae.getId().equals(UUID.fromString(account.getAccountId().toString())))
          .findFirst().orElse(null);

      accountEntities.add(accountToEntity(account, entity, existingAccount));
    }

    entity.replaceAccounts(accountEntities);
    return entity;
  }

  // =========================================================================
  // Position helpers
  // =========================================================================

  Account accountToDomain(AccountJpaEntity ae) {
    Map<AssetSymbol, Position> positionMap = new LinkedHashMap<>();
    for (PositionJpaEntity pe : ae.getPositions()) {
      AcbPosition pos = positionToDomain(pe, ae.getBaseCurrencyCode());
      positionMap.put(pos.symbol(), pos);
    }

    // Use reconstitute() so the stable DB UUID flows into the domain record.
    // This is what allows the mapper to diff on save and skip re-inserting existing
    // gains.
    List<RealizedGainRecord> gains = new ArrayList<>();
    for (RealizedGainJpaEntity ge : ae.getRealizedGains()) {
      gains.add(realizedGainToDomain(ge));
    }

    Currency currency = Currency.of(ae.getBaseCurrencyCode());
    Money cashBalance = new Money(ae.getCashBalanceAmount(), currency);

    return Account.reconstitute(AccountId.fromString(ae.getId().toString()), ae.getName(),
        AccountType.valueOf(ae.getAccountType()), currency,
        PositionStrategy.valueOf(ae.getPositionStrategy()),
        HealthStatus.valueOf(ae.getHealthStatus()),
        AccountLifecycleState.valueOf(ae.getLifecycleState()), ae.getClosedDate(),
        ae.getCreatedDate(), ae.getLastUpdatedOn(), cashBalance, positionMap, gains);
  }

  AccountJpaEntity accountToEntity(Account domain, PortfolioJpaEntity portfolioEntity,
      AccountJpaEntity existing) {

    AccountJpaEntity entity;
    if (existing == null) {
      entity = AccountJpaEntity.create(UUID.fromString(domain.getAccountId().toString()),
          portfolioEntity, domain.getName(), domain.getAccountType().name(),
          domain.getAccountCurrency().getCode(), domain.getPositionStrategy().name(),
          domain.getHealthStatus().name(), domain.getState().name(),
          domain.getCashBalance().amount(), domain.getCashBalance().currency().getCode(),
          domain.getCloseDate(), domain.getCreationDate(), domain.getLastUpdatedOn());
    } else {
      AccountJpaEntity updated = AccountJpaEntity.create(existing.getId(), portfolioEntity,
          domain.getName(), domain.getAccountType().name(), domain.getAccountCurrency().getCode(),
          domain.getPositionStrategy().name(), domain.getHealthStatus().name(),
          domain.getState().name(), domain.getCashBalance().amount(),
          domain.getCashBalance().currency().getCode(), domain.getCloseDate(),
          domain.getCreationDate(), domain.getLastUpdatedOn());
      existing.applyFrom(updated);
      entity = existing;
    }

    // Positions — full replace is correct here because positions are always
    // fully rebuilt from transactions by PositionRecalculationService.
    List<PositionJpaEntity> positionEntities = new ArrayList<>();
    for (Map.Entry<AssetSymbol, Position> entry : domain.getPositionEntries()) {
      AssetSymbol sym = entry.getKey();
      Position pos = entry.getValue();
      UUID posId = findExistingPositionId(existing, sym.symbol());
      positionEntities.add(
          positionToEntity(posId != null ? posId : UUID.randomUUID(), entity, pos));
    }
    entity.replacePositions(positionEntities);

    // Realized gains — append-only. NEVER clear and re-insert.
    //
    // 1. Collect the UUIDs that are already persisted in the DB.
    // 2. Filter domain gains to only those not yet persisted.
    // 3. Append only the delta.
    //
    // This works because RealizedGainRecord carries a stable UUID generated at
    // the moment Account.recordRealizedGain() is called, and reconstituted from
    // the DB row UUID when the account is loaded. The IDs are stable across saves.
    Set<UUID> persistedGainIds = existing == null ? Collections.emptySet()
        : existing.getRealizedGains().stream().map(RealizedGainJpaEntity::getId)
            .collect(Collectors.toSet());

    List<RealizedGainJpaEntity> newGainEntities = new ArrayList<>();
    for (RealizedGainRecord rg : domain.getRealizedGains()) {
      if (!persistedGainIds.contains(rg.id())) {
        // Use rg.id() — NOT UUID.randomUUID() — so the ID is stable across saves.
        newGainEntities.add(realizedGainToEntity(rg.id(), entity, rg));
      }
    }
    entity.addNewRealizedGains(newGainEntities);

    return entity;
  }

  // =========================================================================
  // RealizedGain helpers
  // =========================================================================

  private AcbPosition positionToDomain(PositionJpaEntity pe, String accountCurrencyCode) {
    Currency currency = Currency.of(accountCurrencyCode);
    return new AcbPosition(new AssetSymbol(pe.getSymbol()), AssetType.valueOf(pe.getAssetType()),
        currency, new Quantity(pe.getQuantity()),
        new Money(pe.getCostBasisAmount(), Currency.of(pe.getCostBasisCurrency())),
        pe.getAcquiredDate(), pe.getLastModifiedAt());
  }

  private PositionJpaEntity positionToEntity(UUID id, AccountJpaEntity accountEntity,
      Position position) {
    if (!(position instanceof AcbPosition acb)) {
      throw new UnsupportedOperationException(
          "Only AcbPosition supported at this time. Got: " + position.getClass().getSimpleName());
    }

    return PositionJpaEntity.create(id, accountEntity, resolveIdentifierType(acb.type()),
        acb.symbol().symbol(), acb.type().name(), acb.totalQuantity().amount(),
        acb.totalCostBasis().amount(), acb.totalCostBasis().currency().getCode(),
        acb.firstAcquiredAt(), acb.lastModifiedAt());
  }

  // =========================================================================
  // Private utilities
  // =========================================================================

  /**
   * Reconstitutes a domain record from a DB row, threading the stable UUID through. This must use
   * RealizedGainRecord.reconstitute() — NOT of() — so the ID matches the persisted row and the
   * mapper can skip re-inserting on the next save.
   */
  private RealizedGainRecord realizedGainToDomain(RealizedGainJpaEntity ge) {
    return RealizedGainRecord.reconstitute(ge.getId(),
        // stable DB row UUID — critical for the append-only diff in toEntity
        new AssetSymbol(ge.getSymbol()),
        new Money(ge.getGainLossAmount(), Currency.of(ge.getGainLossCurrency())),
        new Money(ge.getCostBasisSoldAmount(), Currency.of(ge.getCostBasisSoldCurrency())),
        ge.getOccurredAt());
  }

  /**
   * Converts a domain realized gain to a JPA entity for persistence. The id parameter MUST be
   * rg.id() — it is passed explicitly to make it impossible to accidentally pass UUID.randomUUID()
   * here again.
   */
  private RealizedGainJpaEntity realizedGainToEntity(UUID id, AccountJpaEntity accountEntity,
      RealizedGainRecord rg) {
    return RealizedGainJpaEntity.create(id, accountEntity, rg.symbol().symbol(),
        rg.realizedGainLoss().amount(), rg.realizedGainLoss().currency().getCode(),
        rg.costBasisSold().amount(), rg.costBasisSold().currency().getCode(), rg.occurredAt());
  }
}