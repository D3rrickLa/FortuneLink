package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.entities.User;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.UserJpaEntity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

  @Mock
  private JpaUserRepository jpaRepository;

  @InjectMocks
  private UserRepositoryImpl repository;

  private final UserId userId = new UserId(UUID.randomUUID());
  private final String email = "alex@fortunelink.com";

  @Nested
  @DisplayName("Query Operations")
  class QueryOperations {

    @Test
    @DisplayName("findById: delegates to JPA and converts returning entity to domain")
    void findByIdDelegatesAndConverts() {

      UserJpaEntity jpaEntity = new UserJpaEntity(userId.id(), email, Instant.now(), Instant.now(), Instant.now());
      when(jpaRepository.findById(userId.id())).thenReturn(Optional.of(jpaEntity));

      Optional<User> result = repository.findById(userId);

      assertThat(result).isPresent();
      assertThat(result.get().getUserId()).isEqualTo(userId);
      assertThat(result.get().getEmail()).isEqualTo(email);
      verify(jpaRepository).findById(userId.id());
    }

    @Test
    @DisplayName("findById: returns empty Optional when user record is absent")
    void findByIdReturnsEmpty() {

      when(jpaRepository.findById(userId.id())).thenReturn(Optional.empty());

      Optional<User> result = repository.findById(userId);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Command Operations")
  class CommandOperations {

    @Test
    @DisplayName("save: converts domain through UserJpaEntity helper, persists, and returns mapped result")
    void saveTransformsAndPersists() {

      User domainInput = new User(userId, Instant.now(), email, Instant.now(), Instant.now());
      UserJpaEntity savedEntity = new UserJpaEntity(userId.id(), email, Instant.now(), Instant.now(), Instant.now());

      when(jpaRepository.save(any(UserJpaEntity.class))).thenReturn(savedEntity);

      User result = repository.save(domainInput);

      assertThat(result).isNotNull();
      assertThat(result.getUserId()).isEqualTo(userId);
      assertThat(result.getEmail()).isEqualTo(email);
      verify(jpaRepository).save(any(UserJpaEntity.class));
    }
  }
}