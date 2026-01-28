package com.laderrco.fortunelink.portfolio_management.application.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CorrectAssetTickerCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.AccountNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidCommandException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioDeletionRequiresConfirmationException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioLimitReachedException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotEmptyException;
import com.laderrco.fortunelink.portfolio_management.application.models.TransactionSearchCriteria;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.assemblers.PortfolioViewAssembler;
import com.laderrco.fortunelink.portfolio_management.application.validators.CommandValidator;
import com.laderrco.fortunelink.portfolio_management.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.InsufficientFundsException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.CashIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.SymbolIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@DisplayName("Portfolio Application Service Tests")
class PortfolioApplicationServiceTest {

    public static final Instant TIME = Instant.now();

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    TransactionQueryService transactionQueryService;

    @Mock
    private PortfolioValuationService portfolioValuationService;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private CommandValidator commandValidator;

    @Mock
    private PortfolioViewAssembler portfolioViewAssembler;

    @InjectMocks
    private PortfolioApplicationService service;

    private UserId userId;
    private AccountId accountId;
    private Portfolio portfolio;
    private Account account;
    private MarketAssetInfo assetInfo;
    private MarketIdentifier identifier;
    private String name;

    @BeforeEach
    void setUp() {
        userId = UserId.randomId();
        accountId = AccountId.randomId();
        name = "Portfolio Name";

        // Create portfolio with account
        portfolio = new Portfolio(userId, name, ValidatedCurrency.USD);
        account = Account.createNew(accountId, "Test Account", AccountType.NON_REGISTERED, ValidatedCurrency.USD);
        portfolio.addAccount(account);

        // Add initial cash
        Transaction depositTx = new Transaction(
                TransactionId.randomId(),
                accountId,
                TransactionType.DEPOSIT,
                new CashIdentifier("USD"),
                BigDecimal.ONE,
                new Money(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                null,
                Instant.now(),
                "Initial deposit");
        portfolio.recordTransaction(accountId, depositTx);

        // Setup asset info
        assetInfo = new MarketAssetInfo(
                "AAPL",
                "Apple Inc.",
                AssetType.STOCK,
                "NASDAQ",
                ValidatedCurrency.USD,
                "Technology",
                "SOME DESC");

        identifier = new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null);
    }

    @Nested
    @DisplayName("Record Asset Purchase Tests")
    class RecordAssetPurchaseTests {

        private RecordPurchaseCommand command;

        @BeforeEach
        void setUp() {
            command = new RecordPurchaseCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    "AAPL",
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Test purchase");
        }

        @Test
        @DisplayName("Should successfully record asset purchase")
        void shouldRecordAssetPurchase() {
            // Given
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo(SymbolIdentifier.of("AAPL"))).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.recordAssetPurchase(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
            verify(marketDataService).getAssetInfo(SymbolIdentifier.of("AAPL"));

            // Verify portfolio state
            ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
            verify(portfolioRepository).save(captor.capture());
            Portfolio savedPortfolio = captor.getValue();

            Optional<Account> savedAccount = savedPortfolio.findAccount(accountId);
            assertThat(savedAccount.get().getAssets()).hasSize(1);
        }

        @Test
        @DisplayName("Should convert price when transaction currency differs from account currency")
        void shouldConvertPriceWhenCurrenciesDiffer() {
            // Given
            ValidatedCurrency accountCurrency = ValidatedCurrency.USD;
            ValidatedCurrency transactionCurrency = ValidatedCurrency.EUR;

            // Update mock account to be in EUR
            account = Account.createNew(accountId, "Test Account", AccountType.NON_REGISTERED, accountCurrency);

            RecordPurchaseCommand multiCurrencyCommand = new RecordPurchaseCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    "AAPL",
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(150), transactionCurrency), // USD
                    null,
                    Instant.now(),
                    "Cross-currency purchase");

            ExchangeRate mockRate = new ExchangeRate(transactionCurrency, accountCurrency, BigDecimal.valueOf(0.92),
                    Instant.now(), "source");

            when(commandValidator.validate(multiCurrencyCommand)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo(any())).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(any())).thenReturn(Optional.of(portfolio));
            when(exchangeRateService.getExchangeRate(transactionCurrency, accountCurrency))
                    .thenReturn(Optional.of(mockRate));
            when(portfolioRepository.save(any())).thenReturn(portfolio);

            // When
            service.recordAssetPurchase(multiCurrencyCommand);

            // Then
            verify(exchangeRateService).getExchangeRate(transactionCurrency, accountCurrency);

            // Use ArgumentCaptor to verify the internal domain state was updated with
            // converted price
            ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
            verify(portfolioRepository).save(captor.capture());

            // Here you would check if the asset in the portfolio reflects the conversion
            // Assuming 'convertedPrice' is used to calculate the cost basis in your domain
        }

        @Test
        @DisplayName("Should not call exchange service when currencies match")
        void shouldNotConvertWhenCurrenciesMatch() {
            // Given
            // Assuming command and account are both USD in setUp()
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo(any())).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(any())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any())).thenReturn(portfolio);

            // When
            service.recordAssetPurchase(command);

            // Then
            verifyNoInteractions(exchangeRateService);
        }

        @Test
        @DisplayName("Should throw exception when validation fails")
        void shouldThrowExceptionWhenValidationFails() {
            // Given
            when(commandValidator.validate(command))
                    .thenReturn(ValidationResult.failure(List.of("Invalid quantity")));

            // When/Then
            assertThatThrownBy(() -> service.recordAssetPurchase(command))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("Invalid purchase command");

            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when asset not found")
        void shouldThrowExceptionWhenAssetNotFound() {
            // Given
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo(SymbolIdentifier.of("AAPL"))).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.recordAssetPurchase(command))
                    .isInstanceOf(AssetNotFoundException.class)
                    .hasMessageContaining("Asset not found: AAPL");
        }

        @Test
        @DisplayName("Should throw exception when portfolio not found")
        void shouldThrowExceptionWhenPortfolioNotFound() {
            // Given
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo(SymbolIdentifier.of("AAPL"))).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.recordAssetPurchase(command))
                    .isInstanceOf(PortfolioNotFoundException.class);
        }

        @Test
        void recordAssetPurchase_WhenAccountNotFound_ThrowsException() {
            AccountId mockAccountId = AccountId.randomId();
            command = new RecordPurchaseCommand(
                    portfolio.getPortfolioId(),
                    mockAccountId,
                    "AAPL",
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Test purchase");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo(SymbolIdentifier.of("AAPL"))).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> service.recordAssetPurchase(command))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(mockAccountId.toString());
        }

        @Test
        @DisplayName("Should convert sale price when sale currency differs from account currency")
        void recordAssetSale_ShouldConvertCurrency() {
            // ---------- IDs ----------
            PortfolioId portfolioId = PortfolioId.randomId();
            AccountId accountId = AccountId.randomId();

            // ---------- Portfolio + Account ----------
            Portfolio portfolio = new Portfolio(
                    UserId.randomId(),
                    "Test Portfolio",
                    ValidatedCurrency.USD);

            Account account = Account.createNew(
                    accountId,
                    "USD Account",
                    AccountType.INVESTMENT,
                    ValidatedCurrency.USD);

            portfolio.addAccount(account);

            // ---------- Seed CASH ----------
            Transaction deposit = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.DEPOSIT,
                    mock(),
                    BigDecimal.valueOf(10_000),
                    new Money(BigDecimal.ONE, ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Initial cash");

            portfolio.recordTransaction(accountId, deposit);

            // ---------- Seed ASSET via BUY (creates Asset internally) ----------
            AssetIdentifier aapl = SymbolIdentifier.of("AAPL");

            Transaction buy = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    aapl,
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(100), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Seed asset");

            portfolio.recordTransaction(accountId, buy);

            // ---------- Persist stub ----------
            when(portfolioRepository.findById(portfolioId))
                    .thenReturn(Optional.of(portfolio));

            when(commandValidator.validate(any(RecordSaleCommand.class)))
                    .thenReturn(ValidationResult.success());

            // ---------- Sale command (EUR price) ----------
            Money salePriceEur = new Money(
                    BigDecimal.valueOf(100),
                    ValidatedCurrency.EUR);

            RecordSaleCommand command = new RecordSaleCommand(
                    portfolioId,
                    accountId,
                    account.getAssets().get(0).getAssetId(), // real asset ID
                    BigDecimal.ONE,
                    salePriceEur,
                    null,
                    Instant.now(),
                    "Sell in EUR");

            // ---------- Exchange rate ----------
            ExchangeRate eurToUsd = new ExchangeRate(
                    ValidatedCurrency.EUR,
                    ValidatedCurrency.USD,
                    BigDecimal.valueOf(1.2), TIME, "");

            when(exchangeRateService.getExchangeRate(
                    ValidatedCurrency.EUR,
                    ValidatedCurrency.USD)).thenReturn(Optional.of(eurToUsd));

            // ---------- Market data (optional) ----------
            when(marketDataService.getAssetInfo(any()))
                    .thenReturn(Optional.empty());

            // ---------- ACT ----------
            TransactionView result = service.recordAssetSale(command);

            // ---------- ASSERT ----------
            assertEquals(
                    new BigDecimal("120.00"),
                    result.price().amount().setScale(2));
            assertEquals(
                    ValidatedCurrency.USD,
                    result.price().currency());

            // ---------- VERIFY ----------
            verify(exchangeRateService)
                    .getExchangeRate(ValidatedCurrency.EUR, ValidatedCurrency.USD);
        }

        @Test
        @DisplayName("Should throw exception when insufficient funds")
        void shouldThrowExceptionWhenInsufficientFunds() {
            // Given
            RecordPurchaseCommand largeCommand = new RecordPurchaseCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    "AAPL",
                    BigDecimal.valueOf(1000), // Too many shares
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Large purchase");

            when(commandValidator.validate(largeCommand)).thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo(SymbolIdentifier.of("AAPL"))).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.recordAssetPurchase(largeCommand))
                    .isInstanceOf(InsufficientFundsException.class);
        }

    }

    @Nested
    @DisplayName("Record Asset Sale Tests")
    class RecordAssetSaleTests {

        private RecordSaleCommand command;
        private AssetId assetId;

        @BeforeEach
        void setUp() {
            // Create a fresh portfolio for this test to ensure clean state
            portfolio = new Portfolio(userId, name, ValidatedCurrency.USD);
            account = Account.createNew(accountId, "Test Account", AccountType.NON_REGISTERED, ValidatedCurrency.USD);
            portfolio.addAccount(account);

            // Add initial cash
            Transaction depositTx = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.DEPOSIT,
                    new CashIdentifier("USD"),
                    BigDecimal.ONE,
                    new Money(BigDecimal.valueOf(10000), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200),
                    "Initial deposit");
            portfolio.recordTransaction(accountId, depositTx);

            // Buy assets that we'll later sell - using the ACTUAL domain logic
            Transaction buyTx = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(), // Use the same identifier that will be used in the sale
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Buy for test");
            portfolio.recordTransaction(accountId, buyTx);

            this.assetId = portfolio.findAccount(accountId)
                    .get()
                    .getAssets()
                    .stream()
                    .findFirst() // Since it's the only asset
                    .get()
                    .getAssetId();

            command = new RecordSaleCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    assetId,
                    BigDecimal.valueOf(5),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Test sale");
        }

        @Test
        @DisplayName("Should successfully record asset sale and update quantity")
        void shouldRecordAssetSale() {
            // Given
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            // when(marketDataService.getAssetInfo(SymbolIdentifier.of("AAPL"))).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // Mock save to return the modified portfolio
            when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            TransactionView response = service.recordAssetSale(command);

            // Then
            assertThat(response).isNotNull();

            // Capture the portfolio that was sent to the database
            ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
            verify(portfolioRepository).save(captor.capture());

            Portfolio savedPortfolio = captor.getValue();

            // Verify account exists
            Optional<Account> savedAccount = savedPortfolio.findAccount(accountId);
            assertThat(savedAccount).isPresent();

            // Verify the asset exists and quantity is updated
            // Use .getAsset() which you defined in your Account class
            Asset soldAsset = savedAccount.get().getAsset(assetId);

            assertThat(soldAsset).isNotNull();
            // Use isEqualByComparingTo for BigDecimals to ignore scale (5.0 vs 5)
            assertThat(soldAsset.getQuantity()).isEqualByComparingTo(BigDecimal.valueOf(5));
        }

        @Test
        @DisplayName("Should throw exception when selling more than owned")
        void shouldThrowExceptionWhenSellingMoreThanOwned() {
            // Given - we only own 10 shares from setUp
            RecordSaleCommand largeCommand = new RecordSaleCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    assetId,
                    BigDecimal.valueOf(100), // Trying to sell 100, but only own 10
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Large sale");

            when(commandValidator.validate(largeCommand)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.recordAssetSale(largeCommand))
                    .isInstanceOf(InsufficientFundsException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        //

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            RecordSaleCommand command = mock(RecordSaleCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.recordAssetSale(command))
                    .isInstanceOf(InvalidTransactionException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw AssetNotFoundException when account exists but asset does not")
        void recordAssetSale_WhenAssetNotFound_ThrowsException() {
            // 1. Setup a command with VALID portfolio/account IDs but a RANDOM asset ID
            RecordSaleCommand invalidAssetCommand = new RecordSaleCommand(
                    portfolio.getPortfolioId(), // Use the ID from your @BeforeEach setup
                    accountId, // Use the ID from your @BeforeEach setup
                    AssetId.randomId(), // This is what will trigger the error
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(20), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    null);

            // 2. Mock Validator
            when(commandValidator.validate(invalidAssetCommand)).thenReturn(ValidationResult.success());

            // 3. Mock Repository to return the setup portfolio
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // 4. Assert the specific exception
            // AssetNotFoundException happens because findAccount succeeds, but
            // getAsset(randomId) fails.
            assertThrows(AssetNotFoundException.class, () -> {
                service.recordAssetSale(invalidAssetCommand);
            });
        }

        @Test
        @DisplayName("Should throw error test for lambda")
        void recordAssetSale_WhenPortfolioNotFound_ThrowsException() {
            // 2. Ensure the command actually contains that symbol
            // (Assuming RecordSaleCommand is a record or has a constructor like this)
            RecordSaleCommand command = new RecordSaleCommand(
                    PortfolioId.randomId(),
                    AccountId.randomId(),
                    assetId,
                    BigDecimal.TEN,
                    Money.of(20, "USD"),
                    null,
                    Instant.now(),
                    null);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(command.portfolioId())).thenReturn(Optional.empty());

            // 5. Assert the exception
            assertThrows(PortfolioNotFoundException.class, () -> {
                service.recordAssetSale(command);
            });
        }

        @Test
        void recordAssetSale_WhenAccountNotFound_ThrowsException() {
            AccountId mockAccountId = AccountId.randomId();
            command = new RecordSaleCommand(
                    portfolio.getPortfolioId(),
                    mockAccountId,
                    assetId,
                    BigDecimal.valueOf(5),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Test sale");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> service.recordAssetSale(command))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(mockAccountId.toString());
        }
    }

    @Nested
    @DisplayName("Record Deposit Tests")
    class RecordDepositTests {

        @Test
        @DisplayName("Should successfully record deposit")
        void shouldRecordDeposit() {
            // Given
            RecordDepositCommand command = new RecordDepositCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    new Money(BigDecimal.valueOf(1000), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Deposit");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.recordDeposit(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            RecordDepositCommand command = mock(RecordDepositCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.recordDeposit(command))
                    .isInstanceOf(InvalidTransactionException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw error test for lambda")
        void recordDeposit_WhenPortfolioNotFound_ThrowsException() {
            RecordDepositCommand command = new RecordDepositCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    Money.of(20, "USD"),
                    null,
                    Instant.now(),
                    null);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class, () -> {
                service.recordDeposit(command);
            });
        }

        @Test
        void recordDeposit_WhenAccountNotFound_ThrowsException() {
            AccountId mockAccountId = AccountId.randomId();
            RecordDepositCommand command = new RecordDepositCommand(
                    portfolio.getPortfolioId(),
                    mockAccountId,
                    new Money(BigDecimal.valueOf(1000), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Deposit");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            // when(marketDataService.getAssetInfo(SymbolIdentifier.of("AAPL"))).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> service.recordDeposit(command))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(mockAccountId.toString());
        }
    }

    @Nested
    @DisplayName("Record Withdrawal Tests")
    class RecordWithdrawalTests {

        @Test
        @DisplayName("Should successfully record withdrawal")
        void shouldRecordWithdrawal() {
            // Given
            RecordWithdrawalCommand command = new RecordWithdrawalCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    new Money(BigDecimal.valueOf(500), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Withdrawal");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.recordWithdrawal(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when insufficient cash")
        void shouldThrowExceptionWhenInsufficientCash() {
            // Given
            RecordWithdrawalCommand command = new RecordWithdrawalCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    new Money(BigDecimal.valueOf(50000), ValidatedCurrency.USD), // More than available
                    null,
                    Instant.now(),
                    "Large withdrawal");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.recordWithdrawal(command))
                    .isInstanceOf(InsufficientFundsException.class);
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            RecordWithdrawalCommand command = mock(RecordWithdrawalCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.recordWithdrawal(command))
                    .isInstanceOf(InvalidTransactionException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw error test for lambda")
        void recordWithdrawal_WhenPortfolioNotFound_ThrowsException() {
            RecordWithdrawalCommand command = new RecordWithdrawalCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    Money.of(20, "USD"),
                    null,
                    Instant.now(),
                    null);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(command.portfolioId())).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class, () -> {
                service.recordWithdrawal(command);
            });
        }

        @Test
        void recordWidthdrawal_WhenAccountNotFound_ThrowsException() {
            AccountId mockAccountId = AccountId.randomId();
            RecordWithdrawalCommand command = new RecordWithdrawalCommand(
                    portfolio.getPortfolioId(),
                    mockAccountId,
                    new Money(BigDecimal.valueOf(500), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Withdrawal");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            // when(marketDataService.getAssetInfo(SymbolIdentifier.of("AAPL"))).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> service.recordWithdrawal(command))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(mockAccountId.toString());
        }

    }

    @Nested
    @DisplayName("Record Dividend Income Tests")
    class RecordDividendIncomeTests {

        @BeforeEach
        void setUp() {
            // Add stock position first
            Transaction buyTx = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    identifier,
                    BigDecimal.valueOf(100),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Buy for dividend test");
            portfolio.recordTransaction(accountId, buyTx);
        }

        @Test
        @DisplayName("Should successfully record dividend income without DRIP")
        void shouldRecordDividendWithoutDrip() {
            // Given
            AssetId realAssetId = portfolio.findAccount(accountId)
                    .orElseThrow()
                    .getAssets()
                    .stream()
                    .findFirst() // Assumes AAPL is the first/only asset added in @BeforeEach
                    .orElseThrow()
                    .getAssetId();

            RecordIncomeCommand command = new RecordIncomeCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    realAssetId,
                    new Money(BigDecimal.valueOf(50), ValidatedCurrency.USD),
                    TransactionType.DIVIDEND,
                    false,
                    BigDecimal.ONE,
                    Instant.now(),
                    "Dividend");

            // 3. MOCKING
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            // Use any() for the identifier since we just need the info for the View
            when(marketDataService.getAssetInfo(any())).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(i -> i.getArgument(0));

            // When
            TransactionView response = service.recordDividendIncome(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should successfully record dividend income with DRIP")
        void shouldRecordDividendWithDrip() {
            // 1. GET THE REAL ID from the setup portfolio
            // This ensures the service can actually find the asset in the account
            AssetId realAssetId = portfolio.findAccount(accountId)
                    .orElseThrow()
                    .getAssets()
                    .stream()
                    .findFirst() // Assumes AAPL is the first/only asset added in @BeforeEach
                    .orElseThrow()
                    .getAssetId();

            // 2. USE THE REAL ID in the command
            RecordIncomeCommand command = new RecordIncomeCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    realAssetId, // <--- No longer random!
                    new Money(BigDecimal.valueOf(75), ValidatedCurrency.USD),
                    TransactionType.DIVIDEND,
                    true,
                    BigDecimal.valueOf(0.5),
                    Instant.now(),
                    "DRIP Dividend");

            // 3. MOCKING
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            // Use any() for the identifier since we just need the info for the View
            when(marketDataService.getAssetInfo(any())).thenReturn(Optional.of(assetInfo));
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(i -> i.getArgument(0));

            // 4. WHEN
            TransactionView response = service.recordDividendIncome(command);

            // 5. THEN
            assertThat(response).isNotNull();

            // Capture the saved portfolio to verify the DRIP increased the quantity
            ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
            verify(portfolioRepository).save(captor.capture());

            Asset updatedAsset = captor.getValue()
                    .findAccount(accountId).get()
                    .getAsset(realAssetId);

            // If you started with 10 shares and DRIP'd 0.5, you should have 10.5
            assertThat(updatedAsset.getQuantity().setScale(5))
                    .isEqualByComparingTo(BigDecimal.valueOf(100.5).setScale(5));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            RecordIncomeCommand command = mock(RecordIncomeCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.recordDividendIncome(command))
                    .isInstanceOf(InvalidTransactionException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw AssetNotFoundException when asset ID is valid but not in account")
        void shouldThrowExceptionsWhenAssetNotFound() {
            // Given
            RecordIncomeCommand command = new RecordIncomeCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    AssetId.randomId(), // FIX: Use a random ID, not null
                    new Money(BigDecimal.valueOf(75), ValidatedCurrency.USD),
                    TransactionType.DIVIDEND,
                    true,
                    BigDecimal.valueOf(0.5),
                    Instant.now(),
                    "DRIP Dividend");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When & Then
            assertThatThrownBy(() -> service.recordDividendIncome(command))
                    .isInstanceOf(AssetNotFoundException.class); // Now this will pass

            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw error test for lambda")
        void recordDividend_WhenPortfolioNotFound_ThrowsException() {
            RecordIncomeCommand command = new RecordIncomeCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    AssetId.randomId(),
                    Money.of(20, "USD"),
                    TransactionType.DIVIDEND,
                    false,
                    BigDecimal.ONE,
                    Instant.now(),
                    null);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(command.portfolioId())).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class, () -> {
                service.recordDividendIncome(command);
            });
        }

        @Test
        void recordWidthdrawal_WhenAccountNotFound_ThrowsException() {
            AccountId mockAccountId = AccountId.randomId();
            RecordIncomeCommand command = new RecordIncomeCommand(
                    portfolio.getPortfolioId(),
                    mockAccountId,
                    AssetId.randomId(),
                    new Money(BigDecimal.valueOf(50), ValidatedCurrency.USD),
                    TransactionType.DIVIDEND,
                    false,
                    BigDecimal.ONE,
                    Instant.now(),
                    "Dividend");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            assertThatThrownBy(() -> service.recordDividendIncome(command))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(mockAccountId.toString());
        }

        @Test
        @DisplayName("Dividend DRIP with shares received should calculate quantity and price per share")
        void recordDividendIncome_DripWithShares() {
            PortfolioId portfolioId = PortfolioId.randomId();
            AccountId accountId = AccountId.randomId();

            Portfolio portfolio = new Portfolio(UserId.randomId(), "Test", ValidatedCurrency.USD);
            Account account = Account.createNew(accountId, "Account", AccountType.INVESTMENT, ValidatedCurrency.USD);
            portfolio.addAccount(account);

            // Seed asset via BUY
            Transaction buy = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    SymbolIdentifier.of("AAPL"),
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(100), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Seed");
            portfolio.recordTransaction(accountId, buy);

            Asset asset = account.getAssets().get(0);

            when(portfolioRepository.findById(portfolioId))
                    .thenReturn(Optional.of(portfolio));
            when(commandValidator.validate(any(RecordIncomeCommand.class)))
                    .thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo(any()))
                    .thenReturn(Optional.empty());

            Money dividendAmount = new Money(BigDecimal.valueOf(50), ValidatedCurrency.USD);

            RecordIncomeCommand command = new RecordIncomeCommand(
                    portfolioId,
                    accountId,
                    asset.getAssetId(),
                    dividendAmount,
                    TransactionType.DIVIDEND,
                    false, // DRIP
                    BigDecimal.valueOf(2), // shares received
                    Instant.now(),
                    "DRIP dividend");

            TransactionView result = service.recordDividendIncome(command);

            // IF branch assertions
            assertEquals(BigDecimal.valueOf(1), result.quantity());
            assertEquals(
                    new BigDecimal("50.00"),
                    result.price().amount().setScale(2));
            assertEquals(ValidatedCurrency.USD, result.price().currency());
        }

        @Test
        @DisplayName("Dividend DRIP with shares received should calculate quantity and price per share")
        void recordDividendIncome_DripWithShares2() {
            PortfolioId portfolioId = PortfolioId.randomId();
            AccountId accountId = AccountId.randomId();

            Portfolio portfolio = new Portfolio(UserId.randomId(), "Test", ValidatedCurrency.USD);
            Account account = Account.createNew(accountId, "Account", AccountType.INVESTMENT, ValidatedCurrency.USD);
            portfolio.addAccount(account);

            // Seed asset via BUY
            Transaction buy = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    SymbolIdentifier.of("AAPL"),
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(100), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Seed");
            portfolio.recordTransaction(accountId, buy);

            Asset asset = account.getAssets().get(0);

            when(portfolioRepository.findById(portfolioId))
                    .thenReturn(Optional.of(portfolio));
            when(commandValidator.validate(any(RecordIncomeCommand.class)))
                    .thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo(any()))
                    .thenReturn(Optional.empty());

            Money dividendAmount = new Money(BigDecimal.valueOf(50), ValidatedCurrency.USD);

            RecordIncomeCommand command = new RecordIncomeCommand(
                    portfolioId,
                    accountId,
                    asset.getAssetId(),
                    dividendAmount,
                    TransactionType.DIVIDEND,
                    true, // DRIP
                    BigDecimal.valueOf(2), // shares received
                    Instant.now(),
                    "DRIP dividend");

            TransactionView result = service.recordDividendIncome(command);

            // IF branch assertions
            assertEquals(BigDecimal.valueOf(2), result.quantity());
            assertEquals(
                    new BigDecimal("25.00"),
                    result.price().amount().setScale(2));
            assertEquals(ValidatedCurrency.USD, result.price().currency());
        }

        @Test
        @DisplayName("Dividend non-DRIP fallback branch executes")
        void recordDividendIncome_NonDripFallbackBranch() {
            PortfolioId portfolioId = PortfolioId.randomId();
            AccountId accountId = AccountId.randomId();

            Portfolio portfolio = new Portfolio(UserId.randomId(), "Test", ValidatedCurrency.USD);
            Account account = Account.createNew(accountId, "Account", AccountType.INVESTMENT, ValidatedCurrency.USD);
            portfolio.addAccount(account);

            Transaction buy = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    SymbolIdentifier.of("AAPL"),
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(100), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Seed");
            portfolio.recordTransaction(accountId, buy);

            Asset asset = account.getAssets().get(0);

            when(portfolioRepository.findById(portfolioId))
                    .thenReturn(Optional.of(portfolio));
            when(commandValidator.validate(any(RecordIncomeCommand.class)))
                    .thenReturn(ValidationResult.success());
            when(marketDataService.getAssetInfo(any()))
                    .thenReturn(Optional.empty());

            Money dividendAmount = new Money(BigDecimal.valueOf(40), ValidatedCurrency.USD);

            // Fallback branch: isDrip false, shares null
            RecordIncomeCommand command = new RecordIncomeCommand(
                    portfolioId,
                    accountId,
                    asset.getAssetId(),
                    dividendAmount,
                    TransactionType.DIVIDEND,
                    false, // DRIP false
                    null, // shares null
                    Instant.now(),
                    "Fallback dividend");

            TransactionView result = service.recordDividendIncome(command);

            // ELSE branch assertions
            assertEquals(BigDecimal.ONE, result.quantity());
            assertEquals(dividendAmount.amount(), result.price().amount());
        }

    }

    @Nested
    @DisplayName("Record Fee Tests")
    class RecordFeeTests {

        @Test
        @DisplayName("Should successfully record fee transaction")
        void shouldRecordFee() {

            // Given
            RecordFeeCommand command = new RecordFeeCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    new Money(BigDecimal.valueOf(25.50), ValidatedCurrency.USD),
                    ValidatedCurrency.USD,
                    null,
                    Instant.now(),
                    "Account maintenance fee");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            Money cashBeforeFee = portfolio.findAccount(accountId).get().getCashBalance();

            // When
            TransactionView response = service.recordFee(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));

            // Verify fee reduced cash balance
            ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
            verify(portfolioRepository).save(captor.capture());
            Portfolio savedPortfolio = captor.getValue();
            Optional<Account> savedAccount = savedPortfolio.findAccount(accountId);
            Money cashAfterFee = savedAccount.get().getCashBalance();

            assertThat(cashAfterFee.amount()).isLessThan(cashBeforeFee.amount());
        }

        @Test
        @DisplayName("Should throw exception when validation fails")
        void shouldThrowExceptionWhenValidationFails() {
            // Given
            RecordFeeCommand command = new RecordFeeCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    new Money(BigDecimal.valueOf(-10), ValidatedCurrency.USD), // Invalid negative amount
                    ValidatedCurrency.USD,
                    null,
                    Instant.now(),
                    "Invalid fee");

            when(commandValidator.validate(command))
                    .thenReturn(ValidationResult.failure(List.of("Fee amount must be positive")));

            // When/Then
            assertThatThrownBy(() -> service.recordFee(command))
                    .isInstanceOf(InvalidTransactionException.class)
                    .hasMessageContaining("Invalid fee command");

            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when portfolio not found")
        void shouldThrowExceptionWhenPortfolioNotFound() {
            // Given
            RecordFeeCommand command = new RecordFeeCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    new Money(BigDecimal.valueOf(25), ValidatedCurrency.USD),
                    ValidatedCurrency.USD,
                    null,
                    Instant.now(),
                    "Fee");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.recordFee(command))
                    .isInstanceOf(PortfolioNotFoundException.class);

            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when account not found")
        void shouldThrowExceptionWhenAccountNotFound() {
            // Given
            AccountId nonExistentAccountId = AccountId.randomId();
            RecordFeeCommand command = new RecordFeeCommand(
                    portfolio.getPortfolioId(),
                    nonExistentAccountId,
                    new Money(BigDecimal.valueOf(25), ValidatedCurrency.USD),
                    ValidatedCurrency.USD,
                    null,
                    Instant.now(),
                    "Fee");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.recordFee(command))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(portfolioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should record fee with different currencies")
        void shouldRecordFeeWithDifferentCurrencies() {
            // Given - create account with CAD currency
            AccountId cadAccountId = AccountId.randomId();
            Account cadAccount = Account.createNew(
                    cadAccountId,
                    "CAD Account",
                    AccountType.NON_REGISTERED,
                    ValidatedCurrency.CAD);
            portfolio.addAccount(cadAccount);

            // Add CAD cash
            Transaction cadDeposit = new Transaction(
                    TransactionId.randomId(),
                    cadAccountId,
                    TransactionType.DEPOSIT,
                    new CashIdentifier("CAD"),
                    BigDecimal.ONE,
                    new Money(BigDecimal.valueOf(5000), ValidatedCurrency.CAD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "CAD deposit");
            portfolio.recordTransaction(cadAccountId, cadDeposit);

            RecordFeeCommand command = new RecordFeeCommand(
                    portfolio.getPortfolioId(),
                    cadAccountId,
                    new Money(BigDecimal.valueOf(15.75), ValidatedCurrency.CAD),
                    ValidatedCurrency.CAD,
                    null,
                    Instant.now(),
                    "Foreign exchange fee");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.recordFee(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should record fee with additional fees")
        void shouldRecordFeeWithAdditionalFees() {
            // Given
            Fee additionalFees = new Fee(FeeType.ACCOUNT_MAINTENANCE,
                    new Money(BigDecimal.valueOf(2.50), ValidatedCurrency.USD),
                    ExchangeRate.createSingle(ValidatedCurrency.USD, "null"), null, Instant.now());
            RecordFeeCommand command = new RecordFeeCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    new Money(BigDecimal.valueOf(25), ValidatedCurrency.USD),
                    ValidatedCurrency.USD,
                    List.of(additionalFees),
                    Instant.now(),
                    "Transfer fee with processing charge");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.recordFee(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));

            // Verify both total amount and additional fees were recorded
            ArgumentCaptor<Portfolio> captor = ArgumentCaptor.forClass(Portfolio.class);
            verify(portfolioRepository).save(captor.capture());
        }

        @Test
        @DisplayName("Should record multiple fees in sequence")
        void shouldRecordMultipleFeesInSequence() {
            // Given
            RecordFeeCommand fee1 = new RecordFeeCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    new Money(BigDecimal.valueOf(10), ValidatedCurrency.USD),
                    ValidatedCurrency.USD,
                    null,
                    Instant.now(),
                    "Monthly fee");

            RecordFeeCommand fee2 = new RecordFeeCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    new Money(BigDecimal.valueOf(5), ValidatedCurrency.USD),
                    ValidatedCurrency.USD,
                    null,
                    Instant.now().plusSeconds(60),
                    "Transaction fee");

            when(commandValidator.validate(any(RecordFeeCommand.class))).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response1 = service.recordFee(fee1);
            TransactionView response2 = service.recordFee(fee2);

            // Then
            assertThat(response1).isNotNull();
            assertThat(response2).isNotNull();
            verify(portfolioRepository, times(2)).save(any(Portfolio.class));
        }
    }

    @Nested
    @DisplayName("Update Transaction Tests")
    class UpdateTransactionTests {

        private Transaction existingTransaction;
        private TransactionId transactionId;

        @BeforeEach
        void setUp() {
            transactionId = TransactionId.randomId();

            // Create existing transaction
            existingTransaction = new Transaction(
                    transactionId,
                    accountId,
                    TransactionType.BUY,
                    identifier,
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Original purchase");

            portfolio.recordTransaction(accountId, existingTransaction);
        }

        @Test
        @DisplayName("Should successfully update transaction")
        void shouldUpdateTransaction() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    transactionId,
                    TransactionType.BUY,
                    identifier,
                    BigDecimal.valueOf(15), // Changed quantity
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Updated purchase");

            List<Transaction> transactions = List.of(existingTransaction);
            if (transactions.isEmpty()) {
                fail("Something wrong, transactions are empty here");
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.updateTransaction(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when updating transaction date to future")
        void shouldThrowExceptionWhenDateInFuture() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    transactionId,
                    TransactionType.BUY,
                    new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null),
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().plusSeconds(86400), // Future date
                    "Future purchase");

            List<Transaction> transactions = List.of(existingTransaction);
            if (transactions.isEmpty()) {
                fail("Something went wrong");
            }

            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.updateTransaction(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Transaction date cannot be in the future");
        }

        @Test
        @DisplayName("Should throw exception when updating with negative quantity")
        void shouldThrowExceptionWhenNegativeQuantity() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    transactionId,
                    TransactionType.BUY,
                    new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null),
                    BigDecimal.valueOf(-5), // Negative quantity
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Invalid quantity");
            List<Transaction> transactions = List.of(existingTransaction);
            if (transactions.isEmpty()) {
                fail("Something went wrong");
            }

            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.updateTransaction(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Quantity must be positive");
        }

        @Test
        @DisplayName("Should successfully update SELL transaction with sufficient holdings")
        void shouldUpdateSellTransactionWithSufficientHoldings() {
            // Given - Setup: Buy 100 shares, then sell 50
            Transaction buyTx = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(100),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200), // 2 hours ago
                    "Initial buy");
            portfolio.recordTransaction(accountId, buyTx);

            TransactionId sellTxId = TransactionId.randomId();
            Transaction sellTx = new Transaction(
                    sellTxId,
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(50),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600), // 1 hour ago
                    "Original sell");
            portfolio.recordTransaction(accountId, sellTx);

            // Now update the sell to 60 shares (still within the 100 we own)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    sellTxId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(60), // Increase sell from 50 to 60
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Updated sell");

            List<Transaction> transactions = List.of(buyTx, sellTx);
            if (transactions.isEmpty()) {
                fail("something went wrong");
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.updateTransaction(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when updating SELL transaction exceeds holdings")
        void shouldThrowExceptionWhenUpdatingSellExceedsHoldings() {
            // Given - Setup: Buy 100 shares, then sell 50
            Transaction buyTx = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(100),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200), // 2 hours ago
                    "Initial buy");
            portfolio.recordTransaction(accountId, buyTx);

            TransactionId sellTxId = TransactionId.randomId();
            Transaction sellTx = new Transaction(
                    sellTxId,
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(50),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600), // 1 hour ago
                    "Original sell");
            portfolio.recordTransaction(accountId, sellTx);

            // Try to update sell to 150 shares (more than the 100 we own)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    sellTxId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(150), // Try to sell 150, but only own 100
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Invalid updated sell");

            List<Transaction> transactions = List.of(buyTx, sellTx);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.updateTransaction(command))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient holdings");

            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should validate SELL with multiple prior transactions")
        void shouldValidateSellWithMultiplePriorTransactions() {
            // Given - Complex scenario: Multiple buys and sells
            TransactionId buy1Id = TransactionId.randomId();
            Transaction buy1 = new Transaction(
                    buy1Id,
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(50),
                    new Money(BigDecimal.valueOf(140), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(10800), // 3 hours ago
                    "First buy");
            portfolio.recordTransaction(accountId, buy1);

            TransactionId buy2Id = TransactionId.randomId();
            Transaction buy2 = new Transaction(
                    buy2Id,
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(50),
                    new Money(BigDecimal.valueOf(145), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200), // 2 hours ago
                    "Second buy");
            portfolio.recordTransaction(accountId, buy2);

            TransactionId sell1Id = TransactionId.randomId();
            Transaction sell1 = new Transaction(
                    sell1Id,
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(30),
                    new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600), // 1 hour ago
                    "First sell");
            portfolio.recordTransaction(accountId, sell1);

            // Now update the sell to 70 shares
            // Holdings: 50 + 50 - 30 = 70 shares at time of sale
            // So selling 70 should be exactly at the limit
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    sell1Id,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(70), // Update to sell exactly all holdings
                    new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Updated sell - all shares");

            List<Transaction> transactions = List.of(buy1, buy2, sell1);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.updateTransaction(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should correctly exclude the transaction being updated from validation")
        void shouldExcludeUpdatedTransactionFromValidation() {
            // Given - Buy 100 shares, then sell 100 shares
            Transaction buyTx = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(100),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200),
                    "Buy 100");
            portfolio.recordTransaction(accountId, buyTx);

            TransactionId sellTxId = TransactionId.randomId();
            Transaction sellTx = new Transaction(
                    sellTxId,
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(100),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Sell all 100");
            portfolio.recordTransaction(accountId, sellTx);

            // Update the sell to 90 shares (should work because we exclude the original 100
            // sell)
            // Without exclusion: holdings would be 100 (buy) - 100 (old sell) = 0, can't
            // sell 90
            // With exclusion: holdings = 100 (buy), can sell 90
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    sellTxId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(90), // Reduce sell from 100 to 90
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Updated sell to 90");

            List<Transaction> transactions = List.of(buyTx, sellTx);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.updateTransaction(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when updating with negative price")
        void shouldThrowExceptionWhenNegativePrice() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    transactionId,
                    TransactionType.BUY,
                    new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null),
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(-150), ValidatedCurrency.USD), // Negative price
                    null,
                    Instant.now().minusSeconds(3600),
                    "Invalid price");

            List<Transaction> transactions = List.of(existingTransaction);
            if (transactions.isEmpty()) {
                fail("Something went wrong");
            }

            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.updateTransaction(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price must be positive");
        }

        @Test
        @DisplayName("Should throw exception when transaction not found")
        void shouldThrowExceptionWhenTransactionNotFound() {
            // Given
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    TransactionId.randomId(), // Different ID
                    TransactionType.BUY,
                    new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null),
                    BigDecimal.valueOf(15),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Updated purchase");

            List<Transaction> transactions = List.of(existingTransaction);
            if (transactions.isEmpty()) {
                fail("This is empty when it shouldn't be");
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.updateTransaction(command))
                    .isInstanceOf(TransactionNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            UpdateTransactionCommand command = mock(UpdateTransactionCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.updateTransaction(command))
                    .isInstanceOf(InvalidTransactionException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should correctly calculate holdings from BUY transactions")
        void shouldCalculateHoldingsFromBuyTransactions() {
            // Given - Multiple BUY transactions
            Transaction buy1 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(30),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(10800), // 3 hours ago
                    "First buy");
            portfolio.recordTransaction(accountId, buy1);

            Transaction buy2 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(40),
                    new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200), // 2 hours ago
                    "Second buy");
            portfolio.recordTransaction(accountId, buy2);

            Transaction buy3 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(30),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600), // 1 hour ago
                    "Third buy");
            portfolio.recordTransaction(accountId, buy3);

            TransactionId sellTxId = TransactionId.randomId();
            Transaction sellTx = new Transaction(
                    sellTxId,
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(50),
                    new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(1800), // 30 minutes ago
                    "Original sell");
            portfolio.recordTransaction(accountId, sellTx);

            // Update sell to 100 (should work: 30 + 40 + 30 = 100 shares available)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    sellTxId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(100), // All available shares
                    new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(1800),
                    "Updated sell - all shares");

            List<Transaction> transactions = List.of(buy1, buy2, buy3, sellTx);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.updateTransaction(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should correctly subtract SELL transactions in holdings calculation")
        void shouldSubtractSellTransactionsInHoldingsCalculation() {
            // Given - Buy lots, sell some, then update a later sell
            Transaction buy1 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(200),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(10800), // 3 hours ago
                    "Buy 200");
            portfolio.recordTransaction(accountId, buy1);

            Transaction sell1 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(50),
                    new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200), // 2 hours ago
                    "Sell 50");
            portfolio.recordTransaction(accountId, sell1);

            Transaction sell2 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(30),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(5400), // 1.5 hours ago
                    "Sell 30");
            portfolio.recordTransaction(accountId, sell2);

            TransactionId sell3Id = TransactionId.randomId();
            Transaction sell3 = new Transaction(
                    sell3Id,
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(20),
                    new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600), // 1 hour ago
                    "Sell 20");
            portfolio.recordTransaction(accountId, sell3);

            // Holdings at sell3 time: 200 (buy1) - 50 (sell1) - 30 (sell2) - 20 (sell3
            // excluded) = 120
            // Update sell3 to 120 (maximum possible)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    sell3Id,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(120), // Sell all remaining shares
                    new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Updated sell to 120");

            List<Transaction> transactions = List.of(buy1, sell1, sell2, sell3);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.updateTransaction(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should fail when SELL transactions reduce holdings below update quantity")
        void shouldFailWhenSellTransactionsReduceHoldings() {
            // Given - Explicitly test that SELL subtracts from holdings
            Transaction buy1 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(100),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(10800), // 3 hours ago
                    "Buy 100");
            portfolio.recordTransaction(accountId, buy1);

            // This SELL reduces available holdings
            Transaction sell1 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(60), // Sell 60, leaving 40
                    new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200), // 2 hours ago
                    "Sell 60");
            portfolio.recordTransaction(accountId, sell1);

            TransactionId sell2Id = TransactionId.randomId();
            Transaction sell2 = new Transaction(
                    sell2Id,
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(10),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600), // 1 hour ago
                    "Sell 10");
            portfolio.recordTransaction(accountId, sell2);

            // Try to update sell2 to 50
            // Holdings: 100 (buy) - 60 (sell1) = 40, but trying to sell 50
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    sell2Id,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(50), // More than available 40
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Updated sell to 50");

            List<Transaction> transactions = List.of(buy1, sell1, sell2);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then - Should fail because sell1 reduced holdings to 40
            assertThatThrownBy(() -> service.updateTransaction(command))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient holdings")
                    .hasMessageContaining("Available: 40")
                    .hasMessageContaining("Attempting to sell: 50");

            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should correctly calculate holdings with mixed BUY and SELL transactions")
        void shouldCalculateHoldingsWithMixedTransactions() {
            // Given - Mixed BUY and SELL transactions
            Transaction buy1 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(100),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(10800), // 3 hours ago
                    "Buy 100");
            portfolio.recordTransaction(accountId, buy1);

            Transaction sell1 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(30),
                    new Money(BigDecimal.valueOf(155), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200), // 2 hours ago
                    "Sell 30");
            portfolio.recordTransaction(accountId, sell1);

            Transaction buy2 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(50),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(5400), // 1.5 hours ago
                    "Buy 50");
            portfolio.recordTransaction(accountId, buy2);

            Transaction sell2 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(20),
                    new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600), // 1 hour ago
                    "Sell 20");
            portfolio.recordTransaction(accountId, sell2);

            // Now update sell2 to sell more shares
            // Holdings at that point: 100 (buy1) - 30 (sell1) + 50 (buy2) - 20 (sell2
            // excluded) = 120
            TransactionId sell2Id = sell2.getTransactionId();
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    sell2Id,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(100), // Update to sell 100 (was 20)
                    new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "Updated sell to 100");

            List<Transaction> transactions = List.of(buy1, sell1, buy2, sell2);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.updateTransaction(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should ignore transactions after the sell date")
        void shouldIgnoreTransactionsAfterSellDate() {
            // Given - Transactions both before AND after the sell date
            Transaction buy1 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(50),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200), // 2 hours ago
                    "Buy 50 BEFORE sell");
            portfolio.recordTransaction(accountId, buy1);

            TransactionId sellTxId = TransactionId.randomId();
            Instant sellDate = Instant.now().minusSeconds(3600); // 1 hour ago
            Transaction sellTx = new Transaction(
                    sellTxId,
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(30),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    sellDate,
                    "Original sell");
            portfolio.recordTransaction(accountId, sellTx);

            // This BUY happens AFTER the sell date, so should be ignored in validation
            Transaction buy2 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(100), // This should NOT count towards available holdings
                    new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(1800), // 30 minutes ago (AFTER sell)
                    "Buy 100 AFTER sell");
            portfolio.recordTransaction(accountId, buy2);

            // Try to update sell to 60 shares
            // Should fail because only 50 shares available at sell time (buy2 is after)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    sellTxId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(60), // Trying to sell more than the 50 available
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    sellDate,
                    "Updated sell");

            List<Transaction> transactions = List.of(buy1, sellTx, buy2);
            if (transactions.isEmpty()) {
                fail();
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));

            // When/Then
            assertThatThrownBy(() -> service.updateTransaction(command))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insufficient holdings")
                    .hasMessageContaining("Available: 50")
                    .hasMessageContaining("Attempting to sell: 60");

            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should successfully update when transactions after sell date exist but not needed")
        void shouldUpdateSuccessfullyWithFutureTransactions() {
            // Given - Similar setup but valid update
            Transaction buy1 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(100),
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(7200), // 2 hours ago
                    "Buy 100 BEFORE sell");
            portfolio.recordTransaction(accountId, buy1);

            TransactionId sellTxId = TransactionId.randomId();
            Instant sellDate = Instant.now().minusSeconds(3600); // 1 hour ago
            Transaction sellTx = new Transaction(
                    sellTxId,
                    accountId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(30),
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    sellDate,
                    "Original sell");
            portfolio.recordTransaction(accountId, sellTx);

            // This happens after, so ignored
            Transaction buy2 = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(50),
                    new Money(BigDecimal.valueOf(165), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(1800), // 30 minutes ago (AFTER sell)
                    "Buy 50 AFTER sell");
            portfolio.recordTransaction(accountId, buy2);

            // Update sell to 80 (valid because 100 shares available at sell time)
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    sellTxId,
                    TransactionType.SELL,
                    assetInfo.toIdentifier(),
                    BigDecimal.valueOf(80), // Valid: only considering buy1 (100 shares)
                    new Money(BigDecimal.valueOf(160), ValidatedCurrency.USD),
                    null,
                    sellDate,
                    "Updated sell to 80");

            List<Transaction> transactions = List.of(buy1, sellTx, buy2);
            if (transactions.isEmpty()) {
                fail("Something wen't wrong");
            }
            Page<Transaction> page = new PageImpl<>(transactions);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);

            // When
            TransactionView response = service.updateTransaction(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw error test for lambda")
        void recorUpdateTransaction_WhenPortfolioNotFound_ThrowsException() {
            UpdateTransactionCommand command = new UpdateTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    TransactionId.randomId(),
                    TransactionType.BUY,
                    mock(AssetIdentifier.class),
                    BigDecimal.ONE,
                    Money.of(20, "USD"),
                    null,
                    Instant.now(),
                    null);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(command.portfolioId())).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class, () -> {
                service.updateTransaction(command);
            });
        }
    }

    @Nested
    @DisplayName("Delete Transaction Tests")
    class DeleteTransactionTests {

        private Transaction transaction;
        private TransactionId transactionId;

        @BeforeEach
        void setUp() {
            transactionId = TransactionId.randomId();

            transaction = new Transaction(
                    transactionId,
                    accountId,
                    TransactionType.BUY,
                    identifier,
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now().minusSeconds(3600),
                    "To be deleted");

            portfolio.recordTransaction(accountId, transaction);
        }

        @Test
        @DisplayName("Should successfully delete transaction")
        void shouldDeleteTransaction() {
            // Given
            DeleteTransactionCommand command = new DeleteTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    transactionId,
                    false,
                    null);

            List<Transaction> transactionList = List.of(transaction);
            if (transactionList.isEmpty()) {
                fail("Something wrong here");
            }
            Page<Transaction> page = new PageImpl<>(transactionList);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                    .thenReturn(page.getContent());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            // When
            service.deleteTransaction(command);

            // Then
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            DeleteTransactionCommand command = mock(DeleteTransactionCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.deleteTransaction(command))
                    .isInstanceOf(InvalidTransactionException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        void deleteTransaction_WhenTransactionIdNotFoundInList_ThrowsException() {
            // Arrange
            TransactionId targetId = TransactionId.randomId();

            DeleteTransactionCommand command = new DeleteTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    targetId,
                    false,
                    "reason");

            // Mock validator
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());

            // Mock list with a DIFFERENT ID so the .filter() fails to find a match
            Transaction otherTransaction = mock(Transaction.class);
            when(otherTransaction.getTransactionId()).thenReturn(TransactionId.randomId());
            when(transactionQueryService.getAllTransactions(any())).thenReturn(List.of(otherTransaction));

            // Act & Assert
            assertThrows(TransactionNotFoundException.class, () -> service.deleteTransaction(command));
        }

        @Test
        void deleteTransaction_WhenPortfolioNotFound_ThrowsException() {
            // Arrange
            TransactionId targetId = TransactionId.randomId();

            DeleteTransactionCommand command = new DeleteTransactionCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    targetId,
                    false,
                    "reason");

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());

            // 1. Transaction must be found first to reach the portfolio check
            Transaction foundTxn = mock(Transaction.class);
            when(foundTxn.getTransactionId()).thenReturn(targetId);
            when(transactionQueryService.getAllTransactions(any())).thenReturn(List.of(foundTxn));

            // 2. Mock Repository to return EMPTY
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(PortfolioNotFoundException.class, () -> service.deleteTransaction(command));
        }
    }

    @Nested
    @DisplayName("Account Management Tests")
    class AccountManagementTests {

        @Test
        @DisplayName("Should successfully add account")
        void shouldAddAccount() {
            PortfolioId portfolioId = PortfolioId.randomId();
            // Given
            AddAccountCommand command = new AddAccountCommand(
                    portfolioId,
                    "New Account",
                    AccountType.TFSA,
                    ValidatedCurrency.CAD);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(portfolio);
            when(portfolioViewAssembler.assembleAccountView(any())).thenReturn(mock(AccountView.class));

            // When
            AccountView response = service.addAccount(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should successfully remove empty account")
        void shouldRemoveEmptyAccount() {
            PortfolioId portfolioId = PortfolioId.randomId();
            // Given
            AccountId newAccountId = AccountId.randomId();
            Account emptyAccount = Account.createNew(newAccountId, "Empty", AccountType.TFSA, ValidatedCurrency.CAD);
            emptyAccount.close();
            portfolio.addAccount(emptyAccount);

            RemoveAccountCommand command = new RemoveAccountCommand(portfolioId, newAccountId);

            when(commandValidator.validate(any(RemoveAccountCommand.class))).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            // When
            service.removeAccount(command);

            // Then
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when removing non-empty account")
        void shouldThrowExceptionWhenRemovingNonEmptyAccount() {
            // Given - add an asset to the account to make it non-empty
            PortfolioId portfolioId = PortfolioId.randomId();
            Transaction buyTx = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetInfo.toIdentifier(),
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Asset to prevent deletion");
            portfolio.recordTransaction(accountId, buyTx);

            RemoveAccountCommand command = new RemoveAccountCommand(portfolioId, accountId);
            when(commandValidator.validate(any(RemoveAccountCommand.class))).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            // Verify account actually has assets before the test
            Optional<Account> accountToRemove = portfolio.findAccount(accountId);
            assertThat(accountToRemove.get().getAssets()).isNotEmpty();

            // When/Then
            assertThatThrownBy(() -> service.removeAccount(command))
                    .isInstanceOf(AccountNotEmptyException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            AddAccountCommand command = mock(AddAccountCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.addAccount(command))
                    .isInstanceOf(InvalidTransactionException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        void addAccount_WhenPortfolioNotFound_ThrowsException() {
            PortfolioId portfolioId = PortfolioId.randomId();
            AddAccountCommand command = new AddAccountCommand(
                    portfolioId,
                    "some name",
                    AccountType.CHEQUING,
                    ValidatedCurrency.USD);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class, () -> service.addAccount(command));
        }

        @Test
        void removeAccount_WhenPortfolioNotFound_ThrowsException() {
            PortfolioId portfolioId = PortfolioId.randomId();
            RemoveAccountCommand command = new RemoveAccountCommand(
                    portfolioId,
                    accountId);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class, () -> service.removeAccount(command));
        }

        @Test
        void removeAccount_WhenAccountNotFound_ThrowsException() {
            PortfolioId portfolioId = PortfolioId.randomId();
            AccountId mockAccountId = AccountId.randomId();
            RemoveAccountCommand command = new RemoveAccountCommand(
                    portfolioId, mockAccountId);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
            // when(portfolio.findAccount(any())).thenReturn();

            assertThatThrownBy(() -> service.removeAccount(command))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(mockAccountId.toString());
        }

        @Test
        void removeAccount_WhenInvalidTransaction_ThrowsException() {
            PortfolioId portfolioId = PortfolioId.randomId();
            RemoveAccountCommand command = new RemoveAccountCommand(portfolioId, accountId);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("reason"));

            assertThrows(InvalidTransactionException.class, () -> service.removeAccount(command));
        }
    }

    @Nested
    @DisplayName("Portfolio Lifecycle Tests")
    class PortfolioLifecycleTests {
        private ValidatedCurrency USD = ValidatedCurrency.USD;

        @Test
        @DisplayName("Should successfully create portfolio")
        void shouldCreatePortfolio() {
            // Given
            UserId newUserId = UserId.randomId();
            CreatePortfolioCommand command = new CreatePortfolioCommand(
                    newUserId,
                    name,
                    ValidatedCurrency.USD,
                    "desc here",
                    true);

            Portfolio newPortfolio = new Portfolio(newUserId, name, ValidatedCurrency.USD);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.countByUserId(newUserId)).thenReturn(0L);
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(newPortfolio);
            when(portfolioViewAssembler.assemblePortfolioView(any())).thenReturn(mock(PortfolioView.class));

            // When
            PortfolioView response = service.createPortfolio(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should successfully create portfolio w/o default account")
        void shouldCreatePortfolioNoAccount() {
            // Given
            UserId newUserId = UserId.randomId();
            CreatePortfolioCommand command = new CreatePortfolioCommand(
                    newUserId,
                    name,
                    ValidatedCurrency.USD,
                    null,
                    false);

            Portfolio newPortfolio = new Portfolio(newUserId, name, ValidatedCurrency.USD);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            // when(portfolioRepository.findById(newPortfolio.getPortfolioId())).thenReturn(Optional.empty());
            when(portfolioRepository.save(any(Portfolio.class))).thenReturn(newPortfolio);
            when(portfolioViewAssembler.assemblePortfolioView(any())).thenReturn(mock(PortfolioView.class));

            // When
            PortfolioView response = service.createPortfolio(command);

            // Then
            assertThat(response).isNotNull();
            verify(portfolioRepository).save(any(Portfolio.class));
            assertThat(response.accounts()).isEmpty();
        }

        @Test
        @DisplayName("Should throw exception when user has reached portfolio limit")
        void shouldThrowExceptionWhenPortfolioLimitReached() {
            // Given
            CreatePortfolioCommand command = new CreatePortfolioCommand(
                    userId,
                    name,
                    ValidatedCurrency.USD,
                    null,
                    true);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());

            // Change: Mock the count instead of an Optional find
            // We return 1 (or more) to trigger the "limit reached" logic
            when(portfolioRepository.countByUserId(userId)).thenReturn(1L);

            // When/Then
            assertThatThrownBy(() -> service.createPortfolio(command))
                    // Suggestion: Rename exception to PortfolioLimitReachedException
                    // to better reflect the new business logic
                    .isInstanceOf(PortfolioLimitReachedException.class)
                    .hasMessageContaining("limit");
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid")
        void shouldThrowExceptionsWhenValidationResultIsNotValid() {
            CreatePortfolioCommand command = mock(CreatePortfolioCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.createPortfolio(command))
                    .isInstanceOf(InvalidCommandException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        // update portfolio
        @Test
        @DisplayName("Should update and return portfolio view when portfolio exists")
        void updatePortfolio_Success() {
            // Arrange
            PortfolioId portfolioId = PortfolioId.randomId();
            UpdatePortfolioCommand command = new UpdatePortfolioCommand(portfolioId, "Updated name", USD, "Desc 2");

            Portfolio existingPortfolio = mock(Portfolio.class);
            Portfolio updatedPortfolio = mock(Portfolio.class);
            PortfolioView expectedView = mock(PortfolioView.class);

            // 1. Stub the repository to find the existing portfolio
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(existingPortfolio));

            // 2. Stub the domain logic: the update call returns the updated aggregate
            when(existingPortfolio.updatePortfolio(anyString(), anyString(), any())).thenReturn(updatedPortfolio);

            // 3. Stub the repository save: return the updated aggregate
            when(portfolioRepository.save(updatedPortfolio)).thenReturn(updatedPortfolio);

            // 4. Stub the assembler: map the aggregate to the view
            when(portfolioViewAssembler.assemblePortfolioView(updatedPortfolio)).thenReturn(expectedView);

            // Act
            PortfolioView result = service.updatePortfolio(command);

            // Assert
            assertNotNull(result);
            assertEquals(expectedView, result);

            // Verify the full lifecycle
            verify(portfolioRepository).findById(portfolioId);
            verify(existingPortfolio).updatePortfolio(
                    command.name(),
                    command.description(),
                    command.defaultCurrency());
            verify(portfolioRepository).save(updatedPortfolio);
            verify(portfolioViewAssembler).assemblePortfolioView(updatedPortfolio);
        }

        @Test
        @DisplayName("Should throw Exception when portfolio to update is not found")
        void updatePortfolio_NotFound() {
            // Arrange
            PortfolioId portfolioId = PortfolioId.randomId();
            UpdatePortfolioCommand command = new UpdatePortfolioCommand(portfolioId, "Updated name", USD, "Desc 2");

            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(PortfolioNotFoundException.class, () -> {
                service.updatePortfolio(command);
            });

            // Verify that save and assemble were never called
            verify(portfolioRepository, never()).save(any());
            verify(portfolioViewAssembler, never()).assemblePortfolioView(any());
        }

        // delete portfolio //
        @Test
        @DisplayName("Should successfully delete empty portfolio with confirmation")
        void shouldDeleteEmptyPortfolio() {
            // Given - create a portfolio with no accounts
            UserId newUserId = UserId.randomId();
            Portfolio emptyPortfolio = new Portfolio(newUserId, name, ValidatedCurrency.USD);
            PortfolioId portfolioId = emptyPortfolio.getPortfolioId();

            DeletePortfolioCommand command = new DeletePortfolioCommand(portfolioId, userId, true, false);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(emptyPortfolio));

            // Verify portfolio is empty before deletion
            assertThat(emptyPortfolio.getAccounts()).isEmpty();
            assertThat(emptyPortfolio.containsAccounts()).isFalse();

            // When
            service.deletePortfolio(command);

            // Then
            verify(portfolioRepository).delete(emptyPortfolio.getPortfolioId());
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        @DisplayName("Should successfully delete empty portfolio with confirmation")
        void shouldDeleteEmptyPortfolioSoftDelete() {
            // Given - create a portfolio with no accounts
            UserId newUserId = UserId.randomId();
            Portfolio emptyPortfolio = new Portfolio(newUserId, name, ValidatedCurrency.USD);
            PortfolioId portfolioId = emptyPortfolio.getPortfolioId();

            DeletePortfolioCommand command = new DeletePortfolioCommand(portfolioId, userId, true, true);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(emptyPortfolio));

            // Verify portfolio is empty before deletion
            assertThat(emptyPortfolio.getAccounts()).isEmpty();
            assertThat(emptyPortfolio.containsAccounts()).isFalse();

            // When
            service.deletePortfolio(command);

            // Then
            verify(portfolioRepository).save(emptyPortfolio);
            verify(portfolioRepository, never()).delete(any(PortfolioId.class));
            assertThat(emptyPortfolio.getDeletedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Should throw exception when deleting without confirmation")
        void shouldThrowExceptionWhenDeletingWithoutConfirmation() {
            // Given
            PortfolioId portfolioId = PortfolioId.randomId();
            DeletePortfolioCommand command = new DeletePortfolioCommand(portfolioId, userId, false, true);
            // When/Then
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            assertThatThrownBy(() -> service.deletePortfolio(command))
                    .isInstanceOf(PortfolioDeletionRequiresConfirmationException.class);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-empty portfolio")
        void shouldThrowExceptionWhenDeletingNonEmptyPortfolio() {
            // Given - portfolio already has accounts from setUp()
            PortfolioId portfolioId = PortfolioId.randomId();
            DeletePortfolioCommand command = new DeletePortfolioCommand(portfolioId, userId, true, true);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

            // Verify portfolio actually has accounts before the test
            assertThat(portfolio.getAccounts()).isNotEmpty();
            assertThat(portfolio.containsAccounts()).isTrue();

            // When/Then - portfolio has accounts, should fail
            assertThatThrownBy(() -> service.deletePortfolio(command))
                    .isInstanceOf(PortfolioNotEmptyException.class)
                    .hasMessageContaining("Cannot delete portfolio with existing accounts");

            // Verify delete was never called
            verify(portfolioRepository, never()).delete(any(PortfolioId.class));
        }

        @Test
        @DisplayName("Should throw exception when validation result is not valid delete ver")
        void shouldThrowExceptionsWhenValidationResultIsNotValidDelete() {
            DeletePortfolioCommand command = mock(DeletePortfolioCommand.class);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("ERROR"));

            assertThatThrownBy(() -> service.deletePortfolio(command))
                    .isInstanceOf(InvalidCommandException.class);

            // Verify no save was attempted
            verify(portfolioRepository, never()).save(any(Portfolio.class));
        }

        @Test
        void deletePortfolio_WhenPortfolioNotFound_ThrowsException() {
            PortfolioId portfolioId = PortfolioId.randomId();
            DeletePortfolioCommand command = new DeletePortfolioCommand(portfolioId, userId, true, false);

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            // when(portfolioRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class, () -> service.deletePortfolio(command));
        }
    }

    @Nested
    @DisplayName("Correct Asset Ticker Tests")
    class CorrectAssetTickerTests {

        @Test
        @DisplayName("Should successfully correct asset ticker")
        void shouldCorrectAssetTicker() {
            // 1. Given: Record the initial "wrong" transaction
            Transaction buyTx = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    new MarketIdentifier("AAPL", null, AssetType.STOCK, "Appled", "USD", null),
                    BigDecimal.TEN,
                    new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                    null,
                    Instant.now(),
                    "Wrong ticker");

            portfolio.recordTransaction(accountId, buyTx);

            // 2. FIX: Capture the REAL AssetId that was generated by the recordTransaction
            // logic
            AssetId generatedId = portfolio.findAccount(accountId)
                    .orElseThrow()
                    .getAssets()
                    .stream()
                    .filter(a -> a.getAssetIdentifier().getPrimaryId().equals("AAPL"))
                    .findFirst()
                    .orElseThrow()
                    .getAssetId();

            // 3. Use that real ID in your command
            CorrectAssetTickerCommand command = new CorrectAssetTickerCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    generatedId, // <--- No longer random!
                    new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple", "USD", null));

            // 4. Mocks
            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.of(portfolio));
            when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(i -> i.getArgument(0));

            // 5. When
            service.correctAssetTicket(command);

            // 6. Then
            verify(portfolioRepository).save(any(Portfolio.class));

            // Optional: Verify the name actually changed in the asset
            Asset correctedAsset = portfolio.findAccount(accountId).get().getAssets().stream().findFirst().get();
            assertThat(correctedAsset.getAssetIdentifier().displayName()).isEqualTo("Apple");
        }

        @Test
        void correctAssetTicket_WhenPortfolioNotFound_ThrowsException() {
            CorrectAssetTickerCommand command = new CorrectAssetTickerCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    mock(AssetId.class),
                    mock(AssetIdentifier.class));

            when(commandValidator.validate(command)).thenReturn(ValidationResult.success());
            when(portfolioRepository.findById(portfolio.getPortfolioId())).thenReturn(Optional.empty());

            assertThrows(PortfolioNotFoundException.class, () -> service.correctAssetTicket(command));
        }

        @Test
        void correctAssetTicket_WhenInvalidTransaction_ThrowsException() {
            CorrectAssetTickerCommand command = new CorrectAssetTickerCommand(
                    portfolio.getPortfolioId(),
                    accountId,
                    mock(AssetId.class),
                    mock(AssetIdentifier.class));

            when(commandValidator.validate(command)).thenReturn(ValidationResult.failure("reason"));

            assertThrows(InvalidTransactionException.class, () -> service.correctAssetTicket(command));
        }
    }

    @Nested
    @DisplayName("Rnadom Tests for coverage")
    public class InnerPortfolioApplicationServiceTest {
        @Test
        @DisplayName("Should correctly calculate holdings and allow valid sell")
        void testValidateSellTransaction_FullCoverage() throws Exception {
            PortfolioApplicationService portfolioApplicationService = new PortfolioApplicationService(
                    portfolioRepository, transactionQueryService, marketDataService, exchangeRateService,
                    commandValidator,
                    portfolioViewAssembler);
            // 1. Setup method access
            Method method = PortfolioApplicationService.class.getDeclaredMethod(
                    "validateSellTransaction",
                    AssetIdentifier.class, BigDecimal.class, Instant.class, List.class, TransactionId.class);
            method.setAccessible(true);

            // 2. Mock/Setup Data
            Instant sellDate = Instant.parse("2023-10-10T10:00:00Z");
            TransactionId excludeId = TransactionId.randomId();

            List<Transaction> transactions = List.of(
                    // 1. Branch: BUY (holdings + 10)
                    createMockTx(TransactionId.randomId(), TransactionType.BUY, new BigDecimal("10.0"),
                            sellDate.minusSeconds(100)),

                    // 2. Branch: SELL (holdings - 2)
                    createMockTx(TransactionId.randomId(), TransactionType.SELL, new BigDecimal("2.0"),
                            sellDate.minusSeconds(50)),

                    // 3. Branch: THE ELSE (Neither BUY nor SELL)
                    // This is what solves your coverage issue. Use a type like DIVIDEND or DEPOSIT.
                    createMockTx(TransactionId.randomId(), TransactionType.DIVIDEND, new BigDecimal("1.0"),
                            sellDate.minusSeconds(40)),

                    // 4. Branch: Exclude (Skip)
                    createMockTx(excludeId, TransactionType.BUY, new BigDecimal("100.0"), sellDate.minusSeconds(10)));

            // Ensure we pass the service instance as the first argument
            assertDoesNotThrow(() -> method.invoke(portfolioApplicationService, identifier, new BigDecimal("5.0"),
                    sellDate, transactions, excludeId));
        }

        private Transaction createMockTx(TransactionId transactionId, TransactionType sell, BigDecimal bigDecimal,
                Instant minusSeconds) {
            return new Transaction(
                    transactionId,
                    accountId,
                    sell,
                    identifier,
                    bigDecimal,
                    new Money(bigDecimal, ValidatedCurrency.USD),
                    null,
                    minusSeconds,
                    null);
        }

    }
}
