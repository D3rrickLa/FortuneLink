package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

/**
 * Pure persistence model for the {@code Portfolio} aggregate root.
 * <p>
 * Zero domain logic lives here. No business methods, no invariant checks. The only responsibility
 * is mapping columns -> Java fields and managing the JPA relationship graph.
 */
@Entity
@Getter
@Table(name = "portfolios")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED) // for JPA
public class PortfolioJpaEntity implements Persistable<UUID> {

  @Id
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "description", length = 255)
  private String description;

  /**
   * ISO-4217 code of the display currency preference (e.g. "CAD", "USD"). Reconstructed into a
   * {@code Currency} domain object by the mapper.
   */
  @Column(name = "display_currency_code", nullable = false, length = 3)
  private String displayCurrencyCode;

  @Column(name = "deleted", nullable = false)
  private boolean deleted;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Column(name = "deleted_by")
  private UUID deletedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Transient
  private boolean isNew = true; // Unified strategy

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  /**
   * LAZY by default. Load eagerly only when you need accounts in the same request (use the
   * {@code @EntityGraph} on the repo for that).
   */
  @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private final Set<AccountJpaEntity> accounts = new LinkedHashSet<>();

  // -------------------------------------------------------------------------
  // Factory, called by the domain mapper when persisting a new Portfolio
  // -------------------------------------------------------------------------

  public static PortfolioJpaEntity create(UUID id, UUID userId, String name, String description,
      String displayCurrencyCode, boolean deleted, Instant deletedAt, UUID deletedBy,
      Instant createdAt, Instant updatedAt) {

    PortfolioJpaEntity e = new PortfolioJpaEntity();
    e.id = id;
    e.userId = userId;
    e.name = name;
    e.description = description;
    e.displayCurrencyCode = displayCurrencyCode;
    e.deleted = deleted;
    e.deletedAt = deletedAt;
    e.deletedBy = deletedBy;
    e.createdAt = createdAt;
    e.updatedAt = updatedAt;
    return e;
  }

  // -------------------------------------------------------------------------
  // Mutation helpers, mapper calls these during an update
  // -------------------------------------------------------------------------

  public void update(String name, String description, String displayCurrencyCode, boolean deleted,
      Instant deletedAt, UUID deletedBy, Instant updatedAt) {

    this.name = name;
    this.description = description;
    this.displayCurrencyCode = displayCurrencyCode;
    this.deleted = deleted;
    this.deletedAt = deletedAt;
    this.deletedBy = deletedBy;
    this.updatedAt = updatedAt;
  }

  // Merge rather than clear-and-add to avoid gratuitous DELETEs from
  // Hibernate when only metadata changed.
  public void replaceAccounts(List<AccountJpaEntity> incoming) {
    Map<UUID, AccountJpaEntity> existing = new HashMap<>();
    for (AccountJpaEntity a : this.accounts) {
      existing.put(a.getId(), a);
    }

    List<AccountJpaEntity> merged = new ArrayList<>();
    for (AccountJpaEntity a : incoming) {
      AccountJpaEntity current = existing.get(a.getId());
      if (current != null) {
        current.applyFrom(a); // in-place update , Hibernate tracks the managed instance
        merged.add(current);
      } else {
        a.setPortfolio(this);
        merged.add(a);
      }
    }

    // Remove accounts that no longer exist in the domain
    Set<UUID> incomingIds = new HashSet<>();
    for (AccountJpaEntity a : incoming) {
      incomingIds.add(a.getId());
    }
    this.accounts.removeIf(a -> !incomingIds.contains(a.getId()));

    // Add new ones
    for (AccountJpaEntity a : merged) {
      this.accounts.add(a);
    }
  }

  // -------------------------------------------------------------------------
  // Getters, read-only for mapper use
  // -------------------------------------------------------------------------
  public Set<AccountJpaEntity> getAccounts() {
    return Collections.unmodifiableSet(accounts);
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostLoad
  @PostPersist
  void markNotNew() {
    this.isNew = false;
  }
}