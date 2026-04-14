package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.PortfolioJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.mappers.PortfolioDomainMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioRepositoryImpl Unit Tests")
class PortfolioRepositoryImplTest {

  private static final UUID RAW_PORTFOLIO_ID = UUID.randomUUID();
  private static final UUID RAW_USER_ID = UUID.randomUUID();
  private static final UUID RAW_ACCOUNT_ID = UUID.randomUUID();
  private static final PortfolioId PORTFOLIO_ID = PortfolioId.fromString(
      RAW_PORTFOLIO_ID.toString());
  private static final UserId USER_ID = UserId.fromString(RAW_USER_ID.toString());
  private static final AccountId ACCOUNT_ID = AccountId.fromString(RAW_ACCOUNT_ID.toString());
  private static final String CAD = "CAD";
  @Mock
  private JpaPortfolioRepository jpaRepository;
  @Mock
  private PortfolioDomainMapper mapper;
  @InjectMocks
  private PortfolioRepositoryImpl repository;

  private PortfolioJpaEntity create() {
    return PortfolioJpaEntity.create(RAW_PORTFOLIO_ID, RAW_USER_ID, "Portfolio Name", "Desc", CAD,
        false, null, null, Instant.now(), Instant.now());
  }

  @Nested
  @DisplayName("Save Operations")
  class SaveOperations {

    @Test
    @DisplayName("save should merge with existing entity and return domain object")
    void saveShouldMergeWithExistingEntity() {

      Portfolio domain = mock(Portfolio.class);
      PortfolioJpaEntity existingEntity = create();
      PortfolioJpaEntity updatedEntity = create();
      updatedEntity.update("Updated Name", "new desc", CAD, false, null, null, Instant.now());

      when(domain.getPortfolioId()).thenReturn(PORTFOLIO_ID);
      when(jpaRepository.findWithAccountsById(RAW_PORTFOLIO_ID)).thenReturn(
          Optional.of(existingEntity));
      when(mapper.toEntity(domain, existingEntity)).thenReturn(updatedEntity);
      when(jpaRepository.save(updatedEntity)).thenReturn(updatedEntity);
      when(mapper.toDomain(updatedEntity)).thenReturn(domain);

      Portfolio result = repository.save(domain);

      assertThat(result).isEqualTo(domain);
      assertThat(result.getAccounts().size()).isEqualTo(0);
      assertThat(existingEntity.getAccounts().size()).isEqualTo(0);
      verify(jpaRepository).findWithAccountsById(RAW_PORTFOLIO_ID);
      verify(jpaRepository).save(updatedEntity);
    }

    @Test
    @DisplayName("save should throw exception when domain is null")
    void saveShouldThrowExceptionWhenDomainIsNull() {
      assertThatThrownBy(() -> repository.save(null)).isInstanceOf(NullPointerException.class)
          .hasMessageContaining("Portfolio cannot be null");
    }
  }

  @Nested
  @DisplayName("Query Operations")
  class QueryOperations {

    @Test
    @DisplayName("findByIdAndUserId should map result to domain")
    void findByIdAndUserIdShouldReturnDomain() {
      PortfolioJpaEntity entity = create();
      Portfolio domain = mock(Portfolio.class);

      when(jpaRepository.findByIdAndUserId(RAW_PORTFOLIO_ID, RAW_USER_ID)).thenReturn(
          Optional.of(entity));
      when(mapper.toDomain(entity)).thenReturn(domain);

      Optional<Portfolio> result = repository.findByIdAndUserId(PORTFOLIO_ID, USER_ID);

      assertThat(result).contains(domain);
    }

    @Test
    @DisplayName("findAllActiveByUserId should return list of domain objects")
    void findAllActiveByUserIdShouldReturnList() {
      when(jpaRepository.findAllActiveByUserId(RAW_USER_ID)).thenReturn(List.of(create()));
      when(mapper.toDomain(any())).thenReturn(mock(Portfolio.class));

      List<Portfolio> results = repository.findAllActiveByUserId(USER_ID);

      assertThat(results).hasSize(1);
      verify(jpaRepository).findAllActiveByUserId(RAW_USER_ID);
    }

    @Test
    @DisplayName("findWithAccountsByIdAndUserId should map entity to domain when found")
    void findWithAccountsByIdAndUserIdShouldReturnDomain() {
      PortfolioJpaEntity entity = create();
      Portfolio domain = mock(Portfolio.class);

      when(jpaRepository.findWithAccountsByIdAndUserId(RAW_PORTFOLIO_ID, RAW_USER_ID)).thenReturn(
          Optional.of(entity));
      when(mapper.toDomain(entity)).thenReturn(domain);

      Optional<Portfolio> result = repository.findWithAccountsByIdAndUserId(PORTFOLIO_ID, USER_ID);

      assertThat(result).contains(domain);
      verify(jpaRepository).findWithAccountsByIdAndUserId(RAW_PORTFOLIO_ID, RAW_USER_ID);
    }
  }

  @Nested
  @DisplayName("Existence Checks")
  class ExistenceChecks {

    @Test
    @DisplayName("existsByIdAndUserIdAndAccountId should call jpaRepository")
    void existsByIdAndUserIdAndAccountIdShouldCallJpa() {
      when(jpaRepository.existsByIdAndUserIdAndAccountId(RAW_PORTFOLIO_ID, RAW_USER_ID,
          RAW_ACCOUNT_ID)).thenReturn(true);

      boolean exists = repository.existsByIdAndUserIdAndAccountId(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID);

      assertThat(exists).isTrue();
      verify(jpaRepository).existsByIdAndUserIdAndAccountId(RAW_PORTFOLIO_ID, RAW_USER_ID,
          RAW_ACCOUNT_ID);
    }

    @Test
    @DisplayName("countByUserId should return count from jpaRepository")
    void countByUserIdShouldReturnCount() {
      when(jpaRepository.countActiveByUserId(RAW_USER_ID)).thenReturn(5L);

      Long count = repository.countByUserId(USER_ID);

      assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("existsActiveByUserId should delegate to jpaRepository")
    void existsActiveByUserIdShouldCallJpa() {
      when(jpaRepository.existsActiveByUserId(RAW_USER_ID)).thenReturn(true);

      boolean exists = repository.existsActiveByUserId(USER_ID);

      assertThat(exists).isTrue();
      verify(jpaRepository).existsActiveByUserId(RAW_USER_ID);
    }

    @Test
    @DisplayName("existsByIdAndUserId should delegate to jpaRepository")
    void existsByIdAndUserIdShouldCallJpa() {
      when(jpaRepository.existsByIdAndUserId(RAW_PORTFOLIO_ID, RAW_USER_ID)).thenReturn(true);

      boolean exists = repository.existsByIdAndUserId(PORTFOLIO_ID, USER_ID);

      assertThat(exists).isTrue();
      verify(jpaRepository).existsByIdAndUserId(RAW_PORTFOLIO_ID, RAW_USER_ID);
    }

    @Test
    @DisplayName("existsByPortfolioIdAndAccountId should delegate to jpaRepository")
    void existsByPortfolioIdAndAccountIdShouldCallJpa() {
      when(jpaRepository.existsByIdAndAccountId(RAW_PORTFOLIO_ID, RAW_ACCOUNT_ID)).thenReturn(true);

      boolean exists = repository.existsByPortfolioIdAndAccountId(PORTFOLIO_ID, ACCOUNT_ID);

      assertThat(exists).isTrue();
      verify(jpaRepository).existsByIdAndAccountId(RAW_PORTFOLIO_ID, RAW_ACCOUNT_ID);
    }
  }

  @Nested
  @DisplayName("Mutation and Utility Operations")
  class UtilityOperations {

    @Test
    @DisplayName("delete should call jpaRepository with UUID")
    void deleteShouldCallJpa() {
      repository.delete(PORTFOLIO_ID);
      verify(jpaRepository).deleteById(RAW_PORTFOLIO_ID);
    }

    @Test
    @DisplayName("markAccountStale should delegate to jpaRepository")
    void markAccountStaleShouldDelegate() {
      repository.markAccountStale(ACCOUNT_ID);
      verify(jpaRepository).markAccountStale(RAW_ACCOUNT_ID);
    }

    @Test
    @DisplayName("findAllActiveUserIds should map UUIDs back to UserIds")
    void findAllActiveUserIdsShouldMapCorrectly() {
      List<UUID> uuids = List.of(RAW_USER_ID);
      when(jpaRepository.findAllActiveUserIds()).thenReturn(uuids);

      List<UserId> result = repository.findAllActiveUserIds();

      assertThat(result).hasSize(1);
      assertThat(result.get(0)).isEqualTo(USER_ID);
    }
  }
}