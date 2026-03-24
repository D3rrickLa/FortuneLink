package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Domain Entity Tests For Portfolio")
class PortfolioTest {
  private static final String DEFAULT_NAME = "Main Portfolio";
  private static final String DEFAULT_DESC = "description";
  private static final Currency DEFAULT_CURRENCY = Currency.CAD;

  private UserId userId;

  @BeforeEach
  void setUp() {
    userId = UserId.random();
  }

  @Nested
  @DisplayName("Initialization")
  class InitializationTests {

    @Test
    @DisplayName("constructor: creates empty proxy instance")
    void constructorCreatesEmptyInstance() {
      Portfolio portfolio = new Portfolio();

      assertAll(
          () -> assertNull(portfolio.getPortfolioId()),
          () -> assertNull(portfolio.getUserId()),
          () -> assertNull(portfolio.getCreatedAt()));
    }

    @Test
    @DisplayName("createNew: initializes portfolio with valid values")
    void createNewSucceedsWithValidInput() {
      Portfolio portfolio = createDefaultPortfolio();

      assertAll(
          () -> assertEquals(userId, portfolio.getUserId()),
          () -> assertEquals(DEFAULT_NAME, portfolio.getName()),
          () -> assertEquals(DEFAULT_DESC, portfolio.getDescription()),
          () -> assertEquals(DEFAULT_CURRENCY, portfolio.getDisplayCurrency()),
          () -> assertFalse(portfolio.isDeleted()),
          () -> assertTrue(portfolio.getAccounts().isEmpty()));
    }

    @Test
    @DisplayName("createNew: sets empty description when null")
    void createNewSetsEmptyDescriptionWhenNull() {
      Portfolio portfolio = Portfolio.createNew(userId, DEFAULT_NAME, null, DEFAULT_CURRENCY);

      assertEquals("", portfolio.getDescription());
    }

    @Test
    @DisplayName("createNew: rejects invalid inputs")
    void createNewFailsWithInvalidInputs() {
      assertThatThrownBy(() -> Portfolio.createNew(null, DEFAULT_NAME, "desc", DEFAULT_CURRENCY))
          .isInstanceOf(DomainArgumentException.class);

      assertThatThrownBy(() -> Portfolio.createNew(userId, null, "desc", DEFAULT_CURRENCY))
          .isInstanceOf(DomainArgumentException.class);

      assertThatThrownBy(() -> Portfolio.createNew(userId, DEFAULT_NAME, " ", null))
          .isInstanceOf(DomainArgumentException.class);
    }
  }

  // =========================
  // Account Management
  // =========================
  @Nested
  @DisplayName("Account Management")
  class AccountManagementTests {
    private Portfolio portfolio;

    @BeforeEach
    void init() {
      portfolio = createDefaultPortfolio();
    }

    @Test
    @DisplayName("createAccount: adds account and updates state")
    void createAccountSucceeds() {
      Instant before = portfolio.getLastUpdatedAt();

      Account account = createAccount(portfolio, "Trading");

      assertAll(
          () -> assertEquals(1, portfolio.getAccountCount()),
          () -> assertTrue(portfolio.getAccounts().contains(account)),
          () -> assertTrue(isAfterOrEqualTo(portfolio.getLastUpdatedAt(), before)));
    }

    private boolean isAfterOrEqualTo(Instant lastUpdatedAt, Instant before) {
      return lastUpdatedAt.isAfter(before) || lastUpdatedAt.equals(before);
    }

    @Test
    @DisplayName("createAccount: rejects duplicate names")
    void createAccountFailsWhenDuplicateNameExists() {
      createAccount(portfolio, "Savings");

      assertThatThrownBy(() -> createAccount(portfolio, "SAVINGS"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createAccount: rejects invalid strategy")
    void createAccountFailsWhenStrategyInvalid() {
      assertThatThrownBy(() -> portfolio.createAccount("Test", AccountType.TAXABLE_INVESTMENT,
          Currency.USD, PositionStrategy.FIFO))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("removeAccount: succeeds when account is closed")
    void removeAccountSucceedsWhenClosed() {
      Account account = createAccount(portfolio, "Test");
      account.close();

      portfolio.removeAccount(account.getAccountId());

      assertFalse(portfolio.hasAccounts());
    }

    @Test
    @DisplayName("removeAccount: fails when account is active")
    void removeAccountFailsWhenActive() {
      Account account = createAccount(portfolio, "Active");

      assertThatThrownBy(() -> portfolio.removeAccount(account.getAccountId()))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("renameAccount: updates name successfully")
    void renameAccountSucceeds() {
      Account account = createAccount(portfolio, "Test");

      portfolio.renameAccount(account.getAccountId(), "NewName");

      assertEquals("NewName", account.getName());
    }

    @Test
    @DisplayName("renameAccount: no-op when name unchanged")
    void renameAccountNoOpWhenSameName() {
      Account account = createAccount(portfolio, "Test");

      portfolio.renameAccount(account.getAccountId(), "Test");

      assertEquals("Test", account.getName());
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = { "  ", "\t" })
    @DisplayName("renameAccount: rejects invalid names")
    void renameAccountFailsWithInvalidName(String invalid) {
      Account account = createAccount(portfolio, "Test");

      assertThatThrownBy(() -> portfolio.renameAccount(account.getAccountId(), invalid))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  // =========================
  // Lifecycle
  // =========================
  @Nested
  @DisplayName("Lifecycle")
  class LifecycleTests {

    private Portfolio portfolio;

    @BeforeEach
    void init() {
      portfolio = createDefaultPortfolio();
    }

    @Test
    @DisplayName("delete: marks portfolio as deleted")
    void markAsDeletedSucceeds() {
      portfolio.markAsDeleted(userId);

      assertTrue(portfolio.isDeleted());
    }

    @Test
    @DisplayName("delete: fails when active accounts exist")
    void markAsDeletedFailsWhenNotEmpty() {
      createAccount(portfolio, "Trading");

      assertThatThrownBy(() -> portfolio.markAsDeleted(userId))
          .isInstanceOf(PortfolioNotEmptyException.class);
    }

    @Test
    @DisplayName("restore: succeeds when portfolio is deleted")
    void restoreSucceeds() {
      portfolio.markAsDeleted(userId);

      portfolio.restore();

      assertFalse(portfolio.isDeleted());
    }

    @Test
    @DisplayName("updateDetails: updates name and description")
    void updateDetailsSucceeds() {
      portfolio.updateDetails("New", "Desc");

      assertAll(
          () -> assertEquals("New", portfolio.getName()),
          () -> assertEquals("Desc", portfolio.getDescription()));
    }

    @Test
    @DisplayName("updateDetails: rejects invalid name")
    void updateDetailsFailsWithInvalidName() {
      assertThatThrownBy(() -> portfolio.updateDetails(null, "desc"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateDisplayCurrency: updates currency")
    void updateDisplayCurrencySucceeds() {
      portfolio.updateDisplayCurrency(Currency.EUR);

      assertEquals(Currency.EUR, portfolio.getDisplayCurrency());
    }
  }

  // =========================
  // Queries
  // =========================
  @Nested
  @DisplayName("Queries")
  class QueryTests {

    private Portfolio portfolio;

    @BeforeEach
    void init() {
      portfolio = createDefaultPortfolio();
      createAccount(portfolio, "Checking");
    }

    @Test
    @DisplayName("findAccountByName: finds account case-insensitively")
    void findAccountByNameSucceeds() {
      assertTrue(portfolio.findAccountByName("CHECKING").isPresent());
    }

    @Test
    @DisplayName("getAccount: throws when not found")
    void getAccountFailsWhenMissing() {
      assertThatThrownBy(() -> portfolio.getAccount(AccountId.newId()))
          .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("belongsToUser: validates ownership")
    void belongsToUserWorks() {
      assertTrue(portfolio.belongsToUser(userId));
      assertFalse(portfolio.belongsToUser(UserId.random()));
    }
  }

  private Portfolio createDefaultPortfolio() {
    return Portfolio.createNew(userId, DEFAULT_NAME, DEFAULT_DESC, DEFAULT_CURRENCY);
  }

  private Account createAccount(Portfolio portfolio, String name) {
    return portfolio.createAccount(name, AccountType.TAXABLE_INVESTMENT,
        Currency.USD, PositionStrategy.ACB);
  }
}