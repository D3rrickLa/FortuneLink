package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserProfile;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.UserProfileEntity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileRepositoryImplTest {

  @Mock
  private JpaUserProfileRepository jpa;

  @InjectMocks
  private UserProfileRepositoryImpl repository;

  private final UserId userId = new UserId(UUID.randomUUID());
  private final String fullName = "John Doe";

  @Nested
  @DisplayName("Query Operations")
  class QueryOperations {

    @Test
    @DisplayName("findById: returns mapped UserProfile when database entity exists")
    void findByIdReturnsMappedDomainWhenFound() {

      UserProfileEntity entity = new UserProfileEntity();
      entity.setUserId(userId.id());
      entity.setFullName(fullName);

      when(jpa.findById(userId.id())).thenReturn(Optional.of(entity));

      Optional<UserProfile> result = repository.findById(userId);

      assertThat(result).isPresent();
      assertThat(result.get().getUserId()).isEqualTo(userId);
      assertThat(result.get().getFullName()).isEqualTo(fullName);
    }

    @Test
    @DisplayName("findById: returns empty Optional when entity does not exist")
    void findByIdReturnsEmptyOptionalWhenNotFound() {

      when(jpa.findById(userId.id())).thenReturn(Optional.empty());

      Optional<UserProfile> result = repository.findById(userId);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  @DisplayName("Command Operations")
  class CommandOperations {

    @Test
    @DisplayName("save: maps domain to entity, saves to DB, and maps result back to domain")
    void saveConvertsAndPersists() {

      UserProfile domainInput = new UserProfile(userId, fullName);

      UserProfileEntity savedEntity = new UserProfileEntity();
      savedEntity.setUserId(userId.id());
      savedEntity.setFullName(fullName);

      ArgumentCaptor<UserProfileEntity> entityCaptor = ArgumentCaptor.forClass(UserProfileEntity.class);
      when(jpa.save(entityCaptor.capture())).thenReturn(savedEntity);

      UserProfile result = repository.save(domainInput);

      UserProfileEntity captured = entityCaptor.getValue();
      assertThat(captured.getUserId()).isEqualTo(userId.id());
      assertThat(captured.getFullName()).isEqualTo(fullName);

      assertThat(result).isNotNull();
      assertThat(result.getUserId()).isEqualTo(userId);
      assertThat(result.getFullName()).isEqualTo(fullName);
    }
  }
}