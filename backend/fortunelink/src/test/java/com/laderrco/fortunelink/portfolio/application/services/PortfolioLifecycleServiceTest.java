package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.commands.ReopenAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeClosedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeReopenedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioDeletionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioLimitReachedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.AccountViewBuilder;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.UpdatePortfolioResult;
import com.laderrco.fortunelink.portfolio.application.validators.PortfolioLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
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
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioLifecycleService Unit Tests")
class PortfolioLifecycleServiceTest {
  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final Currency USD = Currency.USD;
  @Mock
  private PortfolioRepository portfolioRepository;
  @Mock
  private TransactionRepository transactionRepository;
  @Mock
  private PortfolioViewMapper portfolioViewMapper;
  @Mock
  private MarketDataService marketDataService;
  @Mock
  private PortfolioValuationService portfolioValuationService;
  @Mock
  private PortfolioLifecycleCommandValidator validator;
  @Mock
  private TransactionTemplate transactionTemplate;
  @Mock
  private AccountViewBuilder accountViewBuilder;
  @Mock
  private PortfolioLoader portfolioLoader;
  @InjectMocks
  private PortfolioLifecycleService service;

  @BeforeEach
  void setUp() {
    // Default mock for validator to return valid
    lenient().when(validator.validate(any(CreatePortfolioCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(UpdatePortfolioCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(DeletePortfolioCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(CreateAccountCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(UpdateAccountCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(ReopenAccountCommand.class)))
        .thenReturn(ValidationResult.success());
    lenient().when(validator.validate(any(DeleteAccountCommand.class)))
        .thenReturn(ValidationResult.success());
  }

  @Nested
  @DisplayName("createPortfolio Constraints")
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
    @DisplayName("createPortfolio: command throws Exception when defautl type is null")
    void createsDefaultAccountThrowsEexceptionFromCreatePortfolioCOmmand() {
      assertThatThrownBy(() -> new CreatePortfolioCommand(USER_ID, "Name", "Desc", USD, true, null,
          PositionStrategy.ACB)).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Default account type required");

    }

    @Test
    @DisplayName("createPortfolio: maps DataIntegrityViolation to PortfolioLimitReachedException")
    void mapsDataIntegrityException() {
      when(portfolioRepository.save(any())).thenThrow(
          new DataIntegrityViolationException("Unique constraint"));

      CreatePortfolioCommand command = new CreatePortfolioCommand(USER_ID, "Name", "Desc", USD,
          false, null, PositionStrategy.ACB);

      assertThatThrownBy(() -> service.createPortfolio(command)).isInstanceOf(
          PortfolioLimitReachedException.class);
    }

    @Test
    @DisplayName("createPortfolio: validator returns failure")
    void createsDefaultAccountThrowExceptionWithValidation() {
      when(validator.validate(any(CreatePortfolioCommand.class))).thenReturn(
          ValidationResult.failure("Everything is null"));
      CreatePortfolioCommand command = new CreatePortfolioCommand(USER_ID, "Name", "Desc", USD,
          true, AccountType.MARGIN, PositionStrategy.ACB);
      assertThatThrownBy(() -> service.createPortfolio(command)).isInstanceOf(
          InvalidCommandException.class);
    }
  }

  @Nested
  @DisplayName("updatePortfolio Logic & Resilience")
  class UpdatePortfolioTests {
    @Test
    @DisplayName("updatePortfolio: skips fee enrichment when accountIds is empty")
    void skipsFeesWhenNoAccounts() {
      Portfolio mockPortfolio = mock(Portfolio.class);
      when(mockPortfolio.getAccounts()).thenReturn(List.of());
      when(mockPortfolio.getDisplayCurrency()).thenReturn(USD);

      // Trigger the transaction block logic
      when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
        // Normally we'd test the lambda content here, but since we mock the result:
        return new UpdatePortfolioResult(mockPortfolio, List.of());
      });

      service.updatePortfolio(
          new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "Name", "Desc", USD));

      // Verify fee repository is never touched
      verify(transactionRepository, never()).sumBuyFeesByAccountAndSymbol(any());
      verify(portfolioViewMapper).toPortfolioView(eq(mockPortfolio), anyList(), any(),
          anyBoolean());
    }

    @Test
    @DisplayName("updatePortfolio: verifies transaction block execution (Write Path)")
    void verifiesTransactionBlockLogic() {
      Portfolio existing = mock(Portfolio.class);
      AccountId accId = new AccountId(UUID.randomUUID());
      Account account = mock(Account.class);

      when(account.getAccountId()).thenReturn(accId);
      when(existing.getAccounts()).thenReturn(List.of(account));
      when(existing.getDisplayCurrency()).thenReturn(USD);

      // Capture the lambda passed to transactionTemplate to verify internal logic
      when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
        TransactionCallback<UpdatePortfolioResult> callback = invocation.getArgument(0);
        // We mock the loader to simulate the inside of the lambda
        when(portfolioLoader.loadUserPortfolioWithGraph(any(), any())).thenReturn(existing);
        when(portfolioRepository.save(any())).thenReturn(existing);

        return callback.doInTransaction(null);
      });

      service.updatePortfolio(
          new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "New Name", "New Desc", USD));

      // Verify internal domain logic was called
      verify(existing).updateDetails("New Name", "New Desc");
      verify(existing).updateDisplayCurrency(USD);
      verify(portfolioRepository).save(existing);
    }

    @Test
    @DisplayName("updatePortfolio: throws exception when TransactionTemplate returns null")
    void throwsExceptionOnNullTransactionResult() {
      when(transactionTemplate.execute(any())).thenReturn(null);
      UpdatePortfolioCommand command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "New Name",
          "DESC", USD);

      assertThatThrownBy(() -> service.updatePortfolio(command)).isInstanceOf(
          IllegalStateException.class).hasMessageContaining("Transaction failed");
    }

    @Test
    @DisplayName("updatePortfolio: handles market data degradation and sets isStale=true")
    void handlesMarketDataFailureGracefully() {
      // Setup
      Portfolio mockPortfolio = mock(Portfolio.class);
      AccountId accId = new AccountId(UUID.randomUUID());
      Account mockAccount = mock(Account.class);

      when(mockPortfolio.getAccounts()).thenReturn(List.of(mockAccount));
      when(mockPortfolio.getDisplayCurrency()).thenReturn(USD);
      when(mockAccount.getAccountId()).thenReturn(accId);
      when(transactionTemplate.execute(any())).thenReturn(
          new UpdatePortfolioResult(mockPortfolio, List.of(accId)));

      // Mock market data failure
      when(marketDataService.getBatchQuotes(anySet())).thenThrow(
          new RuntimeException("External Service Down"));

      UpdatePortfolioCommand command = new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "Update",
          "DESC2", USD);
      service.updatePortfolio(command);

      // Verify mapping was called with isStale = true
      verify(portfolioViewMapper).toPortfolioView(eq(mockPortfolio), anyList(), any(), eq(true));
    }

    @Test
    @DisplayName("updatePortfolio: propagates isStale if internal accounts are already stale")
    void propagatesStaleFlagFromAccounts() {
      Portfolio mockPortfolio = mock(Portfolio.class);
      Account staleAccount = mock(Account.class);
      AccountId accountId = new AccountId(UUID.randomUUID());

      when(staleAccount.isStale()).thenReturn(true);
      when(staleAccount.getAccountId()).thenReturn(accountId);
      when(mockPortfolio.getAccounts()).thenReturn(List.of(staleAccount));
      when(mockPortfolio.getDisplayCurrency()).thenReturn(USD);
      when(transactionTemplate.execute(any())).thenReturn(
          new UpdatePortfolioResult(mockPortfolio, List.of(accountId)));

      when(marketDataService.getBatchQuotes(any())).thenReturn(Map.of());
      when(portfolioValuationService.calculateTotalValue(any(), any(), any())).thenReturn(
          Money.zero("USD"));

      service.updatePortfolio(
          new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "Update", "DESC2", USD));

      verify(portfolioViewMapper).toPortfolioView(any(), any(), any(), eq(true));
    }

    @Test
    @DisplayName("updatePortfolio: continues even if fee enrichment fails")
    void continuesOnFeeFetchError() {
      Portfolio mockPortfolio = mock(Portfolio.class);
      AccountId accId = new AccountId(UUID.randomUUID());

      when(transactionTemplate.execute(any())).thenReturn(
          new UpdatePortfolioResult(mockPortfolio, List.of(accId)));
      when(transactionRepository.sumBuyFeesByAccountAndSymbol(any())).thenThrow(
          new RuntimeException("DB Error"));

      service.updatePortfolio(
          new UpdatePortfolioCommand(PORTFOLIO_ID, USER_ID, "Update", "DESC2", USD));

      // Verify process reached the end despite fee error
      verify(portfolioViewMapper).toPortfolioView(any(), any(), any(), anyBoolean());
    }
  }

  @Nested
  @DisplayName("deletePortfolio Branching logic")
  class DeletePortfolioTests {
    @Test
    @DisplayName("soft delete: rejects recursive delete if accounts are non-empty")
    void recursiveDeleteFailsWithNonEmptyAccounts() {
      Portfolio portfolio = mock(Portfolio.class);
      Account activeAccount = mock(Account.class);

      when(portfolioRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(portfolio));
      when(portfolio.getAccounts()).thenReturn(List.of(activeAccount));
      when(activeAccount.isActive()).thenReturn(true);
      when(activeAccount.getPositionCount()).thenReturn(5); // Non-empty

      DeletePortfolioCommand command = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true,
          true);

      assertThatThrownBy(() -> service.deletePortfolio(command)).isInstanceOf(
              PortfolioDeletionException.class)
          .hasMessageContaining("zero positions and zero cash balance");
    }

    @Test
    @DisplayName("soft delete: successful recursive close of empty accounts")
    void recursiveDeleteClosesEmptyAccounts() {
      Portfolio portfolio = mock(Portfolio.class);
      Account activeAccount = mock(Account.class);
      AccountId accId = new AccountId(UUID.randomUUID());

      when(portfolioRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(portfolio));
      when(portfolio.getAccounts()).thenReturn(List.of(activeAccount));
      when(activeAccount.isActive()).thenReturn(true);
      when(activeAccount.getPositionCount()).thenReturn(0);
      when(activeAccount.getCashBalance()).thenReturn(Money.zero("USD"));
      when(activeAccount.getAccountId()).thenReturn(accId);

      service.deletePortfolio(new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true, true));

      verify(portfolio).closeAccount(accId);
      verify(portfolio).markAsDeleted(USER_ID);
      verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("hard delete: calls delete directly on repository")
    void hardDeleteCallsRepository() {
      UserId id = UserId.random();
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolioRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(portfolio));

      service.deletePortfolio(new DeletePortfolioCommand(PORTFOLIO_ID, id, true, false, false));

      verify(portfolioRepository).delete(PORTFOLIO_ID);
      verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("deletePortfolio: throws PortfolioNotFoundException when missing")
    void throwsWhenNotFound() {
      when(portfolioRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true,
          false);
      assertThatThrownBy(() -> service.deletePortfolio(cmd)).isInstanceOf(
          PortfolioNotFoundException.class);
    }

    @Test
    @DisplayName("soft delete: maps PortfolioNotEmptyException to PortfolioDeletionException")
    void mapsNotEmptyException() {
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolioRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.of(portfolio));
      // Simulate domain throwing PortfolioNotEmptyException
      doThrow(new PortfolioNotEmptyException("Close accounts first")).when(portfolio)
          .markAsDeleted(USER_ID);

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true,
          false);
      assertThatThrownBy(() -> service.deletePortfolio(cmd)).isInstanceOf(
          PortfolioDeletionException.class).hasMessageContaining("close accounts first");
    }

    @Test
    @DisplayName("recursive delete: throws when an account has a positive cash balance")
    void recursiveDeleteFailsWithPositiveCashBalance() {
      Portfolio portfolio = mock(Portfolio.class);
      Account accountWithCash = mock(Account.class);

      when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(
          Optional.of(portfolio));
      when(portfolio.getAccounts()).thenReturn(List.of(accountWithCash));

      // Mocking: Active account with 0 positions but positive cash
      when(accountWithCash.isActive()).thenReturn(true);
      when(accountWithCash.getPositionCount()).thenReturn(0);
      when(accountWithCash.getCashBalance()).thenReturn(Money.of("100.00", USD));

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true,
          true);

      assertThatThrownBy(() -> service.deletePortfolio(cmd)).isInstanceOf(
              PortfolioDeletionException.class)
          .hasMessageContaining("zero positions and zero cash balance");
    }

    @Test
    @DisplayName("recursive delete: throws when an account has active positions")
    void recursiveDeleteFailsWithActivePositions() {
      Portfolio portfolio = mock(Portfolio.class);
      Account accountWithPositions = mock(Account.class);

      when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(
          Optional.of(portfolio));
      when(portfolio.getAccounts()).thenReturn(List.of(accountWithPositions));

      // Mocking: Active account with positions
      when(accountWithPositions.isActive()).thenReturn(true);
      when(accountWithPositions.getPositionCount()).thenReturn(1);

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true,
          true);

      assertThatThrownBy(() -> service.deletePortfolio(cmd)).isInstanceOf(
          PortfolioDeletionException.class);
    }

    @Test
    @DisplayName("soft delete: maps PortfolioAlreadyDeletedException to PortfolioDeletionException")
    void mapsAlreadyDeletedException() {
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(
          Optional.of(portfolio));

      // Trigger the specific catch block
      doThrow(new PortfolioAlreadyDeletedException("Already gone")).when(portfolio)
          .markAsDeleted(USER_ID);

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true,
          false);

      assertThatThrownBy(() -> service.deletePortfolio(cmd)).isInstanceOf(
          PortfolioDeletionException.class).hasMessage("Already gone");
    }

    @Test
    @DisplayName("soft delete: maps IllegalStateException to PortfolioDeletionException")
    void mapsIllegalStateException() {
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolioRepository.findByIdAndUserId(PORTFOLIO_ID, USER_ID)).thenReturn(
          Optional.of(portfolio));

      // Trigger the specific catch block for IllegalStateException
      doThrow(new IllegalStateException("Inconsistent state")).when(portfolio)
          .markAsDeleted(USER_ID);

      DeletePortfolioCommand cmd = new DeletePortfolioCommand(PORTFOLIO_ID, USER_ID, true, true,
          false);

      assertThatThrownBy(() -> service.deletePortfolio(cmd)).isInstanceOf(
          PortfolioDeletionException.class).hasMessage("Inconsistent state");
    }
  }

  @Nested
  @DisplayName("Account Operations (Simple Mapping)")
  class AccountOperationTests {
    @Test
    @DisplayName("reopenAccount: success path")
    void reopenAccountSuccess() {
      Portfolio portfolio = mock(Portfolio.class);
      AccountId accId = AccountId.newId();
      when(portfolioLoader.loadUserPortfolio(PORTFOLIO_ID, USER_ID)).thenReturn(portfolio);

      service.reopenAccount(new ReopenAccountCommand(accId, PORTFOLIO_ID, USER_ID));

      verify(portfolio).reopenAccount(accId);
      verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("createAccount: success path")
    void createAccountSuccess() {
      Portfolio portfolio = mock(Portfolio.class);
      Account newAccount = mock(Account.class);
      when(portfolioLoader.loadUserPortfolio(PORTFOLIO_ID, USER_ID)).thenReturn(portfolio);
      when(portfolio.createAccount(any(), any(), any(), any())).thenReturn(newAccount);

      service.createAccount(
          new CreateAccountCommand(PORTFOLIO_ID, USER_ID, "Taxable", AccountType.CHEQUING,
              PositionStrategy.ACB, USD));

      verify(portfolioRepository).save(portfolio);
      verify(portfolioViewMapper).toNewAccountView(newAccount);
    }

    @Test
    @DisplayName("updateAccount: success path")
    void updateAccountSuccess() {
      Portfolio portfolio = mock(Portfolio.class);
      AccountId accId = AccountId.newId();
      when(portfolioLoader.loadUserPortfolio(PORTFOLIO_ID, USER_ID)).thenReturn(portfolio);

      service.updateAccount(new UpdateAccountCommand(PORTFOLIO_ID, USER_ID, accId, "New Name"));

      verify(portfolio).renameAccount(accId, "New Name");
      verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("reopenAccount: maps IllegalStateException to AccountCannotBeReopenedException")
    void reopenAccountErrorMapping() {
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);
      doThrow(new IllegalStateException("Closed forever")).when(portfolio).reopenAccount(any());

      ReopenAccountCommand command = new ReopenAccountCommand(AccountId.newId(), PORTFOLIO_ID,
          USER_ID);

      assertThatThrownBy(() -> service.reopenAccount(command)).isInstanceOf(
          AccountCannotBeReopenedException.class);
    }

    @Test
    @DisplayName("deleteAccount: happy path saves portfolio after closing account")
    void deleteAccountSuccess() {
      Portfolio portfolio = mock(Portfolio.class);
      when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);

      service.deleteAccount(new DeleteAccountCommand(PORTFOLIO_ID, USER_ID, AccountId.newId()));

      verify(portfolio).closeAccount(any());
      verify(portfolioRepository).save(portfolio);
    }

    @Test
    @DisplayName("deleteAccount: throws IllegalStateException when isn't empty")
    void deleteAccountThrowsException() {
      Portfolio portfolio = mock(Portfolio.class);
      AccountId accountId = AccountId.newId();

      when(portfolioLoader.loadUserPortfolio(any(), any())).thenReturn(portfolio);
      doThrow(IllegalStateException.class).when(portfolio).closeAccount(eq(accountId));

      DeleteAccountCommand command = new DeleteAccountCommand(PORTFOLIO_ID, USER_ID, accountId);
      assertThatThrownBy(() -> service.deleteAccount(command)).isInstanceOf(
          AccountCannotBeClosedException.class);
    }
  }
}