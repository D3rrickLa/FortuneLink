package com.laderrco.fortunelink.portfolio.domain.model.entities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioAlreadyDeletedException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.PositionStrategy;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

class PortfolioTest {
  private PortfolioId portfolioId;
  private UserId userId;
  private String portfolioName;

  @BeforeEach
  void setUp() {
    portfolioId = PortfolioId.newId();
    userId = UserId.random();
    portfolioName = "Main Portfolio";
  }

  @Nested
  @DisplayName("Constructor and Initialization Tests")
  class InitializationTests {

    @Test
    void constructor_Success_InitializesCorrectly_Default() {
      Portfolio portfolio = new Portfolio();
      assertAll(
          () -> assertNull(portfolio.getPortfolioId()),
          () -> assertNull(portfolio.getUserId()),
          () -> assertNull(portfolio.getCreatedAt()));
    }

    @Test
    @DisplayName("Create New static constructor")
    void constructor_Success_createNew_Factorymethod() {
      Portfolio portfolio = Portfolio.createNew(userId, portfolioName, "description", Currency.CAD);
      assertAll(
          () -> assertEquals(userId, portfolio.getUserId()),
          () -> assertEquals(portfolioName, portfolio.getName()),
          () -> assertEquals("description", portfolio.getDescription()),
          () -> assertEquals(Currency.CAD, portfolio.getDisplayCurrency()));
    }

    @Test
    @DisplayName("Create New static constructor blank description")
    void constructor_Success_createNew_NoDescriptionShouldBeBlank() {
      Portfolio portfolio = Portfolio.createNew(userId, portfolioName, null, Currency.CAD);
      assertAll(
          () -> assertEquals(userId, portfolio.getUserId()),
          () -> assertEquals(portfolioName, portfolio.getName()),
          () -> assertTrue(portfolio.getDescription().isEmpty()),
          () -> assertEquals(Currency.CAD, portfolio.getDisplayCurrency()));
    }

    @Test
    @DisplayName("Constructor_Success_ValidParameters")
    void constructor_Success_InitializesCorrectly() {
      Portfolio portfolio = Portfolio.createNew(userId, portfolioName, "desc", Currency.CAD);

      assertThat(portfolio.getUserId()).isEqualTo(userId);
      assertThat(portfolio.getName()).isEqualTo(portfolioName);
      assertThat(portfolio.getAccounts()).isEmpty();
      assertThat(portfolio.getDisplayCurrency()).isEqualTo(Currency.CAD);
      assertThat(portfolio.isDeleted()).isFalse();
      assertThat(portfolio.getCreatedAt()).isBeforeOrEqualTo(Instant.now());
      assertThat(portfolio.getDescription()).isEqualTo("desc");
    }

    @Test
    @DisplayName("Constructor_Failure_NullOrEmptyParameters")
    void constructor_Failure_ThrowsExceptionOnInvalidInputs() {
      assertThatThrownBy(() -> Portfolio.createNew(null, portfolioName, "desc", Currency.CAD))
          .isInstanceOf(DomainArgumentException.class);

      assertThatThrownBy(() -> Portfolio.createNew(userId, null, "desc", Currency.CAD))
          .isInstanceOf(DomainArgumentException.class);

      assertThatThrownBy(() -> Portfolio.createNew(userId, portfolioName, " ", null))
          .isInstanceOf(DomainArgumentException.class);
    }

    @Test
    @DisplayName("Reconstitute_Success_MapsAllFields")
    void reconstitute_Success_RestoresState() {
      Instant fixedTime = Instant.now();
      Map<AccountId, Account> accounts = new HashMap<>();

      Portfolio portfolio = Portfolio.reconstitute(
          portfolioId, userId, "Old Name", "Desc", accounts, Currency.CAD,
          true, fixedTime, userId, fixedTime, fixedTime);

      assertThat(portfolio.isDeleted()).isTrue();
      assertThat(portfolio.getDeletedOn()).isEqualTo(fixedTime);
      assertThat(portfolio.getName()).isEqualTo("Old Name");
    }
  }

  @Nested
  @DisplayName("Account Management Tests")
  class AccountManagementTests {

    private Portfolio portfolio;

    @BeforeEach
    void initPortfolio() {
      portfolio = Portfolio.createNew(userId, portfolioName, "description", Currency.CAD);
      portfolioId = this.portfolio.getPortfolioId();
    }

    @Test
    @DisplayName("createAccount_Success_AddsToMapAndTouchesTimestamp")
    void createAccount_Success_AddsAccount() {
      Instant beforeUpdate = portfolio.getLastUpdatedAt();

      Account account = portfolio.createAccount(
          "Trading", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);

      assertThat(portfolio.getAccountCount()).isEqualTo(1);
      assertThat(portfolio.getAccounts()).contains(account);
      assertThat(portfolio.getLastUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
      assertThat(portfolio.hasAccounts()).isTrue();
    }

    @Test
    @DisplayName("createAccount_Failure_DuplicateName")
    void createAccount_Failure_DuplicateNameThrows() {
      portfolio.createAccount(
          "Savings", AccountType.TAXABLE_INVESTMENT, Currency.USD, PositionStrategy.ACB);

      assertThatThrownBy(
          () -> portfolio.createAccount(
              "SAVINGS", AccountType.TAXABLE_INVESTMENT, Currency.USD,
              PositionStrategy.ACB))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("createAccount_Failure_NotACB")
    void createAccount_Failure_NotACB() {
      assertThatThrownBy(
          () -> portfolio.createAccount(
              "SAVINGS", AccountType.TAXABLE_INVESTMENT, Currency.USD, PositionStrategy.FIFO))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Only ACB strategy is supported.");
    }

    @Test
    @DisplayName("createAccount_Failure_NameEmpty")
    void createAccount_Failure_AccountNameEmpty() {
      assertThatThrownBy(
          () -> portfolio.createAccount(
              " ", AccountType.TAXABLE_INVESTMENT, Currency.USD,
              PositionStrategy.ACB))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Account name cannot be empty");
    }

    @Test
    @DisplayName("removeAccount_Success_WhenClosedAndEmpty")
    void removeAccount_Success_RemovesInactiveAccount() {
      Account account = portfolio.createAccount(
          "Test", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);
      AccountId id = account.getAccountId();

      // Mocking internal account state for removal conditions
      // Assuming Account has methods: close(), isActive(), getCashBalance(),
      // getPositionCount()
      account.close();

      portfolio.removeAccount(id);
      assertThat(portfolio.hasAccounts()).isFalse();
    }

    @Test
    @DisplayName("removeAccount_Failure_WhenActiveOrHasBalance")
    void removeAccount_Failure_ThrowsIfAccountNotReady() {
      Account account = portfolio.createAccount(
          "Active", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);

      assertThatThrownBy(() -> portfolio.removeAccount(account.getAccountId()))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Close it first");
    }

    @Test
    @DisplayName("reopenAccount_Success_WhenClosedAndReOpen")
    void reopenAccount_Success() {
      Account account = portfolio.createAccount(
          "Test", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);
      AccountId id = account.getAccountId();

      // Mocking internal account state for removal conditions
      // Assuming Account has methods: close(), isActive(), getCashBalance(),
      // getPositionCount()
      // account.close();

      portfolio.closeAccount(id);
      assertThat(portfolio.findAccount(id).get().isActive()).isFalse();

      portfolio.reopenAccount(id);
      assertThat(portfolio.findAccount(id).get().isActive()).isTrue();
    }

    @Test
    @DisplayName("findAccountByName")
    void findAccountByName_Success_FindsAccount() {
      Instant beforeUpdate = portfolio.getLastUpdatedAt();

      Account account = portfolio.createAccount(
          "Trading", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);

      assertThat(portfolio.getAccountCount()).isEqualTo(1);
      assertThat(portfolio.getAccounts()).contains(account);
      assertThat(portfolio.getLastUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
      assertThat(portfolio.hasAccounts()).isTrue();

      Account foundAccount = portfolio.findAccountByName("Trading").get();
      assertEquals(account, foundAccount);
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = { "  ", "\t", "\n" }) // Testing null, empty, and blank
    @DisplayName("findAccountByName_ReturnEmpty_WhenNameIsInvalid")
    void findAccountByName_Failure_ReturnsEmptyWhenNameIsNullOrBlank(String invalidName) {
      portfolio.createAccount(
          "Trading", AccountType.TAXABLE_INVESTMENT, Currency.USD, PositionStrategy.ACB);

      Optional<Account> foundAccount = portfolio.findAccountByName(invalidName);

      assertThat(foundAccount).isEmpty();
    }

    @Test
    @DisplayName("findAccountByType_Success")
    void findAccountByType_Success() {
      Account account = portfolio.createAccount(
          "Test", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);

      List<Account> foundAccount = portfolio.findAccountsByType(AccountType.TAXABLE_INVESTMENT);
      assertTrue(foundAccount.size() == 1);
      assertEquals(account, foundAccount.get(0));
    }

    @Test
    @DisplayName("renameAccount_Success")
    void renameAccount_Success() {
      Account account = portfolio.createAccount(
          "Test", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);
      AccountId id = account.getAccountId();

      portfolio.renameAccount(id, "AMONGUS");

      assertEquals("AMONGUS", account.getName());
    }

    @Test
    @DisplayName("renameAccount_Success_SameNameIsNoOp")
    void renameAccount_Success_SameName() {
      Account account = portfolio.createAccount(
          "Test", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);

      // This should NOT throw an exception
      portfolio.renameAccount(account.getAccountId(), "Test");

      assertThat(account.getName()).isEqualTo("Test");
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = { "  ", "\t", "\n" })
    // Testing null, empty, and blank
    void renameAccount_Failure_NameIsNullAndOrEmpty(String invalidName) {
      Account account = portfolio.createAccount(
          "Test", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);
      AccountId id = account.getAccountId();

      assertThatThrownBy(() -> portfolio.renameAccount(id, invalidName))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Account name cannot be empty");

    }

    @Test
    @DisplayName("renameAccount_Failure_NameExistsAlready")
    void renameAccount_Failure_NameExistsAlready() {
      Account account = portfolio.createAccount(
          "Test", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);
      portfolio.createAccount(
          "Test_2", AccountType.TAXABLE_INVESTMENT, Currency.USD,
          PositionStrategy.ACB);
      AccountId id = account.getAccountId();

      assertThatThrownBy(() -> portfolio.renameAccount(id, "Test_2"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Account name already exists");

    }

  }

  @Nested
  @DisplayName("Portfolio Lifecycle Tests (Delete/Restore)")
  class LifecycleTests {

    private Portfolio portfolio;

    @BeforeEach
    void init() {
      portfolio = Portfolio.createNew(userId, portfolioName, "description", Currency.USD);
      portfolioId = this.portfolio.getPortfolioId();
    }

    @Test
    @DisplayName("markAsDeleted_Success_SetsDeletedFlag")
    void markAsDeleted_Success_UpdatesState() {
      portfolio.markAsDeleted(userId);

      assertThat(portfolio.isDeleted()).isTrue();
      assertThat(portfolio.getDeletedBy()).isEqualTo(userId);
      assertThat(portfolio.getDeletedOn()).isNotNull();
    }

    @Test
    @DisplayName("markAsDeleted_Failure_HasActiveAccounts")
    void markAsDeleted_Failure_ThrowsIfNotEmpty() {
      portfolio.createAccount(
          "Trading", AccountType.TAXABLE_INVESTMENT, Currency.USD, PositionStrategy.ACB);

      assertThatThrownBy(() -> portfolio.markAsDeleted(userId))
          .isInstanceOf(PortfolioNotEmptyException.class)
          .hasMessageContaining("Cannot delete portfolio with");
    }

    @Test
    @DisplayName("markAsDeleted_Failure_AlreadyDeleted")
    void markAsDeleted_Failure_ThrowsWhenAlreadyDeleted() {
      portfolio.markAsDeleted(userId);
      assertThatThrownBy(() -> portfolio.markAsDeleted(userId))
          .isInstanceOf(PortfolioAlreadyDeletedException.class)
          .hasMessageContaining("Portfolio is already deleted");
    }

    @Test
    @DisplayName("restore_Success_ClearsDeletedMetadata")
    void restore_Success_ResetsFields() {
      portfolio.markAsDeleted(userId);
      portfolio.restore();

      assertThat(portfolio.isDeleted()).isFalse();
      assertThat(portfolio.getDeletedBy()).isNull();
      assertThat(portfolio.getDeletedOn()).isNull();
    }

    @Test
    @DisplayName("restore_Failure_IfNotDeleted")
    void restore_Failure_ThrowsIfNotInDeletedState() {
      assertThatThrownBy(() -> portfolio.restore())
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("updateDetails_Success_UpdatesNameAndDescription")
    void updateDetails_Success_UpdatesFieldsAndTouchesTimestamp() {
      // 1. Capture the time IMMEDIATELY before the action
      Instant beforeUpdate = Instant.now();

      String newName = "Wealth Growth 2024";
      String newDesc = "Main retirement fund";

      // 2. Perform the action
      portfolio.updateDetails(newName, newDesc);

      // 3. Assertions
      assertThat(portfolio.getName()).isEqualTo(newName);
      assertThat(portfolio.getDescription()).isEqualTo(newDesc);

      // Use isAfterOrEqualTo to handle ultra-fast execution
      assertThat(portfolio.getLastUpdatedAt())
          .as("The lastUpdatedOn timestamp should be refreshed")
          .isAfterOrEqualTo(beforeUpdate);
    }

    @Test
    @DisplayName("updateDetails_Success_HandlesNullDescription")
    void updateDetails_Success_NullDescriptionSetsEmptyString() {
      // Description should be forced to "" if null is passed
      portfolio.updateDetails("New Name", null);

      assertThat(portfolio.getDescription()).isEqualTo("");
      assertThat(portfolio.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("updateDetails_Success_TrimsInputStrings")
    void updateDetails_Success_TrimsNameAndDescription() {
      portfolio.updateDetails("  Trimmed Name  ", "  Trimmed Desc  ");

      assertThat(portfolio.getName()).isEqualTo("Trimmed Name");
      assertThat(portfolio.getDescription()).isEqualTo("Trimmed Desc");
    }

    @Test
    @DisplayName("updateDetails_Failure_EmptyOrNullName")
    void updateDetails_Failure_ThrowsExceptionForInvalidNames() {
      // Test Null
      assertThatThrownBy(() -> portfolio.updateDetails(null, "Valid Desc"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Portfolio name cannot be empty");

      // Test Empty
      assertThatThrownBy(() -> portfolio.updateDetails("", "Valid Desc"))
          .isInstanceOf(IllegalArgumentException.class);

      // Test Blank/Whitespace
      assertThatThrownBy(() -> portfolio.updateDetails("   ", "Valid Desc"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateDisplayCurrency_Success_Changes_Currency")
    void updateDisplayCurrency_Success_NoErrors() {
      Currency oldCurrency = portfolio.getDisplayCurrency();
      portfolio.updateDisplayCurrency(Currency.EUR);

      assertEquals(Currency.EUR, portfolio.getDisplayCurrency());
      assertNotEquals(oldCurrency, portfolio.getDisplayCurrency());
    }

    @Test
    void reportRecalculationFailure_Success_MarkAsStale() {
      Account account_1 = portfolio.createAccount(
          "Invest", AccountType.TFSA, Currency.USD, PositionStrategy.ACB);
      portfolio.reportRecalculationFailure(account_1.getAccountId());
      assertThat(account_1.isStale()).isTrue();
    }

    @Test
    void reportRecalculationFailure_failure_accountNotFound() {
      Account account_1 = new Account(
          AccountId.newId(), "Not Found", AccountType.CHEQUING, Currency.USD, PositionStrategy.ACB);
      String id = account_1.getAccountId().id().toString();

      assertThatThrownBy(() -> portfolio.reportRecalculationFailure(account_1.getAccountId()))
          .isInstanceOf(AccountNotFoundException.class)
          .hasMessageContaining(
              "Portfolio, " + portfolio.getPortfolioId().id().toString() + " does not have/cannot find " + id);
    }

    @Test
    void reportRecalculationSuccess_Success_MarkAsHealth() {
      Account account_1 = portfolio.createAccount(
          "Invest", AccountType.TFSA, Currency.USD, PositionStrategy.ACB);
      portfolio.reportRecalculationFailure(account_1.getAccountId());
      assertThat(account_1.isStale()).isTrue();

      portfolio.reportRecalculationSuccess(account_1.getAccountId());
      assertThat(account_1.isStale()).isFalse();
    }

    @Test
    void reportRecalculationSuccess_failure_accountNotFound() {
      Account account_1 = new Account(
          AccountId.newId(), "Not Found", AccountType.CHEQUING, Currency.USD, PositionStrategy.ACB);

      String id = account_1.getAccountId().id().toString();

      assertThatThrownBy(() -> portfolio.reportRecalculationSuccess(account_1.getAccountId()))
          .isInstanceOf(AccountNotFoundException.class)
          .hasMessageContaining(
              "Portfolio, " + portfolio.getPortfolioId().id().toString() + " does not have/cannot find " + id);
    }

    @Test
    void reportRecalculationSuccess_Success_DoesNothingWhenHealthyStatus() {
      Account account_1 = portfolio.createAccount(
          "Invest", AccountType.TFSA, Currency.USD, PositionStrategy.ACB);

      portfolio.reportRecalculationSuccess(account_1.getAccountId());
      assertThat(account_1.isStale()).isFalse();
    }
  }

  @Nested
  @DisplayName("Query and Search Tests")
  class QueryTests {

    private Portfolio portfolio;

    @BeforeEach
    void init() {
      portfolio = Portfolio.createNew(userId, portfolioName, "description", Currency.USD);
      portfolioId = this.portfolio.getPortfolioId();
      portfolio.createAccount("Checking", AccountType.TFSA, Currency.USD, PositionStrategy.ACB);
    }

    @Test
    @DisplayName("findAccountByName_Success_CaseInsensitiveSearch")
    void findAccountByName_Success_FindsAccount() {
      Optional<Account> found = portfolio.findAccountByName("CHECKING");
      assertThat(found).isPresent();
      assertThat(found.get().getName()).isEqualTo("Checking");
    }

    @Test
    @DisplayName("getAccount_Failure_ThrowsOnMissingId")
    void getAccount_Failure_ThrowsNotFound() {
      AccountId randomId = AccountId.newId();
      assertThatThrownBy(() -> portfolio.getAccount(randomId))
          .isInstanceOf(AccountNotFoundException.class);
    }

    @Test
    @DisplayName("belongsToUser_Success_ValidatesOwnership")
    void belongsToUser_Success_ReturnsTrue() {
      assertThat(portfolio.belongsToUser(userId)).isTrue();
      assertThat(portfolio.belongsToUser(UserId.random())).isFalse();
    }
  }
}