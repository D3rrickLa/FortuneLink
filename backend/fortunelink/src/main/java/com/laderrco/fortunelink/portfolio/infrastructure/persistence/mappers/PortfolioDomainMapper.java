package com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers;

import java.util.*;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.*;
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

/**
 * Bidirectional mapper between the {@code Portfolio} aggregate and its JPA
 * entities.
 * <p>
 * Mapping direction conventions:
 * <ul>
 * <li>{@code toDomain} -> JPA entity graph -> domain object (used during
 * reads)</li>
 * <li>{@code toEntity} -> domain object -> JPA entity graph (used during
 * writes)</li>
 * </ul>
 *
 * This class is the only place in the codebase that knows about both layers.
 * It must not contain business logic. If you find yourself adding an {@code if}
 * that reflects a domain rule here, that rule belongs in the domain instead.
 */
@Component
public class PortfolioDomainMapper {

  // =========================================================================
  // Portfolio — toDomain
  // =========================================================================

  public Portfolio toDomain(PortfolioJpaEntity entity) {
    Objects.requireNonNull(entity, "PortfolioJpaEntity cannot be null");

    Map<AccountId, Account> accountMap = new LinkedHashMap<>();
    for (AccountJpaEntity ae : entity.getAccounts()) {
      Account account = accountToDomain(ae);
      accountMap.put(account.getAccountId(), account);
    }

    UserId deletedBy = entity.getDeletedBy() != null
        ? UserId.fromString(entity.getDeletedBy().toString())
        : null;

    return Portfolio.reconstitute(
        PortfolioId.fromString(entity.getId().toString()),
        UserId.fromString(entity.getUserId().toString()),
        entity.getName(),
        entity.getDescription(),
        accountMap,
        Currency.of(entity.getDisplayCurrencyCode()),
        entity.isDeleted(),
        entity.getDeletedAt(),
        deletedBy,
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  // =========================================================================
  // Portfolio — toEntity (new or update)
  // =========================================================================

  /**
   * Converts a domain {@code Portfolio} to a JPA entity graph ready to save.
   * <p>
   * For updates, pass the {@code existing} JPA entity so that Hibernate
   * tracks the managed instance rather than creating a detached clone.
   * For inserts, pass {@code null} and a fresh entity will be created.
   */
  public PortfolioJpaEntity toEntity(Portfolio domain, PortfolioJpaEntity existing) {
    Objects.requireNonNull(domain, "Portfolio domain object cannot be null");

    UUID deletedBy = domain.getDeletedBy() != null
        ? UUID.fromString(domain.getDeletedBy().toString())
        : null;

    PortfolioJpaEntity entity;
    if (existing == null) {
      entity = PortfolioJpaEntity.create(
          UUID.fromString(domain.getPortfolioId().toString()),
          UUID.fromString(domain.getUserId().toString()),
          domain.getName(),
          domain.getDescription(),
          domain.getDisplayCurrency().getCode(),
          domain.isDeleted(),
          domain.getDeletedOn(),
          deletedBy,
          domain.getCreatedAt(),
          domain.getLastUpdatedAt());
    } else {
      existing.update(
          domain.getName(),
          domain.getDescription(),
          domain.getDisplayCurrency().getCode(),
          domain.isDeleted(),
          domain.getDeletedOn(),
          deletedBy,
          domain.getLastUpdatedAt());
      entity = existing;
    }

    // Build account JPA entities
    List<AccountJpaEntity> accountEntities = new ArrayList<>();
    for (Account account : domain.getAccounts()) {
      // Find the matching existing entity (if present) to allow in-place update
      AccountJpaEntity existingAccount = existing == null ? null
          : existing.getAccounts().stream()
              .filter(ae -> ae.getId().equals(UUID.fromString(account.getAccountId().toString())))
              .findFirst()
              .orElse(null);

      accountEntities.add(accountToEntity(account, entity, existingAccount));
    }

    entity.replaceAccounts(accountEntities);
    return entity;
  }

  // =========================================================================
  // Account — toDomain (package-accessible for AccountDomainMapper)
  // =========================================================================

  Account accountToDomain(AccountJpaEntity ae) {
    // Build position map
    Map<AssetSymbol, Position> positionMap = new LinkedHashMap<>();
    for (PositionJpaEntity pe : ae.getPositions()) {
      AcbPosition pos = positionToDomain(pe, ae.getBaseCurrencyCode());
      positionMap.put(pos.symbol(), pos);
    }

    // Build realized gains list
    List<RealizedGainRecord> gains = new ArrayList<>();
    for (RealizedGainJpaEntity ge : ae.getRealizedGains()) {
      gains.add(realizedGainToDomain(ge));
    }

    Currency currency = Currency.of(ae.getBaseCurrencyCode());
    Money cashBalance = new Money(ae.getCashBalanceAmount(), currency);

    return Account.reconstitute(
        AccountId.fromString(ae.getId().toString()),
        ae.getName(),
        AccountType.valueOf(ae.getAccountType()),
        currency,
        PositionStrategy.valueOf(ae.getPositionStrategy()),
        HealthStatus.valueOf(ae.getHealthStatus()),
        AccountLifecycleState.valueOf(ae.getLifecycleState()),
        ae.getClosedDate(),
        ae.getCreatedDate(),
        ae.getLastUpdatedOn(),
        cashBalance,
        positionMap,
        gains);
  }

  // =========================================================================
  // Account — toEntity
  // =========================================================================

  AccountJpaEntity accountToEntity(Account domain, PortfolioJpaEntity portfolioEntity,
      AccountJpaEntity existing) {

    boolean active = domain.getState() != AccountLifecycleState.CLOSED;

    AccountJpaEntity entity;
    if (existing == null) {
      entity = AccountJpaEntity.create(
          UUID.fromString(domain.getAccountId().toString()),
          portfolioEntity,
          domain.getName(),
          domain.getAccountType().name(),
          domain.getAccountCurrency().getCode(),
          domain.getPositionStrategy().name(),
          domain.getHealthStatus().name(),
          domain.getState().name(),
          domain.getCashBalance().amount(),
          domain.getCashBalance().currency().getCode(),
          active,
          domain.getCloseDate(),
          domain.getCreationDate(),
          domain.getLastUpdatedOn());
    } else {
      // applyFrom updates all mutable fields in-place
      AccountJpaEntity updated = AccountJpaEntity.create(
          existing.getId(),
          portfolioEntity,
          domain.getName(),
          domain.getAccountType().name(),
          domain.getAccountCurrency().getCode(),
          domain.getPositionStrategy().name(),
          domain.getHealthStatus().name(),
          domain.getState().name(),
          domain.getCashBalance().amount(),
          domain.getCashBalance().currency().getCode(),
          active,
          domain.getCloseDate(),
          domain.getCreationDate(),
          domain.getLastUpdatedOn());
      existing.applyFrom(updated);
      entity = existing;
    }

    // Positions
    List<PositionJpaEntity> positionEntities = new ArrayList<>();
    for (Map.Entry<AssetSymbol, Position> entry : domain.getPositionEntries()) {
      AssetSymbol sym = entry.getKey();
      Position pos = entry.getValue();

      // Find existing row by symbol to preserve its UUID and avoid delete/insert
      // churn
      UUID posId = findExistingPositionId(existing, sym.symbol());
      positionEntities.add(positionToEntity(posId != null ? posId : UUID.randomUUID(),
          entity, pos));
    }
    entity.replacePositions(positionEntities);

    // Realized gains — full replace (append-only in domain, idempotent here)
    List<RealizedGainJpaEntity> gainEntities = new ArrayList<>();
    for (RealizedGainRecord rg : domain.getRealizedGains()) {
      gainEntities.add(realizedGainToEntity(UUID.randomUUID(), entity, rg));
    }
    entity.replaceRealizedGains(gainEntities);

    return entity;
  }

  // =========================================================================
  // Position helpers
  // =========================================================================

  private AcbPosition positionToDomain(PositionJpaEntity pe, String accountCurrencyCode) {
    Currency currency = Currency.of(accountCurrencyCode);
    return new AcbPosition(
        new AssetSymbol(pe.getSymbol()),
        AssetType.valueOf(pe.getAssetType()),
        currency,
        new Quantity(pe.getQuantity()),
        new Money(pe.getCostBasisAmount(), Currency.of(pe.getCostBasisCurrency())),
        pe.getAcquiredDate(),
        pe.getLastModifiedAt());
  }

  private PositionJpaEntity positionToEntity(UUID id, AccountJpaEntity accountEntity,
      Position position) {
    // MVP: AcbPosition only. When FIFO is added, pattern-match on type.
    if (!(position instanceof AcbPosition acb)) {
      throw new UnsupportedOperationException(
          "Only AcbPosition supported at this time. Got: "
              + position.getClass().getSimpleName());
    }

    return PositionJpaEntity.create(
        id,
        accountEntity,
        resolveIdentifierType(acb.type()),
        acb.symbol().symbol(),
        acb.type().name(),
        acb.totalQuantity().amount(),
        acb.totalCostBasis().amount(),
        acb.totalCostBasis().currency().getCode(),
        acb.firstAcquiredAt(),
        acb.lastModifiedAt());
  }

  // =========================================================================
  // RealizedGain helpers
  // =========================================================================

  private RealizedGainRecord realizedGainToDomain(RealizedGainJpaEntity ge) {
    return new RealizedGainRecord(
        new AssetSymbol(ge.getSymbol()),
        new Money(ge.getGainLossAmount(), Currency.of(ge.getGainLossCurrency())),
        new Money(ge.getCostBasisSoldAmount(), Currency.of(ge.getCostBasisSoldCurrency())),
        ge.getOccurredAt());
  }

  private RealizedGainJpaEntity realizedGainToEntity(UUID id, AccountJpaEntity accountEntity,
      RealizedGainRecord rg) {
    return RealizedGainJpaEntity.create(
        id,
        accountEntity,
        rg.symbol().symbol(),
        rg.realizedGainLoss().amount(),
        rg.realizedGainLoss().currency().getCode(),
        rg.costBasisSold().amount(),
        rg.costBasisSold().currency().getCode(),
        rg.occurredAt());
  }

  // =========================================================================
  // Private utilities
  // =========================================================================

  /**
   * Maps {@code AssetType} → the {@code identifier_type} discriminator used in V1
   * schema.
   * MARKET covers stocks, ETFs, bonds. CRYPTO is its own discriminator. CASH for
   * cash.
   */
  private static String resolveIdentifierType(AssetType type) {
    return switch (type) {
      case CRYPTO -> "CRYPTO";
      case CASH -> "CASH";
      default -> "MARKET";
    };
  }

  /**
   * Finds the UUID of an existing {@code PositionJpaEntity} row by symbol so we
   * can reuse it on update (avoids unnecessary DELETE + INSERT in Hibernate).
   */
  private static UUID findExistingPositionId(AccountJpaEntity existing, String symbol) {
    if (existing == null)
      return null;
    return existing.getPositions().stream()
        .filter(p -> p.getSymbol().equals(symbol))
        .map(PositionJpaEntity::getId)
        .findFirst()
        .orElse(null);
  }
}