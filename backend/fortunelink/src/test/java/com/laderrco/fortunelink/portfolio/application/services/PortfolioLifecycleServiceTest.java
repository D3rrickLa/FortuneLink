package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioDeletionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioLimitReachedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.application.views.PortfolioView;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioAlreadyDeletedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioLifecycleService Unit Tests")
class PortfolioLifecycleServiceTest {

  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final Currency USD = Currency.USD;

  @Mock
  private PortfolioRepository portfolioRepository;
  @Mock
  private PortfolioViewMapper portfolioViewMapper;
  @Mock
  private PortfolioLifecycleCommandValidator validator;

  @Mock
  private PortfolioLoader portfolioLoader;

  @InjectMocks
  private PortfolioLifecycleService service;

  @BeforeEach
  void setUp() {
    // Validation passes by default
    lenient().when(validator.validate(any(CreatePortfolioCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(UpdatePortfolioCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(DeletePortfolioCommand.class)))
        .thenReturn(ValidationResult.success());
  }

  @Nested
  @DisplayName("createPortfolio Logic")
  class CreatePortfolioTests {
    @Test
    @DisplayName("createPortfolio: creates default account when flag is set")
    void createsDefaultAccount() {
      when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(i -> i.getArguments()[0]);

      CreatePortfolioCommand command = new CreatePortfolioCommand(USER_ID, "Name", "Desc", USD,
          true, AccountType.MARGIN, PositionStrategy.ACB);
      service.createPortfolio(command);

      verify(portfolioRepository).save(argThat(p -> !p.getAccounts().isEmpty()));
      verify(portfolioViewMapper).toNewPortfolioView(any());
    }

    @Test
    @DisplayName("createPortfolio: throws PortfolioLimitReachedException when active portfolio exists (Guard)")
    void throwsPortfolioLimitReachedViaGuard() {
      when(portfolioRepository.existsActiveByUserId(USER_ID)).thenReturn(true);

      CreatePortfolioCommand command = new CreatePortfolioCommand(USER_ID, "Name", "Desc", USD,
          false, null, PositionStrategy.ACB);

      assertThatThrownBy(() -> service.createPortfolio(command)).isInstanceOf(
              PortfolioLimitReachedException.class)
          .hasMessageContaining("User already has an active portfolio");

      verify(portfolioRepository, never()).save(any(Portfolio.class));
    }

    @Test
    @DisplayName("createPortfolio: handles race condition via DataIntegrityViolationException")
    void handlesRaceCondition() {
      when(portfolioRepository.existsActiveByUserId(USER_ID)).thenReturn(false);
      when(portfolioRepository.save(any(Portfolio.class))).thenThrow(
          new DataIntegrityViolationException("Duplicate Key"));

      CreatePortfolioCommand command = new CreatePortfolioCommand(USER_ID, "Name", "Desc", USD,
          false, null, PositionStrategy.ACB);

      assertThatThrownBy(() -> service.createPortfolio(command)).isInstanceOf(
              PortfolioLimitReachedException.class)
          .hasMessageContaining("A portfolio was recently created for this user.");
    }

    @Test
    @DisplayName("createPortfolio: successful path")
    void createsPortfolioSuccessfully() {
      when(portfolioRepository.existsActiveByUserId(USER_ID)).thenReturn(false);
      when(portfolioRepository.save(any(Portfolio.class))).thenReturn(
          Portfolio.createNew(USER_ID, "Portfolio", "My desc", USD));

      CreatePortfolioCommand command = new CreatePortfolioCommand(USER_ID, "Name", "Desc", USD,
          false, null, PositionStrategy.ACB);
      when(portfolioViewMapper.toNewPortfolioView(any())).thenReturn(
          new PortfolioView(PORTFOLIO_ID, USER_ID, "Portfolio", "My Desc", List.of(), null, false,
              null, null));
      PortfolioView result = service.createPortfolio(command);

      assertThat(result).isNotNull();
      verify(portfolioRepository).save(any(Portfolio.class));
    }
  }

  @Nested
  @DisplayName("updatePortfolio Logic")
  class UpdatePortfolioTests {
    @Test
    @DisplayName("updatePortfolio: updates details and returns lightweight view")
    void updatesDetailsAndReturnsView() {
      UpdatePortfolioCommand command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "New Name",
          "New Desc", USD);
      Portfolio existingPortfolio = Portfolio.createNew(USER_ID, "TFSA", "MY TFSA", USD);

      when(portfolioLoader.loadUserPortfolioWithGraph(eq(PORTFOLIO_ID), eq(USER_ID))).thenReturn(
          existingPortfolio);

      service.updatePortfolio(command);

      verify(portfolioRepository).save(existingPortfolio);
      assertThat(existingPortfolio.getDescription()).isEqualTo(command.description());
      assertThat(existingPortfolio.getName()).isEqualTo(command.name());
      assertThat(existingPortfolio.getDisplayCurrency()).isEqualTo(command.currency());
    }

    @Test
    @DisplayName("updatePortfolio: ensures validation occurs before updating")
    void validatesBeforeTransaction() {
      UpdatePortfolioCommand command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "Name",
          "Desc", USD);
      when(validator.validate(command)).thenReturn(ValidationResult.failure("Invalid Name"));

      assertThatThrownBy(() -> service.updatePortfolio(command)).isInstanceOf(
          InvalidCommandException.class);
    }
  }

  @Nested
  @DisplayName("deletePortfolio Branching logic")
  class DeletePortfolioTests {
    static Stream<Arguments> softDeleteExceptionProvider() {
      return Stream.of(
          // Source Exception -> Expected Message (or partial match)
          Arguments.of(new PortfolioNotEmptyException("error"),
              "close accounts first or use recursive delete"),
          Arguments.of(new PortfolioAlreadyDeletedException("Already gone"), "Already gone"),
          Arguments.of(new IllegalStateException("Bad state"), "Bad state"),
          Arguments.of(new PortfolioDeletionException("Direct error"), "Direct error"));
    }

    static Stream<Arguments> recursiveDeleteAccountProvider() {
      return Stream.of(Arguments.of(false, 1, Money.of(100, "USD"), "Success"),
          Arguments.of(true, 0, Money.zero(Currency.USD), "Success"),
          Arguments.of(true, 5, Money.zero(Currency.USD), "Failure"),
          Arguments.of(true, 0, Money.of(50, "USD"), "Failure"));
    }

    @Test
    @DisplayName("hard delete: fails when active accounts exist and recursive is false")
    void hardDeleteFailsWithActiveAccounts() {
      Portfolio portfolio = mock(Portfolio.class);
      Account activeAccount = mock(Account.class);

      when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(
          Optional.of(portfolio));
      when(portfolio.getAccounts()).thenReturn(List.of(activeAccount));
      when(activeAccount.isActive()).thenReturn(true);

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, false, false);

      assertThatThrownBy(() -> service.deletePortfolio(cmd)).isInstanceOf(
              PortfolioDeletionException.class)
          .hasMessageContaining("Cannot hard delete portfolio with 1 active account(s)");

      verify(portfolioRepository, never()).delete(any());
    }

    @Test
    @DisplayName("hard delete: closes empty accounts and deletes when recursive is true")
    void hardDeleteRecursiveSuccess() {
      Portfolio portfolio = mock(Portfolio.class);
      Account emptyAccount = mock(Account.class);
      AccountId accId = AccountId.newId();

      when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(
          Optional.of(portfolio));
      when(portfolio.getAccounts()).thenReturn(List.of(emptyAccount));

      // 1st call (guard): true
      // 2nd call (action loop): true
      // 3rd call (final hardDelete check): false (simulating it is now closed)
      when(emptyAccount.isActive()).thenReturn(true, true, false);

      when(emptyAccount.getPositionCount()).thenReturn(0);
      when(emptyAccount.getCashBalance()).thenReturn(Money.zero(USD));
      when(emptyAccount.getAccountId()).thenReturn(accId);

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, false, true);

      service.deletePortfolio(cmd);

      // Now this will be invoked because the filter inside the forEach sees 'true'
      verify(portfolio).closeAccount(accId);
      verify(portfolioRepository).delete(PORTFOLIO_ID);
    }

    @Test
    @DisplayName("hard delete: recursive fails if active accounts have positions")
    void hardDeleteRecursiveFailsWithAssets() {
      Portfolio portfolio = mock(Portfolio.class);
      Account busyAccount = mock(Account.class);

      when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(
          Optional.of(portfolio));
      when(portfolio.getAccounts()).thenReturn(List.of(busyAccount));

      when(busyAccount.isActive()).thenReturn(true);
      when(busyAccount.getPositionCount()).thenReturn(5); // Not empty!

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, false, true);
      assertThatThrownBy(() -> service.deletePortfolio(cmd)).isInstanceOf(
              PortfolioDeletionException.class)
          .hasMessageContaining("zero positions and zero cash balance");

      verify(portfolioRepository, never()).delete(any());
    }

    @Test
    @DisplayName("soft delete: recursive delete fails if accounts have cash")
    void recursiveDeleteFailsWithCash() {
      Portfolio portfolio = mock(Portfolio.class);
      Account account = mock(Account.class);

      when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(
          Optional.of(portfolio));
      when(portfolio.getAccounts()).thenReturn(List.of(account));
      when(account.isActive()).thenReturn(true);
      when(account.getCashBalance()).thenReturn(new Money(BigDecimal.TEN, USD));

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true);

      assertThatThrownBy(() -> service.deletePortfolio(cmd)).isInstanceOf(
              PortfolioDeletionException.class)
          .hasMessageContaining("zero positions and zero cash balance");
    }

    @Test
    @DisplayName("soft delete: successful recursive close of empty accounts")
    void recursiveDeleteClosesEmptyAccounts() {
      Portfolio portfolio = mock(Portfolio.class);
      Account activeAccount = mock(Account.class);
      AccountId accId = AccountId.newId();

      when(portfolioRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(portfolio));
      when(portfolio.getAccounts()).thenReturn(List.of(activeAccount));
      when(activeAccount.isActive()).thenReturn(true);
      when(activeAccount.getPositionCount()).thenReturn(0);
      when(activeAccount.getCashBalance()).thenReturn(Money.zero(USD));
      when(activeAccount.getAccountId()).thenReturn(accId);

      service.deletePortfolio(new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true));

      verify(portfolio).closeAccount(accId);
      verify(portfolio).markAsDeleted(USER_ID);
      verify(portfolioRepository).save(portfolio);
    }

    @ParameterizedTest
    @MethodSource("softDeleteExceptionProvider")
    void deletePortfolioSoftDeleteShouldHandleExceptions(Exception thrownException,
        String expectedMessage) {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      DeletePortfolioCommand command = new DeletePortfolioCommand(portfolioId, userId, true, false);

      Portfolio portfolio = mock(Portfolio.class);

      when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(
          Optional.of(portfolio));

      doThrow(thrownException).when(portfolio).markAsDeleted(userId);

      PortfolioDeletionException ex = assertThrows(PortfolioDeletionException.class,
          () -> service.deletePortfolio(command));

      assertTrue(ex.getMessage().contains(expectedMessage));
    }

    @ParameterizedTest
    @MethodSource("recursiveDeleteAccountProvider")
    void deletePortfolioRecursiveTestsAccountEligibility(boolean isActive, int positionCount,
        Money cashBalance, String expectedResult) {
      PortfolioId portfolioId = PortfolioId.newId();
      UserId userId = UserId.random();
      DeletePortfolioCommand command = new DeletePortfolioCommand(portfolioId, userId, true, true);
      Account mockAccount = mock(Account.class);

      when(mockAccount.isActive()).thenReturn(isActive);
      if (isActive) {
        // These only matter if the account is active (due to short-circuiting)
        when(mockAccount.getPositionCount()).thenReturn(positionCount);
        if (positionCount == 0) {
          when(mockAccount.getCashBalance()).thenReturn(cashBalance);
        }
      }

      Portfolio portfolio = mock(Portfolio.class);
      when(portfolio.getAccounts()).thenReturn(List.of(mockAccount));

      when(portfolioRepository.findByIdAndUserId(portfolioId, userId)).thenReturn(
          Optional.of(portfolio));

      if ("Failure".equals(expectedResult)) {
        // If anyMatch is true, your code likely throws an exception downstream
        // (assuming closeAllEligibleAccounts throws if it finds non-empty accounts)
        assertThrows(PortfolioDeletionException.class, () -> service.deletePortfolio(command));
      } else {
        service.deletePortfolio(command);
        verify(portfolioRepository).save(portfolio);
      }
    }

    @Test
    @DisplayName("deletePortfolio: throws exception when portfolio not found")
    void deletePortfolioThrowsWhenPortfolioNotFound() {
      when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(
          Optional.empty());

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, false, false);
      assertThatThrownBy(() -> service.deletePortfolio(cmd)).isInstanceOf(
          PortfolioNotFoundException.class);
    }
  }
}