package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import com.laderrco.fortunelink.portfolio.domain.model.entities.User;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.LastModifiedDate;

@Entity
@Data
@Table(name = "users", schema = "public")
@AllArgsConstructor
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@ToString(of = {"id", "email"})
public class UserJpaEntity {

  @Id
  @Column(name = "id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "last_sign_in_at")
  private Instant lastSignInAt;

  // ── Factory ───────────────────────────────────────────────────────────────

  /**
   * Called by auth sync trigger handler when a new Supabase user is created.
   */
  public static UserJpaEntity create(UUID id, String email) {
    UserJpaEntity u = new UserJpaEntity();
    u.id = id;
    u.email = email;
    u.createdAt = Instant.now();
    u.lastSignInAt = Instant.now();
    return u;
  }

  public static User toDomainUser(UserJpaEntity jpaEntity) {
    return new User(UserId.fromString(jpaEntity.id.toString()), jpaEntity.createdAt,
        jpaEntity.getEmail(), jpaEntity.updatedAt, jpaEntity.lastSignInAt);
  }

  public static UserJpaEntity toEntity(User user) {
    return new UserJpaEntity(user.getUserId().id(), user.getEmail(), user.getCreatedAt(),
        user.getUpdatedAt(), user.getLastSignInAt());
  }
}