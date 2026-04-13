package com.laderrco.fortunelink.portfolio.infrastructure.persistence.repositories;

import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.GainsAggregation;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.AccountJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.PortfolioJpaEntity;
import com.laderrco.fortunelink.portfolio.infrastructure.persistence.entities.RealizedGainJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RealizedGainsQueryRepositoryImpl Unit Tests")
class RealizedGainsQueryRepositoryImplTest {

  @Mock
  private JpaRealizedGainRepository jpaRepository;

  @InjectMocks
  private RealizedGainsQueryRepositoryImpl repository;

  private static final String CAD = "CAD";
  private static final UUID RAW_PORTFOLIO_ID = UUID.randomUUID();
  private static final UUID RAW_USER_ID = UUID.randomUUID();
  private static final UUID RAW_ACCOUNT_ID = UUID.randomUUID();
  private static final AccountId ACCOUNT_ID = AccountId.fromString(RAW_ACCOUNT_ID.toString());
  private static final AssetSymbol SYMBOL = new AssetSymbol("AAPL");
  private static final int TAX_YEAR = 2024;
  private static final Pageable PAGEABLE = PageRequest.of(0, 10);

  @Nested
  @DisplayName("Paginated Query Operations")
  class PaginatedQueries {

    @Test
    @DisplayName("findByAccountId should map paged entities to domain records")
    void findByAccountIdShouldMapToDomain() {
      Page<RealizedGainJpaEntity> entityPage = new PageImpl<>(List.of(createMockEntity()));
      when(jpaRepository.findByAccountId(RAW_ACCOUNT_ID, PAGEABLE)).thenReturn(entityPage);

      Page<RealizedGainRecord> result = repository.findByAccountId(ACCOUNT_ID, PAGEABLE);

      assertThat(result.getContent()).hasSize(1);
      assertThat(result.getContent().get(0).symbol()).isEqualTo(SYMBOL);
      verify(jpaRepository).findByAccountId(RAW_ACCOUNT_ID, PAGEABLE);
    }

    @Test
    @DisplayName("findByAccountIdAndYear should delegate with correct parameters")
    void findByAccountIdAndYearShouldDelegate() {
      when(jpaRepository.findByAccountIdAndYear(eq(RAW_ACCOUNT_ID), eq(TAX_YEAR), any()))
          .thenReturn(Page.empty());

      repository.findByAccountIdAndYear(ACCOUNT_ID, TAX_YEAR, PAGEABLE);

      verify(jpaRepository).findByAccountIdAndYear(RAW_ACCOUNT_ID, TAX_YEAR, PAGEABLE);
    }

    @Test
    @DisplayName("findByAccountIdAndSymbol should delegate with raw string symbol")
    void findByAccountIdAndSymbolShouldDelegate() {
      when(jpaRepository.findByAccountIdAndSymbol(eq(RAW_ACCOUNT_ID), eq("AAPL"), any()))
          .thenReturn(Page.empty());

      repository.findByAccountIdAndSymbol(ACCOUNT_ID, SYMBOL, PAGEABLE);

      verify(jpaRepository).findByAccountIdAndSymbol(RAW_ACCOUNT_ID, "AAPL", PAGEABLE);
    }

    @Test
    @DisplayName("findByAccountIdAndYearAndSymbol should delegate with all parameters")
    void findByAccountIdAndYearAndSymbolShouldDelegate() {
      when(jpaRepository.findByAccountIdAndYearAndSymbol(any(), anyInt(), anyString(), any()))
          .thenReturn(Page.empty());

      repository.findByAccountIdAndYearAndSymbol(ACCOUNT_ID, TAX_YEAR, SYMBOL, PAGEABLE);

      verify(jpaRepository).findByAccountIdAndYearAndSymbol(RAW_ACCOUNT_ID, TAX_YEAR, "AAPL", PAGEABLE);
    }
  }

  @Nested
  @DisplayName("Aggregation and Metadata Operations")
  class AggregationOperations {

    @Test
    @DisplayName("calculateTotals should handle null symbol and return aggregation")
    void calculateTotalsShouldHandleNullSymbol() {
      GainsAggregation expectedAggregation = mock(GainsAggregation.class);
      when(jpaRepository.calculateTotals(RAW_ACCOUNT_ID, TAX_YEAR, null))
          .thenReturn(expectedAggregation);

      GainsAggregation result = repository.calculateTotals(ACCOUNT_ID, TAX_YEAR, null);

      assertThat(result).isEqualTo(expectedAggregation);
    }

    @Test
    @DisplayName("calculateTotals should pass symbol string when provided")
    void calculateTotalsShouldPassSymbolString() {
      repository.calculateTotals(ACCOUNT_ID, TAX_YEAR, SYMBOL);

      verify(jpaRepository).calculateTotals(RAW_ACCOUNT_ID, TAX_YEAR, "AAPL");
    }

    @Test
    @DisplayName("findAccountCurrencyCode should return optional string from jpa")
    void findAccountCurrencyCodeShouldReturnResult() {
      when(jpaRepository.findAccountCurrencyById(RAW_ACCOUNT_ID)).thenReturn(Optional.of("USD"));

      Optional<String> result = repository.findAccountCurrencyCode(ACCOUNT_ID);

      assertThat(result).contains("USD");
    }
  }

  
  private PortfolioJpaEntity createPortfolio() {
    return PortfolioJpaEntity.create(
        RAW_PORTFOLIO_ID, RAW_USER_ID, "Portfolio Name", "Desc", CAD, false, null, null,
        Instant.now(), Instant.now());
  }

  private AccountJpaEntity createAccount() {
    return AccountJpaEntity.create(RAW_ACCOUNT_ID, createPortfolio(), "name", "TFSA", CAD, "ACB", "HEALTHY", "ACTIVE",
        null, CAD, null, Instant.now(), Instant.now());
  }

  private RealizedGainJpaEntity createMockEntity() {
    RealizedGainJpaEntity entity = RealizedGainJpaEntity.create(
        UUID.randomUUID(),
        createAccount(),
        "AAPL",
        BigDecimal.ZERO,
        CAD,
        BigDecimal.ZERO,
        CAD,
        Instant.now());
    return entity;
  }
}