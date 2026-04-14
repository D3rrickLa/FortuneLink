package com.laderrco.fortunelink.portfolio.application.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
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

@ExtendWith(MockitoExtension.class)
class PortfolioLoaderTest {

  private static final PortfolioId PID = PortfolioId.newId();
  private static final UserId UID = UserId.random();
  private static final AccountId AID = AccountId.newId();

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
    void loadUserPortfolioshouldReturnPortfolio() {
      when(portfolioRepository.findByIdAndUserId(PID, UID)).thenReturn(Optional.of(portfolio));
      when(portfolio.isDeleted()).thenReturn(false);

      Portfolio result = portfolioLoader.loadUserPortfolio(PID, UID);

      assertEquals(portfolio, result);
    }

    @Test
    @DisplayName("loadUserPortfolio: should throw exception when portfolio is marked as deleted")
    void loadUserPortfolioshouldThrowWhenDeleted() {
      when(portfolioRepository.findByIdAndUserId(PID, UID)).thenReturn(Optional.of(portfolio));
      when(portfolio.isDeleted()).thenReturn(true);

      assertThrows(PortfolioNotFoundException.class,
          () -> portfolioLoader.loadUserPortfolio(PID, UID));
    }

    @Test
    @DisplayName("loadUserPortfolioWithGraph: should return portfolio with relations when not deleted")
    void loadUserPortfolioWithGraphshouldReturnPortfolio() {
      when(portfolioRepository.findWithAccountsByIdAndUserId(PID, UID)).thenReturn(
          Optional.of(portfolio));
      when(portfolio.isDeleted()).thenReturn(false);

      Portfolio result = portfolioLoader.loadUserPortfolioWithGraph(PID, UID);

      assertEquals(portfolio, result);
    }
  }

  @Nested
  @DisplayName("loadAllUserPortfolios: Bulk Loading")
  class LoadAllPortfoliosTests {

    @Test
    @DisplayName("loadAllUserPortfolios: should return list of active portfolios")
    void loadAllUserPortfoliosshouldReturnList() {
      List<Portfolio> expectedList = List.of(portfolio);
      when(portfolioRepository.findAllActiveByUserId(UID)).thenReturn(expectedList);

      List<Portfolio> result = portfolioLoader.loadAllUserPortfolios(UID);

      assertEquals(expectedList, result);
      assertEquals(1, result.size());
    }
  }

  @Nested
  @DisplayName("Validation Logic: Ownership and Existence")
  class ValidationTests {

    @Test
    @DisplayName("validateOwnership: should throw exception when ownership check fails")
    void validateOwnershipshouldThrowWhenNotFound() {
      when(portfolioRepository.existsByIdAndUserId(PID, UID)).thenReturn(false);

      assertThrows(PortfolioNotFoundException.class,
          () -> portfolioLoader.validateOwnership(PID, UID));
    }

    @Test
    @DisplayName("validateAccountOwnershipToPortfolio: should throw exception when account does not belong to portfolio")
    void validateAccountOwnershipToPortfolioshouldThrowWhenRelationMissing() {
      when(portfolioRepository.existsByPortfolioIdAndAccountId(PID, AID)).thenReturn(false);

      assertThrows(AccountNotFoundException.class,
          () -> portfolioLoader.validateAccountOwnershipToPortfolio(PID, AID));
    }

    @Test
    @DisplayName("validatePortfolioAndAccountOwnership: should throw exception when the tripartite check fails")
    void validatePortfolioAndAccountOwnershipshouldThrowWhenAnyLinkMissing() {
      when(portfolioRepository.existsByIdAndUserIdAndAccountId(PID, UID, AID)).thenReturn(false);

      assertThrows(PortfolioNotFoundException.class,
          () -> portfolioLoader.validatePortfolioAndAccountOwnership(PID, UID, AID));
    }
  }

  @Nested
  @DisplayName("Common Edge Cases: Parameterized missing data")
  class EdgeCaseTests {
    @ParameterizedTest
    @CsvSource({"loadUserPortfolio", "loadUserPortfolioWithGraph"})
    @DisplayName("loadingMethods: should throw PortfolioNotFoundException when repository returns empty")
    void loadingMethodsshouldThrowWhenEmpty(String methodName) {
      if (methodName.equals("loadUserPortfolio")) {
        when(portfolioRepository.findByIdAndUserId(PID, UID)).thenReturn(Optional.empty());
        assertThrows(PortfolioNotFoundException.class,
            () -> portfolioLoader.loadUserPortfolio(PID, UID));
      } else {
        when(portfolioRepository.findWithAccountsByIdAndUserId(PID, UID)).thenReturn(
            Optional.empty());
        assertThrows(PortfolioNotFoundException.class,
            () -> portfolioLoader.loadUserPortfolioWithGraph(PID, UID));
      }
    }

    @ParameterizedTest
    @CsvSource({"loadUserPortfolio", "loadUserPortfolioWithGraph"})
    @DisplayName("Loading methods should throw PortfolioNotFoundException when repository returns empty")
    void loadingMethodsShouldThrowWhenEmpty(String methodName) {
      if (methodName.equals("loadUserPortfolio")) {
        when(portfolioRepository.findByIdAndUserId(PID, UID)).thenReturn(Optional.empty());
        assertThrows(PortfolioNotFoundException.class,
            () -> portfolioLoader.loadUserPortfolio(PID, UID));
      } else {
        when(portfolioRepository.findWithAccountsByIdAndUserId(PID, UID)).thenReturn(
            Optional.empty());
        assertThrows(PortfolioNotFoundException.class,
            () -> portfolioLoader.loadUserPortfolioWithGraph(PID, UID));
      }
    }

    @Test
    @DisplayName("loadUserPortfolioWithGraph: should throw PortfolioNotFoundException when portfolio is deleted")
    void loadWithGraphShouldThrowWhenDeleted() {
      Portfolio deletedPortfolio = mock(Portfolio.class);
      when(deletedPortfolio.isDeleted()).thenReturn(true);

      when(portfolioRepository.findWithAccountsByIdAndUserId(PID, UID)).thenReturn(
          Optional.of(deletedPortfolio));

      assertThrows(PortfolioNotFoundException.class,
          () -> portfolioLoader.loadUserPortfolioWithGraph(PID, UID));

      verify(portfolioRepository).findWithAccountsByIdAndUserId(PID, UID);
    }

    @Test
    @DisplayName("loadUserPortfolioWithGraph: should return portfolio when exists and not deleted")
    void loadWithGraphSuccess() {
      Portfolio activePortfolio = mock(Portfolio.class);
      when(activePortfolio.isDeleted()).thenReturn(false);

      when(portfolioRepository.findWithAccountsByIdAndUserId(PID, UID)).thenReturn(
          Optional.of(activePortfolio));

      Portfolio result = portfolioLoader.loadUserPortfolioWithGraph(PID, UID);

      assertThat(result).isEqualTo(activePortfolio);
      verify(portfolioRepository).findWithAccountsByIdAndUserId(PID, UID);
    }
  }

  @Nested
  @DisplayName("Validation Logic: Happy Paths")
  class ValidationHappyPathTests {

    @Test
    @DisplayName("validateOwnership: should complete normally when ownership exists")
    void validateOwnershipshouldSucceedWhenOwnershipExists() {

      when(portfolioRepository.existsByIdAndUserId(PID, UID)).thenReturn(true);

      portfolioLoader.validateOwnership(PID, UID);

    }

    @Test
    @DisplayName("validateAccountOwnershipToPortfolio: should complete normally when account belongs to portfolio")
    void validateAccountOwnershipToPortfolioshouldSucceedWhenRelationExists() {

      when(portfolioRepository.existsByPortfolioIdAndAccountId(PID, AID)).thenReturn(true);

      portfolioLoader.validateAccountOwnershipToPortfolio(PID, AID);
    }

    @Test
    @DisplayName("validatePortfolioAndAccountOwnership: should complete normally when tripartite link is valid")
    void validatePortfolioAndAccountOwnershipshouldSucceedWhenAllLinksExist() {

      when(portfolioRepository.existsByIdAndUserIdAndAccountId(PID, UID, AID)).thenReturn(true);

      portfolioLoader.validatePortfolioAndAccountOwnership(PID, UID, AID);
    }
  }
}