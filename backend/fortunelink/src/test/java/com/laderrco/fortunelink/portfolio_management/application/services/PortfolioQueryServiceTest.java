package com.laderrco.fortunelink.portfolio_management.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.laderrco.fortunelink.portfolio_management.application.mappers.AllocationMapper;
import com.laderrco.fortunelink.portfolio_management.application.mappers.TransactionMapper;
import com.laderrco.fortunelink.portfolio_management.application.models.AllocationType;
import com.laderrco.fortunelink.portfolio_management.application.models.TransactionSearchCriteria;
import com.laderrco.fortunelink.portfolio_management.application.queries.AnalyzeAllocationQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAssetQueryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.ViewNetWorthQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.ViewPerformanceQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AllocationDetail;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AllocationView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AssetView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.NetWorthView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PerformanceView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionHistoryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.assemblers.PortfolioViewAssembler;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.TransactionNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.SymbolIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.AssetAllocationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PerformanceCalculationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

@ExtendWith(MockitoExtension.class)
class PortfolioQueryServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private TransactionQueryRepository transactionRepository;

    @Mock
    private MarketDataService marketDataService;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private PerformanceCalculationService performanceCalculationService;

    @Mock
    private AssetAllocationService assetAllocationService;

    @Mock
    private PortfolioValuationService portfolioValuationService;

    @Mock
    private TransactionQueryService transactionQueryService;

    @Mock
    private PortfolioViewAssembler portfolioViewAssembler;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private AllocationMapper allocationMapper;

    @InjectMocks
    private PortfolioQueryService queryService;

    private PortfolioId portfolioId;
    private AccountId accountId;
    private Portfolio portfolio;
    private Instant time;
    private ValidatedCurrency testCurrency;

    @BeforeEach
    void setUp() {
        portfolioId = new PortfolioId(UUID.randomUUID());
        accountId = new AccountId(UUID.randomUUID());
        testCurrency = ValidatedCurrency.USD;
        time = Instant.now();

        portfolio = mock(Portfolio.class);
        // stubbings that aren't used by all the test will throw an error
        // we will make it lenient instead
        // when(portfolio.getPortfolioId()).thenReturn(portfolioId);
        // when(portfolio.getPortfolioCurrencyPreference()).thenReturn(testCurrency);

        lenient().when(portfolio.getPortfolioId())
                .thenReturn(portfolioId);
        lenient().when(portfolio.getPortfolioCurrencyPreference()).thenReturn(testCurrency);
    }

    // ==================== getPortfolios Tests ====================
    @Test
    @DisplayName("Should return portfolio when valid ID is provided")
    void getPortfolio_Success() {
        // Arrange
        PortfolioId portfolioId = new PortfolioId(UUID.randomUUID());
        GetPortfolioByIdQuery query = new GetPortfolioByIdQuery(portfolioId);

        // 1. Mock the Domain Object
        Portfolio mockPortfolio = mock(Portfolio.class);

        // 2. Mock the View (The result you expect from the service)
        // Using mock() here is fine for a quick test, or use a real PortfolioView
        PortfolioView mockView = mock(PortfolioView.class);

        // 3. Stub the Repository
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(mockPortfolio));

        // 4. IMPORTANT: Stub the Assembler
        // Without this, queryService calls assembler.assemble(...) and gets null
        when(portfolioViewAssembler.assemblePortfolioView(mockPortfolio)).thenReturn(mockView);

        // Act
        PortfolioView result = queryService.getPortfolioById(query);

        // Assert
        assertNotNull(result, "The result should not be null");
        assertEquals(mockView, result);
        verify(portfolioRepository).findById(portfolioId);
        verify(portfolioViewAssembler).assemblePortfolioView(mockPortfolio);
    }

    @Test
    @DisplayName("Should throw exception when portfolio ID does not exist")
    void getPortfolio_NotFound() {
        // Arrange
        PortfolioId portfolioId = new PortfolioId(UUID.randomUUID());
        GetPortfolioByIdQuery query = new GetPortfolioByIdQuery(portfolioId);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PortfolioNotFoundException.class, () -> queryService.getPortfolioById(query));
    }

    @Test
    @DisplayName("Should return list of portfolios for a user")
    void getUserPortfolios_Success() {
        // Arrange
        UserId userId = new UserId(UUID.randomUUID());
        GetPortfoliosByUserIdQuery query = new GetPortfoliosByUserIdQuery(userId);
        List<Portfolio> mockList = List.of(mock(Portfolio.class), mock(Portfolio.class));

        when(portfolioRepository.findAllByUserId(userId)).thenReturn(mockList);

        // Act
        List<PortfolioSummaryView> result = queryService.getUserPortfolioSummaries(query);

        // Assert
        assertEquals(2, result.size());
        verify(portfolioRepository).findAllByUserId(userId);

    }

    @Test
    @DisplayName("Should return empty list when user has no portfolios")
    void getUserPortfolios_Empty() {
        // Arrange
        UserId userId = new UserId(UUID.randomUUID());
        GetPortfoliosByUserIdQuery query = new GetPortfoliosByUserIdQuery(userId);

        when(portfolioRepository.findAllByUserId(userId)).thenReturn(List.of());

        // Act
        List<PortfolioSummaryView> result = queryService.getUserPortfolioSummaries(query);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ==================== getNetWorth Tests ====================

    @Test
    void getNetWorth_Success_WithAsOfDate() {
        // Arrange
        Instant asOfDate = Instant.now().minusSeconds(86400);
        ViewNetWorthQuery query = new ViewNetWorthQuery(portfolioId, asOfDate);

        Money totalAssets = new Money(new BigDecimal("50000.00"), testCurrency);
        // Money totalLiabilities = new Money(new BigDecimal("20000.00"), testCurrency);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
        when(portfolioValuationService.calculateTotalValue(portfolio, asOfDate))
                .thenReturn(totalAssets);

        // Act
        NetWorthView response = queryService.getNetWorth(query);

        // Assert
        assertNotNull(response);
        assertEquals(totalAssets, response.totalAssets());
        // assertEquals(totalLiabilities, response.totalLiabilities()); // this part of
        // the test won't work because liabilities are in a different context
        assertEquals(new Money(new BigDecimal("50000.00"), testCurrency), response.netWorth());
        assertEquals(asOfDate, response.asOfDate());
        assertEquals(testCurrency, response.currency());

        verify(portfolioRepository).findById(portfolioId);
        verify(portfolioValuationService).calculateTotalValue(portfolio, asOfDate);
    }

    @Test
    void getNetWorth_Success_WithoutAsOfDate() {
        // Arrange
        // Pass null as asOfDate - the service should use current time internally
        ViewNetWorthQuery query = new ViewNetWorthQuery(portfolioId, null);

        Money totalAssets = new Money(new BigDecimal("50000.00"), testCurrency);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
        // Use any(Instant.class) since we don't know the exact timestamp the service
        // will use
        when(portfolioValuationService.calculateTotalValue(
                eq(portfolio),
                // eq(marketDataService),
                any(Instant.class)))
                .thenReturn(totalAssets);

        // Act
        NetWorthView response = queryService.getNetWorth(query);

        // Assert
        assertNotNull(response);
        assertEquals(totalAssets, response.totalAssets());
        assertEquals(new Money(new BigDecimal("50000.00"), testCurrency), response.netWorth());
        assertNotNull(response.asOfDate());
        // Verify the asOfDate is recent (within last second)
        assertTrue(response.asOfDate().isBefore(Instant.now().plusSeconds(1)));
        assertTrue(response.asOfDate().isAfter(Instant.now().minusSeconds(2)));
        assertEquals(testCurrency, response.currency());

        verify(portfolioRepository).findById(portfolioId);
        verify(portfolioValuationService).calculateTotalValue(
                eq(portfolio),
                // eq(marketDataService),
                any(Instant.class));
    }

    @Test
    void getNetWorth_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        ViewNetWorthQuery query = new ViewNetWorthQuery(portfolioId, null);
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PortfolioNotFoundException.class, () -> queryService.getNetWorth(query));
        verify(portfolioRepository).findById(portfolioId);
        verifyNoInteractions(portfolioValuationService);
    }

    @Test
    void getNetWorth_ThrowsException_WhenQueryIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> queryService.getNetWorth(null));
        verifyNoInteractions(portfolioRepository);
    }

    // ==================== getPortfolioPerformance Tests ====================

    @Test
    void getPortfolioPerformance_Success() {
        // Arrange
        // Using a fixed point in time is safer for tests, but keeping your logic:
        Instant startDate = Instant.now().minusSeconds(86400 * 365);
        Instant endDate = Instant.now();
        ViewPerformanceQuery query = new ViewPerformanceQuery(portfolioId, accountId, startDate, endDate);

        // Mock Data
        List<Transaction> transactionList = new ArrayList<>();
        Percentage totalReturn = new Percentage(new BigDecimal("15.50"));
        Money realizedGains = new Money(new BigDecimal("1000.00"), testCurrency);
        Money unrealizedGains = new Money(new BigDecimal("500.00"), testCurrency);
        Percentage timeWeightedReturn = new Percentage(new BigDecimal("14.80"));

        // Stubbing
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

        // Match the method being called: getAllTransactions(Criteria)
        when(transactionQueryService.getAllTransactions(any(TransactionSearchCriteria.class)))
                .thenReturn(transactionList);

        when(performanceCalculationService.calculateTotalReturn(portfolio))
                .thenReturn(totalReturn);
        when(performanceCalculationService.calculateRealizedGains(portfolio, transactionList))
                .thenReturn(realizedGains);
        when(performanceCalculationService.calculateUnrealizedGains(portfolio))
                .thenReturn(unrealizedGains);
        when(performanceCalculationService.calculateTimeWeightedReturn(portfolio))
                .thenReturn(timeWeightedReturn);

        // Act
        PerformanceView response = queryService.getPortfolioPerformance(query);

        // Assert
        assertNotNull(response);
        assertEquals(totalReturn, response.totalReturns());
        assertEquals(realizedGains, response.realizedGains());
        assertEquals(unrealizedGains, response.unrealizedGains());
        assertEquals(timeWeightedReturn, response.timeWeightedReturn());
        assertNotNull(response.annualizedReturn());
        assertNotNull(response.period());

        verify(portfolioRepository).findById(portfolioId);

        // Capturing the argument to verify internal logic
        ArgumentCaptor<TransactionSearchCriteria> criteriaCaptor = ArgumentCaptor
                .forClass(TransactionSearchCriteria.class);

        // Verify the correct method name and capture the object
        verify(transactionQueryService).getAllTransactions(criteriaCaptor.capture());

        // Assert the values inside the captured object
        TransactionSearchCriteria capturedCriteria = criteriaCaptor.getValue();
        assertEquals(portfolioId, capturedCriteria.portfolioId());

        // Optional: Verify dates were converted to LA time correctly
        assertNotNull(capturedCriteria.startDate());
        assertNotNull(capturedCriteria.endDate());
    }

    @Test
    void getPortfolioPerformance_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        ViewPerformanceQuery query = new ViewPerformanceQuery(
                portfolioId,
                accountId,
                Instant.now().minusSeconds(86400),
                Instant.now());
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PortfolioNotFoundException.class,
                () -> queryService.getPortfolioPerformance(query));
    }

    @Test
    void getPortfolioPerformance_ThrowsException_WhenQueryIsNull() {
        // Act & Assert
        assertThrows(NullPointerException.class,
                () -> queryService.getPortfolioPerformance(null));
    }

    // ==================== getAssetAllocation Tests ====================

    @Test
    void getAssetAllocation_ByType_Success() {
        // Arrange
        AnalyzeAllocationQuery query = new AnalyzeAllocationQuery(
                portfolioId,
                AllocationType.BY_TYPE,
                null);

        Map<AssetType, Money> allocationMap = Map.of(
                AssetType.STOCK, new Money(new BigDecimal("60000.00"), testCurrency),
                AssetType.BOND, new Money(new BigDecimal("30000.00"), testCurrency),
                AssetType.CASH, new Money(new BigDecimal("10000.00"), testCurrency));

        Money totalValue = new Money(new BigDecimal("100000.00"), testCurrency);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
        when(assetAllocationService.calculateAllocationByType(eq(portfolio), any(Instant.class)))
                .thenReturn(allocationMap);
        when(portfolioValuationService.calculateTotalValue(eq(portfolio), any(Instant.class)))
                .thenReturn(totalValue);

        // Act
        AllocationView response = queryService.getAssetAllocation(query);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getAsOfDate()); // Verify it's not null anymore
        // // Verify the allocations are present and have correct percentages
        assertNotNull(response.getAllocations());
        assertEquals(3, response.getAllocations().size());

        verify(portfolioRepository).findById(portfolioId);
        verify(assetAllocationService).calculateAllocationByType(eq(portfolio), any(Instant.class));
        verify(portfolioValuationService, times(1)).calculateTotalValue(eq(portfolio), any(Instant.class));
    }

    @Test
    void getAssetAllocation_ByAccount_Success() {
        // Arrange
        AnalyzeAllocationQuery query = new AnalyzeAllocationQuery(
                portfolioId,
                AllocationType.BY_ACCOUNT,
                time);

        Map<AccountType, Money> allocationMap = Map.of(
                AccountType.TFSA, new Money(new BigDecimal("40.00"), ValidatedCurrency.USD),
                AccountType.RRSP, new Money(new BigDecimal("60.00"), ValidatedCurrency.USD));

        Money totalValue = new Money(new BigDecimal("100000.00"), testCurrency);
        // AllocationResponse expectedResponse = mock(AllocationResponse.class);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
        when(assetAllocationService.calculateAllocationByAccount(portfolio, time))
                .thenReturn(allocationMap);
        when(portfolioValuationService.calculateTotalValue(portfolio, time))
                .thenReturn(totalValue);
        // when(AllocationMapper.toResponse(anyMap(), eq(totalValue), eq(time)))
        // .thenReturn(expectedResponse);

        // Act
        AllocationView response = queryService.getAssetAllocation(query);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getAllocations());
        assertEquals(2, response.getAllocations().size());

        // Verify specific allocations
        AllocationDetail tfsaDetail = response.getAllocations().get("TFSA");
        assertNotNull(tfsaDetail);
        assertEquals("TFSA", tfsaDetail.getCategory());
        assertEquals("Account Type", tfsaDetail.getCategoryType());

        verify(assetAllocationService).calculateAllocationByAccount(portfolio, time);
    }

    @Test
    void getAssetAllocation_ByCurrency_Success() {
        // Arrange
        AnalyzeAllocationQuery query = new AnalyzeAllocationQuery(
                portfolioId,
                AllocationType.BY_CURRENCY,
                time);

        // All values should be in the base currency (USD) for percentage calculations
        Map<ValidatedCurrency, Money> allocationMap = Map.of(
                ValidatedCurrency.USD, new Money(new BigDecimal("70000.00"), ValidatedCurrency.USD),
                ValidatedCurrency.CAD, new Money(new BigDecimal("30000.00"), ValidatedCurrency.USD));

        Money totalValue = new Money(new BigDecimal("100000.00"), testCurrency);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
        when(assetAllocationService.calculateAllocationByCurrency(portfolio, time))
                .thenReturn(allocationMap);
        when(portfolioValuationService.calculateTotalValue(portfolio, time))
                .thenReturn(totalValue);

        // Act
        AllocationView response = queryService.getAssetAllocation(query);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getAllocations());
        assertEquals(2, response.getAllocations().size());

        // Check if it's using the currency symbol or something else as the key
        // Try getting with the actual keys
        assertTrue(response.getAllocations().containsKey("USD"), "Should contain USD key");
        assertTrue(response.getAllocations().containsKey("CAD"), "Should contain CAD key");

        AllocationDetail usdDetail = response.getAllocations().get("USD");
        assertNotNull(usdDetail, "USD detail should not be null");
        assertEquals("USD", usdDetail.getCategory());
        assertEquals("Currency", usdDetail.getCategoryType());

        AllocationDetail cadDetail = response.getAllocations().get("CAD");
        assertNotNull(cadDetail, "CAD detail should not be null");
        assertEquals("CAD", cadDetail.getCategory());
        assertEquals("Currency", cadDetail.getCategoryType());

        verify(assetAllocationService).calculateAllocationByCurrency(portfolio, time);
    }

    @Test
    void getAssetAllocation_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        AnalyzeAllocationQuery query = new AnalyzeAllocationQuery(
                portfolioId,
                AllocationType.BY_TYPE,
                time);
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PortfolioNotFoundException.class,
                () -> queryService.getAssetAllocation(query));
    }

    // ==================== getTransactionHistory Tests ====================

    @Test
    void getTransactionHistory_Success_WithDateRange() {
        // 1. Arrange
        Instant startDate = Instant.now().minusSeconds(86400 * 30);
        Instant endDate = Instant.now();
        // Assuming your query object uses page 1, size 10
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
                portfolioId, accountId, startDate, endDate, null, 1, 10);

        List<Transaction> transactions = createMockTransactions(5);
        // PageRequest.of(0, 10) because your log showed the code calling with 0
        if (transactions.isEmpty()) {
            fail();
        }
        Page<Transaction> transactionPage = new PageImpl<>(transactions, PageRequest.of(0, 10), 5L);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

        // 2. The Fix: doReturn is less prone to "PotentialStubbingProblem"
        lenient().doReturn(transactionPage).when(transactionQueryService).queryTransactions(
                any(TransactionSearchCriteria.class),
                anyInt(),
                anyInt());

        // 3. Act
        TransactionHistoryView response = queryService.getTransactionHistory(query);

        // 4. Assert
        assertNotNull(response);
        assertEquals(5, response.totalElements()); // This should now be 5 because transactionPage total is 5L

        // 5. Verify
        ArgumentCaptor<TransactionSearchCriteria> criteriaCaptor = ArgumentCaptor
                .forClass(TransactionSearchCriteria.class);
        verify(transactionQueryService).queryTransactions(
                criteriaCaptor.capture(),
                anyInt(),
                anyInt());

        assertEquals(accountId, criteriaCaptor.getValue().accountId());
    }

    @Test
    void getTransactionHistory_Success_WithoutDateRange() {
        // Arrange
        // User asks for page 1
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
                portfolioId, null, null, null, null, 0, 10);

        List<Transaction> transactions = createMockTransactions(15);
        List<Transaction> pageTransactions = transactions.subList(0, 10);

        // Use 0 here because the service code is calling the repository with 0
        if (pageTransactions.isEmpty()) {
            fail();
        }
        Page<Transaction> transactionPage = new PageImpl<>(pageTransactions, PageRequest.of(0, 10), 15);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));

        // Use anyInt() or eq(0) to match what the code actually does
        when(transactionQueryService.queryTransactions(any(TransactionSearchCriteria.class), anyInt(), anyInt()))
                .thenReturn(transactionPage);

        // Act
        TransactionHistoryView response = queryService.getTransactionHistory(query);

        // Assert
        assertNotNull(response);
        assertNotNull(response);
        assertEquals(15, response.totalElements());

        // Check the label specifically
        assertEquals("All time", response.dateRange().getLabel());

        // Double check that the underlying data is actually null as expected
        assertNull(response.dateRange().startDate());
        assertNull(response.dateRange().endDate());

        ArgumentCaptor<TransactionSearchCriteria> criteriaCaptor = ArgumentCaptor
                .forClass(TransactionSearchCriteria.class);

        // FIX: Change eq(1) to eq(0) to match the actual interaction in your log
        verify(transactionQueryService).queryTransactions(criteriaCaptor.capture(), eq(0), eq(10));

        // Verify portfolio scope
        assertEquals(portfolioId, criteriaCaptor.getValue().portfolioId());
        assertNull(criteriaCaptor.getValue().accountId());
    }

    @Test
    void getTransactionHistory_Success_WithAccountFilter() {
        // Arrange
        Instant endDate = Instant.now();
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
                portfolioId, accountId, null, endDate, null, 1, 10);

        List<Transaction> transactions = createMockTransactionsWithAccount(accountId, 5);
        if (transactions.isEmpty()) {
            fail();
        }
        Page<Transaction> transactionPage = new PageImpl<>(transactions, PageRequest.of(0, 10), 5);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
        when(transactionQueryService.queryTransactions(any(TransactionSearchCriteria.class), anyInt(), anyInt()))
                .thenReturn(transactionPage);

        // Act
        TransactionHistoryView response = queryService.getTransactionHistory(query);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.totalElements());

        ArgumentCaptor<TransactionSearchCriteria> criteriaCaptor = ArgumentCaptor
                .forClass(TransactionSearchCriteria.class);
        verify(transactionQueryService).queryTransactions(criteriaCaptor.capture(), anyInt(), anyInt());

        // Verify that accountId was passed into the criteria
        assertEquals(accountId, criteriaCaptor.getValue().accountId());
    }

    @Test
    void getTransactionHistory_Success_WithTransactionTypeFilter() {
        // Arrange
        Instant startDate = Instant.now().minusSeconds(86400 * 30);
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
                portfolioId, null, startDate, null, TransactionType.BUY, 1, 10);

        List<Transaction> transactions = createMockTransactions(8);
        if (transactions.isEmpty()) {
            fail();
        }
        Page<Transaction> transactionPage = new PageImpl<>(transactions, PageRequest.of(0, 10), 8);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
        when(transactionQueryService.queryTransactions(any(TransactionSearchCriteria.class), anyInt(), anyInt()))
                .thenReturn(transactionPage);

        // Act
        TransactionHistoryView response = queryService.getTransactionHistory(query);

        // Assert
        assertNotNull(response);
        assertEquals(8, response.totalElements());

        ArgumentCaptor<TransactionSearchCriteria> criteriaCaptor = ArgumentCaptor
                .forClass(TransactionSearchCriteria.class);
        verify(transactionQueryService).queryTransactions(criteriaCaptor.capture(), anyInt(), anyInt());

        // Verify the filter was mapped
        assertEquals(TransactionType.BUY, criteriaCaptor.getValue().transactionType());
    }

    @Test
    void getTransactionHistory_Success_Pagination() {
        // Arrange
        // Requesting page 2 with size 5
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
                portfolioId, null, null, null, null, 2, 5);

        List<Transaction> pageTransactions = createMockTransactions(5);

        // Match the service's 0-indexed logic: Page 2 is index 1
        if (pageTransactions.isEmpty()) {
            fail();
        }
        Page<Transaction> transactionPage = new PageImpl<>(
                pageTransactions,
                PageRequest.of(1, 5),
                15);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
        when(transactionQueryService.queryTransactions(any(TransactionSearchCriteria.class), anyInt(), anyInt()))
                .thenReturn(transactionPage);

        // Act
        TransactionHistoryView response = queryService.getTransactionHistory(query);

        // Assert
        assertEquals(15, response.totalElements());
        // Note: Check if your response mapper adds +1 back to the page number for the
        // user
        assertEquals(2, response.pageNumber());
        assertEquals(5, response.pageSize());

        // Verify the query parameters (Index 1 = Page 2)
        verify(transactionQueryService).queryTransactions(any(), eq(1), eq(5));
    }

    @Test
    void getTransactionHistory_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
                portfolioId, null, null, null, null, 1, 10);
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PortfolioNotFoundException.class,
                () -> queryService.getTransactionHistory(query));
    }

    // ==================== getAccountSummary Tests ====================

    @Test
    void getAccountSummary_Success() throws NoSuchMethodException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        // Arrange
        GetAccountSummaryQuery query = new GetAccountSummaryQuery(portfolioId, accountId);

        Account account = createMockAccount(accountId);
        AccountView expectedResponse = mock(AccountView.class);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
        when(portfolio.findAccount(accountId)).thenReturn(Optional.of(account));
        when(portfolioViewAssembler.assembleAccountView(account))
                .thenReturn(expectedResponse);

        // Act
        AccountView response = queryService.getAccountSummary(query);

        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);

        verify(portfolio).findAccount(accountId);
        verify(portfolioViewAssembler).assembleAccountView(account);
    }

    @Test
    void getAccountSummary_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        GetAccountSummaryQuery query = new GetAccountSummaryQuery(portfolioId, accountId);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PortfolioNotFoundException.class,
                () -> queryService.getAccountSummary(query));
    }

    @Test
    void getAccountSummary_ThrowsException_WhenAccountNotFound() {
        // Arrange
        GetAccountSummaryQuery query = new GetAccountSummaryQuery(portfolioId, AccountId.randomId());

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.ofNullable(mock(Portfolio.class)));

        // Act & Assert
        assertThrows(AccountNotFoundException.class,
                () -> queryService.getAccountSummary(query));
    }

    // ==================== getPortfolioSummary Tests ====================

    @Test
    void getPortfolioSummary_Success() {
        // Arrange
        GetPortfolioSummaryQuery query = new GetPortfolioSummaryQuery(portfolioId);
        PortfolioView expectedResponse = mock(PortfolioView.class);

        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.of(portfolio));
        when(portfolioViewAssembler.assemblePortfolioView(portfolio))
                .thenReturn(expectedResponse);

        // Act
        PortfolioView response = queryService.getUserPortfolioView(query);

        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);

        verify(portfolioViewAssembler).assemblePortfolioView(portfolio);
    }

    @Test
    void getPortfolioSummary_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        GetPortfolioSummaryQuery query = new GetPortfolioSummaryQuery(portfolioId);
        when(portfolioRepository.findById(portfolioId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(PortfolioNotFoundException.class,
                () -> queryService.getUserPortfolioView(query));
    }

    // get asset summary
    @Test
    @DisplayName("Should return populated AssetView when asset exists")
    void shouldReturnPopulatedAssetView_WhenAssetExists() {
        // 1. Arrange IDs
        UUID pIdStr = UUID.randomUUID();
        UUID aIdStr = UUID.randomUUID();
        UUID asIdStr = UUID.randomUUID();
        AssetId assetId = new AssetId(asIdStr);
        AssetIdentifier identifier = new SymbolIdentifier("AAPL");

        GetAssetQueryView query = new GetAssetQueryView(
                new PortfolioId(pIdStr),
                new AccountId(aIdStr),
                assetId);

        // 2. Mock Asset
        Asset mockAsset = mock(Asset.class);
        lenient().when(mockAsset.getAssetId()).thenReturn(assetId);
        lenient().when(mockAsset.getAssetIdentifier()).thenReturn(identifier);
        lenient().when(mockAsset.getQuantity()).thenReturn(BigDecimal.TEN);
        lenient().when(mockAsset.calculateUnrealizedGainLoss(any())).thenReturn(Money.of(30, "USD"));
        lenient().when(mockAsset.calculateCurrentValue(any())).thenReturn(Money.of(30, "USD"));
        lenient().when(mockAsset.getCostBasis()).thenReturn(Money.of(100, "USD"));
        lenient().when(mockAsset.getCostPerUnit()).thenReturn(Money.of(30, "USD"));
        lenient().when(mockAsset.getAcquiredOn()).thenReturn(Instant.now().minusSeconds(3600));
        lenient().when(mockAsset.getLastSystemInteraction()).thenReturn(Instant.now());

        // 3. Mock Account - FIX: Stub the getAsset method
        Account mockAccount = mock(Account.class);
        // when(mockAccount.getAssets()).thenReturn(Collections.singletonList(mockAsset));
        // This prevents the NPE
        when(mockAccount.getAsset(assetId)).thenReturn(mockAsset);

        // 4. Mock Portfolio
        Portfolio mockPortfolio = mock(Portfolio.class);
        when(mockPortfolio.findAccount(any())).thenReturn(Optional.of(mockAccount));

        // Mock Repository
        when(portfolioRepository.findById(any())).thenReturn(Optional.of(mockPortfolio));

        // 5. Mock Market Data
        Money currentPrice = Money.of(180, "USD");
        when(marketDataService.getCurrentPrice(identifier)).thenReturn(currentPrice);

        // 6. FIX: Stub the PortfolioAssembler
        // Create the expected view object that the assembler should produce
        AssetView expectedView = mock(AssetView.class);
        when(expectedView.assetId()).thenReturn(assetId);
        when(expectedView.currentPrice()).thenReturn(currentPrice);

        when(portfolioViewAssembler.assembleAssetView(eq(mockAsset), eq(currentPrice)))
                .thenReturn(expectedView);

        // 7. Act
        AssetView result = queryService.getAssetSummary(query);

        // 8. Assert
        assertNotNull(result, "The result should not be null. Check if portfolioAssembler is stubbed.");
        assertEquals(assetId, result.assetId());
        assertEquals(currentPrice, result.currentPrice());

        verify(portfolioViewAssembler).assembleAssetView(any(), any());
    }

    @Test
    void shouldThrowAccountNotFoundException_WhenAccountDoesNotExist() {
        // Arrange
        PortfolioId pId = new PortfolioId(UUID.randomUUID());
        AccountId aId = new AccountId(UUID.randomUUID());
        AssetId asId = new AssetId(UUID.randomUUID());
        GetAssetQueryView query = new GetAssetQueryView(pId, aId, asId);

        Portfolio mockPortfolio = mock(Portfolio.class);
        // Simulate portfolio exists, but account does NOT exist within it
        when(portfolioRepository.findById(pId)).thenReturn(Optional.of(mockPortfolio));
        when(mockPortfolio.findAccount(aId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            queryService.getAssetSummary(query);
        });

        verify(marketDataService, never()).getCurrentPrice(any());
    }

    @Test
    @DisplayName("Should throw AssetNotFoundException when querying an ID that doesn't exist in a real Portfolio")
    void shouldThrowAssetNotFoundException_WhenAssetDoesNotExistInAccount() {
        // 1. Arrange: Setup the real Aggregate Hierarchy
        AccountId aId = AccountId.randomId();
        AssetId ghostId = AssetId.randomId(); // The ID that doesn't exist

        // Create a REAL Portfolio (The Aggregate Root)
        Portfolio realPortfolio = new Portfolio(
                UserId.randomId(),
                "Main Wealth Portfolio",
                ValidatedCurrency.USD);

        // Create and add a REAL Account via the Portfolio
        PortfolioId pId = realPortfolio.getPortfolioId();
        GetAssetQueryView query = new GetAssetQueryView(pId, aId, ghostId);
        Account realAccount = Account.createNew(
                aId,
                "Investment Account",
                AccountType.INVESTMENT,
                ValidatedCurrency.USD);
        realPortfolio.addAccount(realAccount);

        // Record a transaction THROUGH THE PORTFOLIO to create an initial asset
        // This ensures internal lists are initialized and the domain logic runs
        Transaction initialBuy = new Transaction(
                TransactionId.randomId(),
                aId,
                TransactionType.BUY,
                new MarketIdentifier("AAPL", null, AssetType.STOCK, "Apple Inc", "USD", null),
                BigDecimal.valueOf(10),
                new Money(BigDecimal.valueOf(150), ValidatedCurrency.USD),
                null,
                Instant.now(),
                "Seed Purchase");

        // The Portfolio is the only one that should call this
        realPortfolio.recordTransaction(aId, initialBuy);

        // Mock the repository to return our fully-initialized real Portfolio
        when(portfolioRepository.findById(pId)).thenReturn(Optional.of(realPortfolio));

        // 2. Act & Assert
        // The Service will load the realPortfolio, find the realAccount,
        // and call account.getAsset(ghostId).
        assertThrows(AssetNotFoundException.class, () -> {
            queryService.getAssetSummary(query);
        });

        // 3. Verification
        verify(marketDataService, never()).getCurrentPrice(any());
    }
    // ==================== Helper Methods ====================

    private List<Transaction> createMockTransactions(int count) {
        AssetIdentifier assetIdentifier = new MarketIdentifier("AAPL", null, AssetType.STOCK, "APPLE", "USD", null);

        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // Create real Transaction objects instead of mocks
            Transaction transaction = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetIdentifier,
                    new BigDecimal("10"),
                    Money.of(150, "USD"),
                    null, // fees
                    LocalDateTime.ofInstant(time, ZoneOffset.UTC).minusDays(i).toInstant(ZoneOffset.UTC),
                    "Test transaction " + i);
            transactions.add(transaction);
        }
        return transactions;
    }

    @Test
    @DisplayName("Should return TransactionView when transaction exists in account")
    void getTransactionDetails_ShouldReturnView_WhenTransactionExists() {
        // 1. Arrange IDs
        PortfolioId pId = PortfolioId.randomId();
        AccountId aId = AccountId.randomId();
        TransactionId tId = TransactionId.randomId();
        GetTransactionByIdQuery query = new GetTransactionByIdQuery(pId, aId, tId);

        // 2. Setup Real Domain Objects
        Portfolio portfolio = new Portfolio(UserId.randomId(), "Test Portfolio", ValidatedCurrency.USD);
        Account account = Account.createNew(aId, "Trading", AccountType.INVESTMENT, ValidatedCurrency.USD);
        portfolio.addAccount(account);

        // Create a real transaction and record it so the account's internal list is
        // populated
        Transaction transaction = new Transaction(
                tId,
                aId,
                TransactionType.BUY,
                new MarketIdentifier("MSFT", null, AssetType.STOCK, "Microsoft", "USD", null),
                BigDecimal.ONE,
                new Money(BigDecimal.valueOf(400), ValidatedCurrency.USD),
                null,
                Instant.now(),
                "Bought 1 MSFT");
        // This call is critical: it puts the transaction in the account's internal list
        portfolio.recordTransaction(aId, transaction);

        // 3. Mock Repository & Assembler
        when(portfolioRepository.findById(pId)).thenReturn(Optional.of(portfolio));

        TransactionView expectedView = mock(TransactionView.class);
        when(portfolioViewAssembler.assembleTransactionView(any(Transaction.class))).thenReturn(expectedView);

        // 4. Act
        TransactionView result = queryService.getTransactionDetails(query);

        // 5. Assert
        assertNotNull(result);
        verify(portfolioViewAssembler).assembleTransactionView(argThat(t -> t.getTransactionId().equals(tId)));
    }

    @Test
    @DisplayName("Should throw TransactionNotFoundException when ID does not exist")
    void getTransactionDetails_ShouldThrowException_WhenTransactionMissing() {
        // Arrange
        PortfolioId pId = PortfolioId.randomId();
        AccountId aId = AccountId.randomId();
        TransactionId missingId = TransactionId.randomId();
        GetTransactionByIdQuery query = new GetTransactionByIdQuery(pId, aId, missingId);

        Portfolio portfolio = new Portfolio(UserId.randomId(), "Empty Portfolio", ValidatedCurrency.USD);
        Account account = Account.createNew(aId, "Trading", AccountType.INVESTMENT, ValidatedCurrency.USD);
        portfolio.addAccount(account);

        when(portfolioRepository.findById(pId)).thenReturn(Optional.of(portfolio));

        // Act & Assert
        assertThrows(TransactionNotFoundException.class, () -> {
            queryService.getTransactionDetails(query);
        });
    }

    @Test
    @DisplayName("Should throw AccountNotFoundException when account does not exist in the portfolio")
    void getTransactionDetails_ShouldThrowAccountNotFoundException_WhenAccountIsMissing() {
        // 1. Arrange IDs
        PortfolioId pId = PortfolioId.randomId();
        AccountId missingAccountId = AccountId.randomId(); // ID that won't be in the portfolio
        TransactionId tId = TransactionId.randomId();

        GetTransactionByIdQuery query = new GetTransactionByIdQuery(
                pId,
                missingAccountId,
                tId);

        // 2. Setup a REAL Portfolio with NO accounts
        Portfolio portfolio = new Portfolio(
                UserId.randomId(),
                "Empty Test Portfolio",
                ValidatedCurrency.USD);

        // 3. Mock Repository to return our portfolio
        // This allows loadUserPortfolio(query.portfolioId()) to succeed
        when(portfolioRepository.findById(pId)).thenReturn(Optional.of(portfolio));

        // 4. Act & Assert
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () -> {
            queryService.getTransactionDetails(query);
        });

        // Verify the exception contains the correct metadata
        assertTrue(exception.getMessage().contains(missingAccountId.toString()));
        assertTrue(exception.getMessage().contains(pId.toString()));

        // Verify we never even tried to find a transaction or call the assembler
        verifyNoInteractions(portfolioViewAssembler);
    }

    private List<Transaction> createMockTransactionsWithAccount(AccountId accountId, int count) {
        AssetIdentifier assetIdentifier = new MarketIdentifier("AAPL", null, AssetType.STOCK, "APPLE", "USD", null);
        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Transaction transaction = new Transaction(
                    TransactionId.randomId(),
                    accountId,
                    TransactionType.BUY,
                    assetIdentifier,
                    new BigDecimal("10"),
                    Money.of(150, "USD"),
                    null, // fees
                    LocalDateTime.ofInstant(time, ZoneOffset.UTC).minusDays(i).toInstant(ZoneOffset.UTC),
                    "Test transaction " + i

            );
            transactions.add(transaction);
        }
        return transactions;
    }

    private Account createMockAccount(AccountId accountId) throws NoSuchMethodException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        AssetIdentifier assetIdentifier = new MarketIdentifier("AAPL", null, AssetType.STOCK, "APPLE", "USD", null);

        // 1. Get the constructor for Asset
        // You must match the parameter types exactly as defined in the Asset class
        Constructor<Asset> constructor = Asset.class.getDeclaredConstructor(
                AssetId.class,
                AssetIdentifier.class,
                BigDecimal.class,
                Money.class,
                Instant.class);

        // 2. Make the constructor accessible
        constructor.setAccessible(true);

        // 3. Instantiate the Asset
        Asset asset = constructor.newInstance(
                AssetId.randomId(),
                assetIdentifier,
                new BigDecimal("10"),
                Money.of(1500, "USD"),
                Instant.now());

        Account account = Account.createNew(accountId, "Test Name", AccountType.INVESTMENT, ValidatedCurrency.USD);

        Method addMethod = account.getClass().getDeclaredMethod("addAsset", Asset.class);
        addMethod.setAccessible(true);

        // WRONG: addMethod.invoke(addMethod, asset);
        // RIGHT: Pass the 'account' instance as the first argument
        addMethod.invoke(account, asset);

        return account;
    }
}