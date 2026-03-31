package com.laderrco.fortunelink.portfolio.application.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioLoaderTest {

  private static final PortfolioId P_ID = PortfolioId.newId();
  private static final UserId U_ID = UserId.random();
  private static final AccountId A_ID = AccountId.newId();

  @Mock
  private PortfolioRepository portfolioRepository;

  @Mock
  private Portfolio portfolio;

  @InjectMocks
  private PortfolioLoader portfolioLoader;

  @Nested
  @DisplayName("loadUserPortfolio: Single Portfolio Loading")
  class LoadUserPortfolioTests {

    @Test
    @DisplayName("loadUserPortfolio: should return portfolio when found and not deleted")
    void loadUserPortfolio_shouldReturnPortfolio() {
      when(portfolioRepository.findByIdAndUserId(P_ID, U_ID)).thenReturn(Optional.of(portfolio));
      when(portfolio.isDeleted()).thenReturn(false);

      Portfolio result = portfolioLoader.loadUserPortfolio(P_ID, U_ID);

      assertEquals(portfolio, result);
    }

    @Test
    @DisplayName("loadUserPortfolio: should throw exception when portfolio is marked as deleted")
    void loadUserPortfolio_shouldThrowWhenDeleted() {
      when(portfolioRepository.findByIdAndUserId(P_ID, U_ID)).thenReturn(Optional.of(portfolio));
      when(portfolio.isDeleted()).thenReturn(true);

      assertThrows(PortfolioNotFoundException.class, () -> portfolioLoader.loadUserPortfolio(P_ID, U_ID));
    }

    @Test
    @DisplayName("loadUserPortfolioWithGraph: should return portfolio with relations when not deleted")
    void loadUserPortfolioWithGraph_shouldReturnPortfolio() {
      when(portfolioRepository.findWithAccountsByIdAndUserId(P_ID, U_ID)).thenReturn(Optional.of(portfolio));
      when(portfolio.isDeleted()).thenReturn(false);

      Portfolio result = portfolioLoader.loadUserPortfolioWithGraph(P_ID, U_ID);

      assertEquals(portfolio, result);
    }
  }

  @Nested
  @DisplayName("loadAllUserPortfolios: Bulk Loading")
  class LoadAllPortfoliosTests {

    @Test
    @DisplayName("loadAllUserPortfolios: should return list of active portfolios")
    void loadAllUserPortfolios_shouldReturnList() {
      List<Portfolio> expectedList = List.of(portfolio);
      when(portfolioRepository.findAllActiveByUserId(U_ID)).thenReturn(expectedList);

      List<Portfolio> result = portfolioLoader.loadAllUserPortfolios(U_ID);

      assertEquals(expectedList, result);
      assertEquals(1, result.size());
    }
  }

  @Nested
  @DisplayName("Validation Logic: Ownership and Existence")
  class ValidationTests {

    @Test
    @DisplayName("validateOwnership: should throw exception when ownership check fails")
    void validateOwnership_shouldThrowWhenNotFound() {
      when(portfolioRepository.existsByIdAndUserId(P_ID, U_ID)).thenReturn(false);

      assertThrows(PortfolioNotFoundException.class, () -> portfolioLoader.validateOwnership(P_ID, U_ID));
    }

    @Test
    @DisplayName("validateAccountOwnershipToPortfolio: should throw exception when account does not belong to portfolio")
    void validateAccountOwnershipToPortfolio_shouldThrowWhenRelationMissing() {
      when(portfolioRepository.existsByPortfolioIdAndAccountId(P_ID, A_ID)).thenReturn(false);

      assertThrows(AccountNotFoundException.class,
          () -> portfolioLoader.validateAccountOwnershipToPortfolio(P_ID, A_ID));
    }

    @Test
    @DisplayName("validatePortfolioAndAccountOwnership: should throw exception when the tripartite check fails")
    void validatePortfolioAndAccountOwnership_shouldThrowWhenAnyLinkMissing() {
      when(portfolioRepository.existsByIdAndUserIdAndAccountId(P_ID, U_ID, A_ID)).thenReturn(false);

      assertThrows(PortfolioNotFoundException.class,
          () -> portfolioLoader.validatePortfolioAndAccountOwnership(P_ID, U_ID, A_ID));
    }
  }

  @Nested
  @DisplayName("Common Edge Cases: Parameterized missing data")
  class EdgeCaseTests {
    @ParameterizedTest
    @CsvSource({
        "loadUserPortfolio",
        "loadUserPortfolioWithGraph"
    })
    @DisplayName("loadingMethods: should throw PortfolioNotFoundException when repository returns empty")
    void loadingMethods_shouldThrowWhenEmpty(String methodName) {
      if (methodName.equals("loadUserPortfolio")) {
        when(portfolioRepository.findByIdAndUserId(P_ID, U_ID)).thenReturn(Optional.empty());
        assertThrows(PortfolioNotFoundException.class, () -> portfolioLoader.loadUserPortfolio(P_ID, U_ID));
      } else {
        when(portfolioRepository.findWithAccountsByIdAndUserId(P_ID, U_ID)).thenReturn(Optional.empty());
        assertThrows(PortfolioNotFoundException.class, () -> portfolioLoader.loadUserPortfolioWithGraph(P_ID, U_ID));
      }
    }
  }

  @Nested
  @DisplayName("Validation Logic: Happy Paths")
  class ValidationHappyPathTests {

    @Test
    @DisplayName("validateOwnership: should complete normally when ownership exists")
    void validateOwnership_shouldSucceedWhenOwnershipExists() {
      // Arrange
      when(portfolioRepository.existsByIdAndUserId(P_ID, U_ID)).thenReturn(true);

      // Act & Assert
      portfolioLoader.validateOwnership(P_ID, U_ID);
      // No exception thrown means success
    }

    @Test
    @DisplayName("validateAccountOwnershipToPortfolio: should complete normally when account belongs to portfolio")
    void validateAccountOwnershipToPortfolio_shouldSucceedWhenRelationExists() {
      // Arrange
      when(portfolioRepository.existsByPortfolioIdAndAccountId(P_ID, A_ID)).thenReturn(true);

      // Act & Assert
      portfolioLoader.validateAccountOwnershipToPortfolio(P_ID, A_ID);
    }

    @Test
    @DisplayName("validatePortfolioAndAccountOwnership: should complete normally when tripartite link is valid")
    void validatePortfolioAndAccountOwnership_shouldSucceedWhenAllLinksExist() {
      // Arrange
      when(portfolioRepository.existsByIdAndUserIdAndAccountId(P_ID, U_ID, A_ID)).thenReturn(true);

      // Act & Assert
      portfolioLoader.validatePortfolioAndAccountOwnership(P_ID, U_ID, A_ID);
    }
  }
}