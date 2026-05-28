package com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities;

import com.laderrco.fortunelink.portfolio.domain.model.entities.User;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserJpaEntityTest {
  private final UUID sampleId = UUID.randomUUID();
  private final String sampleEmail = "test@fortunelink.com";
  private final Instant now = Instant.now();

  @Nested
  @DisplayName("Factory and Mapping Methods")
  class FactoryAndMappingMethods {

    @Test
    @DisplayName("create: initializes ID, email, and tracking dates correctly")
    void createInitializesFieldsCorrectly() {
      // Act
      UserJpaEntity entity = UserJpaEntity.create(sampleId, sampleEmail);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getId()).isEqualTo(sampleId);
      assertThat(entity.getEmail()).isEqualTo(sampleEmail);
      assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
      assertThat(entity.getLastSignInAt()).isEqualTo(entity.getCreatedAt());
    }

    @Test
    @DisplayName("toDomainUser: transforms JPA Entity values into domain User values cleanly")
    void toDomainUserMapsAllFields() {
      // Arrange
      UserJpaEntity entity = new UserJpaEntity(
          sampleId,
          sampleEmail,
          now.minusSeconds(10), // createdAt
          now, // updatedAt
          now // lastSignInAt
      );

      // Act
      User domainUser = UserJpaEntity.toDomainUser(entity);

      // Assert
      assertThat(domainUser).isNotNull();
      assertThat(domainUser.getUserId()).isEqualTo(UserId.fromString(sampleId.toString()));
      assertThat(domainUser.getEmail()).isEqualTo(sampleEmail);
      assertThat(domainUser.getCreatedAt()).isEqualTo(entity.getCreatedAt());
      assertThat(domainUser.getUpdatedAt()).isEqualTo(entity.getUpdatedAt());
      assertThat(domainUser.getLastSignInAt()).isEqualTo(entity.getLastSignInAt());
    }

    @Test
    @DisplayName("toEntity: transforms Domain values back into JPA Entity values cleanly")
    void toEntityMapsAllFields() {
      // Arrange
      User domainUser = new User(
          UserId.fromString(sampleId.toString()),
          now.minusSeconds(10),
          sampleEmail,
          now,
          now);

      // Act
      UserJpaEntity entity = UserJpaEntity.toEntity(domainUser);

      // Assert
      assertThat(entity).isNotNull();
      assertThat(entity.getId()).isEqualTo(domainUser.getUserId().id());
      assertThat(entity.getEmail()).isEqualTo(domainUser.getEmail());
      assertThat(entity.getCreatedAt()).isEqualTo(domainUser.getCreatedAt());
      assertThat(entity.getUpdatedAt()).isEqualTo(domainUser.getUpdatedAt());
      assertThat(entity.getLastSignInAt()).isEqualTo(domainUser.getLastSignInAt());
    }
  }

  @Nested
  @DisplayName("Lombok Behavioral Configurations")
  class LombokBehavioralConfigurations {

    @Test
    @DisplayName("toString: includes only ID and Email to safe-guard against circular loops or heavy structures")
    void toStringExposesOnlyCoreIdentifiers() {
      // Arrange
      UserJpaEntity entity = UserJpaEntity.create(sampleId, sampleEmail);

      // Act
      String toStringResult = entity.toString();

      // Assert
      assertThat(toStringResult)
          .contains(sampleId.toString())
          .contains(sampleEmail)
          .doesNotContain("createdAt")
          .doesNotContain("lastSignInAt");
    }
  }
}