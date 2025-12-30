package com.laderrco.fortunelink.portfolio_management.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.laderrco.fortunelink.portfolio_management.application.mappers.AllocationMapper;
import com.laderrco.fortunelink.portfolio_management.application.mappers.PortfolioMapper;
import com.laderrco.fortunelink.portfolio_management.application.mappers.TransactionMapper;
import com.laderrco.fortunelink.portfolio_management.application.models.AllocationType;
import com.laderrco.fortunelink.portfolio_management.application.queries.AnalyzeAllocationQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.ViewNetWorthQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.ViewPerformanceQuery;
import com.laderrco.fortunelink.portfolio_management.application.responses.AccountResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationDetail;
import com.laderrco.fortunelink.portfolio_management.application.responses.AllocationResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.NetWorthResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PerformanceResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.PortfolioResponse;
import com.laderrco.fortunelink.portfolio_management.application.responses.TransactionHistoryResponse;
import com.laderrco.fortunelink.portfolio_management.domain.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.TransactionType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.MarketIdentifier;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.TransactionId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.TransactionQueryRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.AssetAllocationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PerformanceCalculationService;
import com.laderrco.fortunelink.portfolio_management.domain.services.PortfolioValuationService;
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
    private PerformanceCalculationService performanceCalculationService;
    
    @Mock
    private AssetAllocationService assetAllocationService;
    
    @Mock
    private PortfolioValuationService portfolioValuationService;
    
    @Mock
    private PortfolioMapper portfolioMapper;
    
    @Mock
    private TransactionMapper transactionMapper;
    
    @Mock
    private AllocationMapper allocationMapper;
    
    @InjectMocks
    private PortfolioQueryService queryService;
    
    private UserId userId;
    private PortfolioId portfolioId;
    private AccountId accountId;
    private Portfolio portfolio;
    private Instant time; 
    private ValidatedCurrency testCurrency;
    
    @BeforeEach
    void setUp() {
        userId = new UserId(UUID.randomUUID());
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
    
    // ==================== getNetWorth Tests ====================
    
    @Test
    void getNetWorth_Success_WithAsOfDate() {
        // Arrange
        Instant asOfDate = Instant.now().minusSeconds(86400);
        ViewNetWorthQuery query = new ViewNetWorthQuery(userId, asOfDate);
        
        Money totalAssets = new Money(new BigDecimal("50000.00"), testCurrency);
        // Money totalLiabilities = new Money(new BigDecimal("20000.00"), testCurrency);
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioValuationService.calculateTotalValue(portfolio, marketDataService, asOfDate))
            .thenReturn(totalAssets);
        
        // Act
        NetWorthResponse response = queryService.getNetWorth(query);
        
        // Assert
        assertNotNull(response);
        assertEquals(totalAssets, response.totalAssets());
        // assertEquals(totalLiabilities, response.totalLiabilities()); // this part of the test won't work because liabilities are in a different context
        assertEquals(new Money(new BigDecimal("50000.00"), testCurrency), response.netWorth());
        assertEquals(asOfDate, response.asOfDate());
        assertEquals(testCurrency, response.currency());
        
        verify(portfolioRepository).findByUserId(userId);
        verify(portfolioValuationService).calculateTotalValue(portfolio, marketDataService, asOfDate);
    }
    
    @Test
    void getNetWorth_Success_WithoutAsOfDate() {
        // Arrange
        // Pass null as asOfDate - the service should use current time internally
        ViewNetWorthQuery query = new ViewNetWorthQuery(userId, null);
        
        Money totalAssets = new Money(new BigDecimal("50000.00"), testCurrency);
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        // Use any(Instant.class) since we don't know the exact timestamp the service will use
        when(portfolioValuationService.calculateTotalValue(
                eq(portfolio), 
                eq(marketDataService), 
                any(Instant.class)))
            .thenReturn(totalAssets);
        
        // Act
        NetWorthResponse response = queryService.getNetWorth(query);
        
        // Assert
        assertNotNull(response);
        assertEquals(totalAssets, response.totalAssets());
        assertEquals(new Money(new BigDecimal("50000.00"), testCurrency), response.netWorth());
        assertNotNull(response.asOfDate());
        // Verify the asOfDate is recent (within last second)
        assertTrue(response.asOfDate().isBefore(Instant.now().plusSeconds(1)));
        assertTrue(response.asOfDate().isAfter(Instant.now().minusSeconds(2)));
        assertEquals(testCurrency, response.currency());
        
        verify(portfolioRepository).findByUserId(userId);
        verify(portfolioValuationService).calculateTotalValue(
                eq(portfolio), 
                eq(marketDataService), 
                any(Instant.class));
    }
    
    @Test
    void getNetWorth_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        ViewNetWorthQuery query = new ViewNetWorthQuery(userId, null);
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(PortfolioNotFoundException.class, () -> queryService.getNetWorth(query));
        verify(portfolioRepository).findByUserId(userId);
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
        Instant startDate = Instant.now().minusSeconds(86400 * 365);
        Instant endDate = Instant.now();
        ViewPerformanceQuery query = new ViewPerformanceQuery(userId, accountId, startDate, endDate);
        
        List<Transaction> transactions = Collections.emptyList();
        Percentage totalReturn = new Percentage(new BigDecimal("15.50"));
        Money realizedGains = new Money(new BigDecimal("1000.00"), testCurrency);
        Money unrealizedGains = new Money(new BigDecimal("500.00"), testCurrency);
        Percentage timeWeightedReturn = new Percentage(new BigDecimal("14.80"));
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(transactionRepository.findByDateRange(
            eq(portfolioId),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(transactions);
        
        when(performanceCalculationService.calculateTotalReturn(portfolio, marketDataService))
            .thenReturn(totalReturn);
        when(performanceCalculationService.calculateRealizedGains(portfolio, transactions))
            .thenReturn(realizedGains);
        when(performanceCalculationService.calculateUnrealizedGains(portfolio, marketDataService))
            .thenReturn(unrealizedGains);
        when(performanceCalculationService.calculateTimeWeightedReturn(portfolio))
            .thenReturn(timeWeightedReturn);
        
        // Act
        PerformanceResponse response = queryService.getPortfolioPerformance(query);
        
        // Assert
        assertNotNull(response);
        assertEquals(totalReturn, response.totalReturns());
        assertEquals(realizedGains, response.realizedGains());
        assertEquals(unrealizedGains, response.unrealizedGains());
        assertEquals(timeWeightedReturn, response.timeWeightedReturn());
        assertNotNull(response.annualizedReturn());
        assertNotNull(response.period());
        
        verify(portfolioRepository).findByUserId(userId);
        verify(transactionRepository).findByDateRange(any(), any(), any(), any());
    }
    
    @Test
    void getPortfolioPerformance_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        ViewPerformanceQuery query = new ViewPerformanceQuery(
            userId, 
            accountId,
            Instant.now().minusSeconds(86400), 
            Instant.now()
        );
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
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
            userId, 
            AllocationType.BY_TYPE,
            time
        );
        
        Map<AssetType, Money> allocationMap = Map.of(
            AssetType.STOCK, new Money(new BigDecimal("60000.00"), testCurrency),
            AssetType.BOND, new Money(new BigDecimal("30000.00"), testCurrency),
            AssetType.CASH, new Money(new BigDecimal("10000.00"), testCurrency)
        );
        
        Money totalValue = new Money(new BigDecimal("100000.00"), testCurrency);
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(assetAllocationService.calculateAllocationByType(portfolio, marketDataService, time))
            .thenReturn(allocationMap);
        when(portfolioValuationService.calculateTotalValue(portfolio, marketDataService, time))
            .thenReturn(totalValue);
        
        // Act
        AllocationResponse response = queryService.getAssetAllocation(query);
        
        // Assert
        assertNotNull(response);
        assertEquals(time, response.getAsOfDate());
        // Verify the allocations are present and have correct percentages
        assertNotNull(response.getAllocations());
        assertEquals(3, response.getAllocations().size());
        
        verify(portfolioRepository).findByUserId(userId);
        verify(assetAllocationService).calculateAllocationByType(portfolio, marketDataService, time);
        verify(portfolioValuationService, times(1)).calculateTotalValue(portfolio, marketDataService, time);
    }
    
    @Test
    void getAssetAllocation_ByAccount_Success() {
        // Arrange
        AnalyzeAllocationQuery query = new AnalyzeAllocationQuery(
            userId, 
            AllocationType.BY_ACCOUNT,
            time
        );
        
        Map<AccountType, Money> allocationMap = Map.of(
            AccountType.TFSA, new Money(new BigDecimal("40.00"), ValidatedCurrency.USD),
            AccountType.RRSP, new Money(new BigDecimal("60.00"), ValidatedCurrency.USD)
        );
        
        Money totalValue = new Money(new BigDecimal("100000.00"), testCurrency);
        // AllocationResponse expectedResponse = mock(AllocationResponse.class);
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(assetAllocationService.calculateAllocationByAccount(portfolio, marketDataService, time))
            .thenReturn(allocationMap);
        when(portfolioValuationService.calculateTotalValue(portfolio, marketDataService, time))
            .thenReturn(totalValue);
        // when(AllocationMapper.toResponse(anyMap(), eq(totalValue), eq(time)))
        //     .thenReturn(expectedResponse);
        
        // Act
        AllocationResponse response = queryService.getAssetAllocation(query);
    
        // Assert
        assertNotNull(response);
        assertNotNull(response.getAllocations());
        assertEquals(2, response.getAllocations().size());
        
        // Verify specific allocations
        AllocationDetail tfsaDetail = response.getAllocations().get("TFSA");
        assertNotNull(tfsaDetail);
        assertEquals("TFSA", tfsaDetail.getCategory());
        assertEquals("Account Type", tfsaDetail.getCategoryType());
        
        verify(assetAllocationService).calculateAllocationByAccount(portfolio, marketDataService, time);
    }
    
    @Test
    void getAssetAllocation_ByCurrency_Success() {
        // Arrange
        AnalyzeAllocationQuery query = new AnalyzeAllocationQuery(
            userId, 
            AllocationType.BY_CURRENCY,
            time
        );
        
        // All values should be in the base currency (USD) for percentage calculations
        Map<ValidatedCurrency, Money> allocationMap = Map.of(
            ValidatedCurrency.USD, new Money(new BigDecimal("70000.00"), ValidatedCurrency.USD),
            ValidatedCurrency.CAD, new Money(new BigDecimal("30000.00"), ValidatedCurrency.USD)
        );
        
        Money totalValue = new Money(new BigDecimal("100000.00"), testCurrency);
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(assetAllocationService.calculateAllocationByCurrency(portfolio, marketDataService, time))
            .thenReturn(allocationMap);
        when(portfolioValuationService.calculateTotalValue(portfolio, marketDataService, time))
            .thenReturn(totalValue);
        
        // Act
        AllocationResponse response = queryService.getAssetAllocation(query);
        
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
        
        verify(assetAllocationService).calculateAllocationByCurrency(portfolio, marketDataService, time);
    }
    
    @Test
    void getAssetAllocation_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        AnalyzeAllocationQuery query = new AnalyzeAllocationQuery(
            userId, 
            AllocationType.BY_TYPE,
            time
        );
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(PortfolioNotFoundException.class, 
            () -> queryService.getAssetAllocation(query));
    }
    
    // ==================== getTransactionHistory Tests ====================
    
    @Test
    void getTransactionHistory_Success_WithDateRange() {
        // Arrange
        Instant startDate = Instant.now().minusSeconds(86400 * 30);
        Instant endDate = Instant.now();
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
            userId, accountId, startDate, endDate, null, 1, 10
        );
        
        List<Transaction> transactions = createMockTransactions(5);
        if (transactions.isEmpty()) {
            fail("Something is wrong, can't have empty transactions for the next step");
        }

        Page<Transaction> transactionPage = new PageImpl<>(
            transactions, 
            PageRequest.of(0, 10), 
            5
        );
                
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        
        // Since accountId is NOT null, stub findByAccountIdAndFilters instead!
        when(transactionRepository.findByAccountIdAndFilters(
            eq(accountId),
            isNull(),  // transactionType is null
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class)
        )).thenReturn(transactionPage);
        
        // when(TransactionMapper.toResponseList(anyList())).thenReturn(transactionResponses);
        
        // Act
        TransactionHistoryResponse response = queryService.getTransactionHistory(query);
        
        // Assert
        assertNotNull(response);
        assertEquals(5, response.totalCount());
        assertEquals(1, response.pageNumber());
        assertEquals(10, response.pageSize());
        assertNotNull(response.dateRange());
        
        verify(portfolioRepository).findByUserId(userId);
        verify(transactionRepository).findByAccountIdAndFilters(
            eq(accountId),
            isNull(),
            any(LocalDateTime.class),
            any(LocalDateTime.class),
            any(Pageable.class)
        );

    }
    
    @Test
    void getTransactionHistory_Success_WithoutDateRange() {
        // Arrange
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
            userId, null, null, null, null, 1, 10
        );
        
        List<Transaction> transactions = createMockTransactions(15);
        if (transactions.isEmpty()) {
            fail("Something is wrong, can't have empty transactions for the next step");
        }
        List<Transaction> pageTransactions = transactions.subList(0, 10);
        if (pageTransactions.isEmpty()) {
            fail("Something is wrong, can't have empty pageTransactions for the next step");
        }
        Page<Transaction> transactionPage = new PageImpl<>(pageTransactions, 
            org.springframework.data.domain.PageRequest.of(0, 10), 
            15
        );
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(transactionRepository.findByPortfolioIdAndFilters(
            eq(portfolioId), 
            isNull(),
            isNull(),
            isNull(),
            any(Pageable.class)
        )).thenReturn(transactionPage);
        // when(TransactionMapper.toResponseList(anyList())).thenReturn(transactionResponses);
        
        // Act
        TransactionHistoryResponse response = queryService.getTransactionHistory(query);
        
        // Assert
        assertNotNull(response);
        assertEquals(15, response.totalCount());
        assertEquals("All time", response.dateRange());
        
        verify(transactionRepository).findByPortfolioIdAndFilters(any(), any(), any(), any(), any());
    }
    
    @Test
    void getTransactionHistory_Success_WithAccountFilter() {
        // Arrange
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
            userId, accountId, null, null, null, 1, 10
        );
        
        List<Transaction> transactions = createMockTransactionsWithAccount(accountId, 5);
        if (transactions.isEmpty()) {
            fail("Something is wrong, can't have empty transactions for the next step");
        }
        Page<Transaction> transactionPage = new PageImpl<>(transactions, 
            org.springframework.data.domain.PageRequest.of(0, 10), 5);
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(transactionRepository.findByAccountIdAndFilters(
            eq(accountId),
            isNull(),
            isNull(),
            isNull(),
            any(Pageable.class)
        )).thenReturn(transactionPage);
        // when(TransactionMapper.toResponseList(anyList())).thenReturn(new ArrayList<>());
        
        // Act
        TransactionHistoryResponse response = queryService.getTransactionHistory(query);
        
        // Assert
        assertNotNull(response);
        assertEquals(5, response.totalCount());
        
        // Verify that account-specific method was called (not portfolio method)
        verify(transactionRepository).findByAccountIdAndFilters(any(), any(), any(), any(), any());
        verify(transactionRepository, never()).findByPortfolioIdAndFilters(any(), any(), any(), any(), any());
    }
    
    @Test
    void getTransactionHistory_Success_WithTransactionTypeFilter() {
        // Arrange
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
            userId, null, null, null, TransactionType.BUY, 1, 10
        );
        
        List<Transaction> transactions = createMockTransactions(8);
        if (transactions.isEmpty()) {
            fail("Something is wrong, can't have empty transactions for the next step");
        }
        Page<Transaction> transactionPage = new PageImpl<>(transactions, 
            org.springframework.data.domain.PageRequest.of(0, 10), 8);
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(transactionRepository.findByPortfolioIdAndFilters(
            eq(portfolioId),
            eq(TransactionType.BUY),
            isNull(),
            isNull(),
            any(Pageable.class)
        )).thenReturn(transactionPage);
        // when(TransactionMapper.toResponseList(anyList())).thenReturn(new ArrayList<>());
        
        // Act
        TransactionHistoryResponse response = queryService.getTransactionHistory(query);
        
        // Assert
        assertNotNull(response);
        assertEquals(8, response.totalCount());
        
        verify(transactionRepository).findByPortfolioIdAndFilters(
            any(), 
            eq(TransactionType.BUY), 
            any(), 
            any(), 
            any()
        );
    }
    
    @Test
    void getTransactionHistory_Success_Pagination() {
        // Arrange
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
            userId,  null,null, null, null, 2, 5
        );
        
        List<Transaction> pageTransactions = createMockTransactions(5);
        if (pageTransactions.isEmpty()) {
            fail("Something is wrong, can't have empty pageTransactions for the next step");
        }
        Page<Transaction> transactionPage = new PageImpl<>(
            pageTransactions, 
            org.springframework.data.domain.PageRequest.of(1, 5), 
            15
        );
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(transactionRepository.findByPortfolioIdAndFilters(
            any(), any(), any(), any(), any()
        )).thenReturn(transactionPage);
        // when(TransactionMapper.toResponseList(anyList())).thenReturn(new ArrayList<>());
        
        // Act
        TransactionHistoryResponse response = queryService.getTransactionHistory(query);
        
        // Assert
        assertEquals(15, response.totalCount());
        assertEquals(2, response.pageNumber());
        assertEquals(5, response.pageSize());
    }
    
    @Test
    void getTransactionHistory_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
            userId,null, null, null,null, 1, 10
        );
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(PortfolioNotFoundException.class, 
            () -> queryService.getTransactionHistory(query));
    }
    
    // ==================== getAccountSummary Tests ====================
    
    @Test
    void getAccountSummary_Success() throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        // Arrange
        GetAccountSummaryQuery query = new GetAccountSummaryQuery(userId, accountId);
        
        Account account = createMockAccount(accountId);
        AccountResponse expectedResponse = mock(AccountResponse.class);
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolio.getAccount(accountId)).thenReturn(account);
        when(portfolioMapper.toAccountResponse(account, marketDataService))
            .thenReturn(expectedResponse);
        
        // Act
        AccountResponse response = queryService.getAccountSummary(query);
        
        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        
        verify(portfolio).getAccount(accountId);
        verify(portfolioMapper).toAccountResponse(account, marketDataService);
    }
    
    @Test
    void getAccountSummary_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        GetAccountSummaryQuery query = new GetAccountSummaryQuery(userId, accountId);
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(PortfolioNotFoundException.class, 
            () -> queryService.getAccountSummary(query));
    }
    
    // ==================== getPortfolioSummary Tests ====================
    
    @Test
    void getPortfolioSummary_Success() {
        // Arrange
        GetPortfolioSummaryQuery query = new GetPortfolioSummaryQuery(userId);
        PortfolioResponse expectedResponse = mock(PortfolioResponse.class);
        
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.of(portfolio));
        when(portfolioMapper.toResponse(portfolio, marketDataService))
            .thenReturn(expectedResponse);
        
        // Act
        PortfolioResponse response = queryService.getPortfolioSummary(query);
        
        // Assert
        assertNotNull(response);
        assertEquals(expectedResponse, response);
        
        verify(portfolioMapper).toResponse(portfolio, marketDataService);
    }
    
    @Test
    void getPortfolioSummary_ThrowsException_WhenPortfolioNotFound() {
        // Arrange
        GetPortfolioSummaryQuery query = new GetPortfolioSummaryQuery(userId);
        when(portfolioRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(PortfolioNotFoundException.class, 
            () -> queryService.getPortfolioSummary(query));
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
                "Test transaction " + i
            );
            transactions.add(transaction);
        }
        return transactions;
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

    private Account createMockAccount(AccountId accountId) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        AssetIdentifier assetIdentifier = new MarketIdentifier("AAPL", null, AssetType.STOCK, "APPLE", "USD", null);

        // 1. Get the constructor for Asset
        // You must match the parameter types exactly as defined in the Asset class
        Constructor<Asset> constructor = Asset.class.getDeclaredConstructor(
            AssetId.class, 
            AssetIdentifier.class, 
            BigDecimal.class, 
            Money.class, 
            Instant.class
        );

        // 2. Make the constructor accessible
        constructor.setAccessible(true);

        // 3. Instantiate the Asset
        Asset asset = constructor.newInstance(
            AssetId.randomId(),
            assetIdentifier,
            new BigDecimal("10"),
            Money.of(1500, "USD"),
            Instant.now()
        );

        Account account = new Account(accountId, "Test Name", AccountType.INVESTMENT, ValidatedCurrency.USD);

        Method addMethod = account.getClass().getDeclaredMethod("addAsset", Asset.class);
        addMethod.setAccessible(true);

        // WRONG: addMethod.invoke(addMethod, asset); 
        // RIGHT: Pass the 'account' instance as the first argument
        addMethod.invoke(account, asset);
        
        return account;
    }
}