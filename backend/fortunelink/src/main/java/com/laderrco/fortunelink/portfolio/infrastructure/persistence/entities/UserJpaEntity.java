package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;

import jakarta.persistence.*;

@Entity
@Data
@Table(name = "users", schema = "public")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@ToString(of = { "id", "email", "baseCurrency" })
public class UserJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "full_name")
  private String fullName;

  @Column(name = "base_currency", nullable = false, length = 3)
  private String baseCurrency = "CAD";

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "last_sign_in_at")
  private Instant lastSignInAt;

  // ── Factory ───────────────────────────────────────────────────────────────

  /** Called by auth sync trigger handler when a new Supabase user is created. */
  public static UserJpaEntity create(UUID id, String email, String fullName) {
    UserJpaEntity u = new UserJpaEntity();
    u.id = id;
    u.email = email;
    u.fullName = fullName;
    u.baseCurrency = Currency.CAD.getCode(); // safe default
    u.createdAt = Instant.now();
    u.lastSignInAt = Instant.now();
    return u;
  }

  // ── Mutation ──────────────────────────────────────────────────────────────

  /**
   * Updates the user's preferred reporting currency.
   * Validation (is this a supported ISO code?) is enforced at the API boundary;
   * here we only enforce non-null.
   */
  public void updateBaseCurrency(String currency) {
    if (currency == null)
      throw new IllegalArgumentException("baseCurrency must not be null");
    this.baseCurrency = currency;
    this.lastSignInAt = Instant.now();
  }

  public void updateProfile(String fullName) {
    this.fullName = fullName;
    this.lastSignInAt = Instant.now();
  }
}