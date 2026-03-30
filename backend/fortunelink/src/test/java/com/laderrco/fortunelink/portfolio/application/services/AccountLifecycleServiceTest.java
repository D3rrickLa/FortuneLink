package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.commands.CreateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.DeleteAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.ReopenAccountCommand;
import com.laderrco.fortunelink.portfolio.application.commands.UpdateAccountCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeClosedException;
import com.laderrco.fortunelink.portfolio.application.exceptions.AccountCannotBeReopenedException;
import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.utils.PortfolioLoader;
import com.laderrco.fortunelink.portfolio.application.validators.AccountLifecycleCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLifecycleService Unit Tests")
public class AccountLifecycleServiceTest {
  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final Currency USD = Currency.USD;

  @Mock
  private PortfolioRepository portfolioRepository;

  @Mock
  private PortfolioViewMapper portfolioViewMapper;

  @Mock
  private AccountLifecycleCommandValidator validator;

  @Mock
  private PortfolioLoader portfolioLoader;

  @InjectMocks
  private AccountLifecycleService service;

  @BeforeEach
  void setUp() {
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
