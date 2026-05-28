package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.UserPreferences;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.UserPreferencesEntity;

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
class UserPreferencesRepositoryImplTest {

  @Mock
  private JpaUserPreferencesRepository jpaRepository;

  @InjectMocks
  private UserPreferencesRepositoryImpl repository;

  private final UserId userId = new UserId(UUID.randomUUID());

  @Nested
  @DisplayName("Query Operations")
  class QueryOperations {

    @Test
    @DisplayName("findById: returns mapped domain object when entity exists")
    void findByIdReturnsMappedDomainWhenFound() {

      UserPreferencesEntity entity = new UserPreferencesEntity();
      entity.setUserId(userId.id());
      entity.setBaseCurrency("CAD");
      entity.setEmailNotifications(true);
      entity.setPriceAlerts(false);
      entity.setDateFormat("YYYY-MM-DD");

      when(jpaRepository.findById(userId.id())).thenReturn(Optional.of(entity));

      Optional<UserPreferences> result = repository.findById(userId);

      assertThat(result).isPresent();
      UserPreferences domain = result.get();
      assertThat(domain.getUserId()).isEqualTo(userId);
      assertThat(domain.getBaseCurrency()).isEqualTo(Currency.of("CAD"));
      assertThat(domain.isEmailNotifications()).isTrue();
      assertThat(domain.isPriceAlerts()).isFalse();
      assertThat(domain.getDateFormat()).isEqualTo("YYYY-MM-DD");
    }

    @Test
    @DisplayName("findById: returns empty Optional when entity does not exist")
    void findByIdReturnsEmptyOptionalWhenNotFound() {

      when(jpaRepository.findById(userId.id())).thenReturn(Optional.empty());

      Optional<UserPreferences> result = repository.findById(userId);

      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("existsById: delegates to underlying JPA repository")
    void existsByIdDelegatesToJpa() {

      when(jpaRepository.existsById(userId.id())).thenReturn(true);

      boolean exists = repository.existsById(userId);

      assertThat(exists).isTrue();
      verify(jpaRepository).existsById(userId.id());
    }
  }

  @Nested
  @DisplayName("Command Operations")
  class CommandOperations {

    @Test
    @DisplayName("save: converts domain properties to entity, commits, and returns updated domain mapping")
    void saveConvertsMapsAndPersists() {

      UserPreferences domainInput = new UserPreferences(
          userId,
          Currency.of("EUR"),
          false,
          true,
          "DD/MM/YYYY");

      UserPreferencesEntity savedEntity = new UserPreferencesEntity();
      savedEntity.setUserId(userId.id());
      savedEntity.setBaseCurrency("EUR");
      savedEntity.setEmailNotifications(false);
      savedEntity.setPriceAlerts(true);
      savedEntity.setDateFormat("DD/MM/YYYY");

      ArgumentCaptor<UserPreferencesEntity> entityCaptor = ArgumentCaptor.forClass(UserPreferencesEntity.class);
      when(jpaRepository.save(entityCaptor.capture())).thenReturn(savedEntity);

      UserPreferences result = repository.save(domainInput);

      UserPreferencesEntity capturedEntity = entityCaptor.getValue();
      assertThat(capturedEntity.getUserId()).isEqualTo(userId.id());
      assertThat(capturedEntity.getBaseCurrency()).isEqualTo("EUR");
      assertThat(capturedEntity.isEmailNotifications()).isFalse();
      assertThat(capturedEntity.isPriceAlerts()).isTrue();
      assertThat(capturedEntity.getDateFormat()).isEqualTo("DD/MM/YYYY");

      assertThat(result).isNotNull();
      assertThat(result.getUserId()).isEqualTo(userId);
      assertThat(result.getBaseCurrency()).isEqualTo(Currency.of("EUR"));
      assertThat(result.isEmailNotifications()).isFalse();
      assertThat(result.isPriceAlerts()).isTrue();
      assertThat(result.getDateFormat()).isEqualTo("DD/MM/YYYY");
    }
  }
}