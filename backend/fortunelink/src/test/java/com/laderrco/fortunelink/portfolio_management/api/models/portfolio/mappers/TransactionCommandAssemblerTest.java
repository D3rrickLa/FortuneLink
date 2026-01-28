package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.FeeRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.RecordTransactionRequest;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeleteTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdateTransactionCommand;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetTransactionHistoryQuery;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.FeeType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.shared.enums.Precision;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;

class TransactionCommandAssemblerTest {

    private ExchangeRateService exchangeRateService;
    private TransactionCommandAssembler assembler;

    @BeforeEach
    void setUp() {
        exchangeRateService = mock(ExchangeRateService.class);
        assembler = new TransactionCommandAssembler(exchangeRateService);
    }

    @Test
    @DisplayName("toPurchaseCommand maps correctly with fees")
    void toPurchaseCommand_mapsCorrectly() {
        String portfolioId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();

        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setSymbol("AAPL");
        req.setQuantity(BigDecimal.TEN);
        req.setPrice(BigDecimal.valueOf(100));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());
        req.setNotes("Note");
        req.setFees(List.of(new FeeRequest(FeeType.BROKERAGE, BigDecimal.valueOf(5), "USD", null, null)));

        when(exchangeRateService.getExchangeRate(any(), any()))
                .thenReturn(Optional.of(new ExchangeRate(ValidatedCurrency.USD, ValidatedCurrency.USD, BigDecimal.ONE,
                        Instant.now(), "desc")));

        RecordPurchaseCommand cmd = assembler.toPurchaseCommand(portfolioId, accountId, ValidatedCurrency.USD, req);

        assertEquals(portfolioId, cmd.portfolioId().portfolioId().toString());
        assertEquals(accountId, cmd.accountId().accountId().toString());
        assertEquals("AAPL", cmd.symbol());
        assertEquals(BigDecimal.TEN, cmd.quantity());
        assertEquals(BigDecimal.valueOf(100).setScale(Precision.getMoneyPrecision()), cmd.price().amount());
        assertEquals(1, cmd.fees().size());
        assertEquals("Note", cmd.notes());
    }

    @Test
    @DisplayName("toPurchaseCommand maps correctly with fees")
    void toPurchaseCommand_mapsCorrectlyEmptyFees() {
        String portfolioId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();

        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setSymbol("AAPL");
        req.setQuantity(BigDecimal.TEN);
        req.setPrice(BigDecimal.valueOf(100));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());
        req.setNotes("Note");
        req.setFees(List.of());

        when(exchangeRateService.getExchangeRate(any(), any()))
                .thenReturn(Optional.of(new ExchangeRate(ValidatedCurrency.USD, ValidatedCurrency.USD, BigDecimal.ONE,
                        Instant.now(), "desc")));

        RecordPurchaseCommand cmd = assembler.toPurchaseCommand(portfolioId, accountId, ValidatedCurrency.USD, req);

        assertEquals(portfolioId, cmd.portfolioId().portfolioId().toString());
        assertEquals(accountId, cmd.accountId().accountId().toString());
        assertEquals("AAPL", cmd.symbol());
        assertEquals(BigDecimal.TEN, cmd.quantity());
        assertEquals(BigDecimal.valueOf(100).setScale(Precision.getMoneyPrecision()), cmd.price().amount());
        assertEquals("Note", cmd.notes());
    }

    @Test
    @DisplayName("toPurchaseCommand maps correctly with fees")
    void toPurchaseCommand_mapsCorrectlyEmptyNull() {
        String portfolioId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();

        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setSymbol("AAPL");
        req.setQuantity(BigDecimal.TEN);
        req.setPrice(BigDecimal.valueOf(100));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());
        req.setNotes("Note");
        req.setFees(null);

        when(exchangeRateService.getExchangeRate(any(), any()))
                .thenReturn(Optional.of(new ExchangeRate(ValidatedCurrency.USD, ValidatedCurrency.USD, BigDecimal.ONE,
                        Instant.now(), "desc")));

        RecordPurchaseCommand cmd = assembler.toPurchaseCommand(portfolioId, accountId, ValidatedCurrency.USD, req);

        assertEquals(portfolioId, cmd.portfolioId().portfolioId().toString());
        assertEquals(accountId, cmd.accountId().accountId().toString());
        assertEquals("AAPL", cmd.symbol());
        assertEquals(BigDecimal.TEN, cmd.quantity());
        assertEquals(BigDecimal.valueOf(100).setScale(Precision.getMoneyPrecision()), cmd.price().amount());
        assertEquals("Note", cmd.notes());
    }

    @Test
    @DisplayName("toSaleCommand throws if AssetId missing")
    void toSaleCommand_missingAssetId_throws() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setQuantity(BigDecimal.ONE);
        req.setPrice(BigDecimal.valueOf(50));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());

        assertThrows(IllegalArgumentException.class,
                () -> assembler.toSaleCommand(PortfolioId.randomId().portfolioId().toString(),
                        AccountId.randomId().accountId().toString(), ValidatedCurrency.USD, req));
    }

    @Test
    @DisplayName("toSaleCommand maps correctly when AssetId provided")
    void toSaleCommand_mapsCorrectly() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setAssetId(UUID.randomUUID().toString());
        req.setQuantity(BigDecimal.ONE);
        req.setPrice(BigDecimal.valueOf(50));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());
        req.setFees(List.of(new FeeRequest(FeeType.BROKERAGE, BigDecimal.valueOf(5), "USD", LocalDateTime.now(), Map.of("Sector", "Tech"))));

        when(exchangeRateService.getExchangeRate(any(), any()))
                .thenReturn(Optional.of(new ExchangeRate(ValidatedCurrency.USD, ValidatedCurrency.USD, BigDecimal.ONE,
                        Instant.now(), "desc")));

        RecordSaleCommand cmd = assembler.toSaleCommand(PortfolioId.randomId().portfolioId().toString(),
                AccountId.randomId().accountId().toString(), ValidatedCurrency.USD, req);
        assertEquals(req.getAssetId(), cmd.assetId().assetId().toString());
    }

    @Test
    @DisplayName("toSaleCommand maps correctly when AssetId provided")
    void toSaleCommand_mapsCorrectlyExchangeRateError() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setAssetId(UUID.randomUUID().toString());
        req.setQuantity(BigDecimal.ONE);
        req.setPrice(BigDecimal.valueOf(50));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());
        req.setFees(List.of(new FeeRequest(FeeType.BROKERAGE, BigDecimal.valueOf(5), "USD", LocalDateTime.now(), Map.of("Sector", "Tech"))));

        when(exchangeRateService.getExchangeRate(any(), any())).thenReturn(Optional.empty());

        assertThrows( IllegalArgumentException.class,() -> assembler.toSaleCommand(PortfolioId.randomId().portfolioId().toString(),
                AccountId.randomId().accountId().toString(), ValidatedCurrency.USD, req));
    }

    @Test
    @DisplayName("toDividendCommand maps correctly including DRIP")
    void toDividendCommand_mapsCorrectly() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setAssetId(UUID.randomUUID().toString());
        req.setPrice(BigDecimal.valueOf(10));
        req.setPriceCurrency("USD");
        req.setTransactionType("DIVIDEND");
        req.setIsDrip(true);
        req.setSharesReceived(BigDecimal.TEN);
        req.setTransactionDate(LocalDateTime.now());

        RecordIncomeCommand cmd = assembler.toDividendCommand(PortfolioId.randomId().portfolioId().toString(),
                AccountId.randomId().accountId().toString(), ValidatedCurrency.USD, req);
        assertEquals("DIVIDEND", cmd.type().name());
        assertTrue(cmd.isDrip());
        assertEquals(BigDecimal.TEN, cmd.sharesReceived());
    }

    @Test
    @DisplayName("toDepositCommand maps correctly")
    void toDepositCommand_mapsCorrectly() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setPrice(BigDecimal.valueOf(1000));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());

        when(exchangeRateService.getExchangeRate(any(), any()))
                .thenReturn(Optional.of(new ExchangeRate(ValidatedCurrency.USD, ValidatedCurrency.USD, BigDecimal.ONE,
                        Instant.now(), "desc")));

        RecordDepositCommand cmd = assembler.toDepositCommand(PortfolioId.randomId().portfolioId().toString(),
                AccountId.randomId().accountId().toString(), ValidatedCurrency.USD, req);
        assertEquals(BigDecimal.valueOf(1000).setScale(Precision.getMoneyPrecision()), cmd.amount().amount());
    }

    @Test
    @DisplayName("toWithdrawalCommand maps correctly")
    void toWithdrawalCommand_mapsCorrectly() {
        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setPrice(BigDecimal.valueOf(200));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());

        when(exchangeRateService.getExchangeRate(any(), any()))
                .thenReturn(Optional.of(new ExchangeRate(ValidatedCurrency.USD, ValidatedCurrency.USD, BigDecimal.ONE,
                        Instant.now(), "desc")));

        RecordWithdrawalCommand cmd = assembler.toWithdrawalCommand(PortfolioId.randomId().portfolioId().toString(),
                AccountId.randomId().accountId().toString(), ValidatedCurrency.USD, req);
        assertEquals(BigDecimal.valueOf(200).setScale(Precision.getMoneyPrecision()), cmd.amount().amount());
    }

    @Test
    @DisplayName("toUpdateCommand maps correctly for asset transaction")
    void toUpdateCommand_assetTransaction() {
        // Prepare a fixed set of UUIDs
        String portfolioId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();

        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setSymbol("AAPL");
        req.setTransactionType("BUY");
        req.setQuantity(BigDecimal.TEN);
        req.setPrice(BigDecimal.valueOf(100));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());
        // req.setIsAssetTransaction(true); // important to trigger assetIdentifier
        // branch

        // Mock exchange rate service for fees (even if empty, assembler uses it)
        when(exchangeRateService.getExchangeRate(any(), any()))
                .thenReturn(Optional.of(new ExchangeRate(
                        ValidatedCurrency.USD,
                        ValidatedCurrency.USD,
                        BigDecimal.ONE,
                        Instant.now(),
                        "desc")));

        UpdateTransactionCommand cmd = assembler.toUpdateCommand(
                portfolioId,
                accountId,
                transactionId,
                ValidatedCurrency.USD,
                req);

        // Assertions
        // assertNotNull(cmd.assetIdentifier(), "AssetIdentifier should be populated for
        // asset transaction");
        assertEquals("BUY", cmd.type().name());
        assertEquals(BigDecimal.TEN, cmd.quantity());
        assertEquals(portfolioId, cmd.portfolioId().portfolioId().toString());
        assertEquals(accountId, cmd.accountId().accountId().toString());
        assertEquals(transactionId, cmd.transactionId().transactionId().toString());
    }

    @Test
    @DisplayName("toUpdateCommand maps correctly for asset transaction")
    void toUpdateCommand_assetTransactionIsAssetTransactionFalse() {
        // Prepare a fixed set of UUIDs
        String portfolioId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();

        RecordTransactionRequest req = new RecordTransactionRequest();
        req.setSymbol("AAPL");
        req.setTransactionType("WITHDRAWAL");
        req.setQuantity(BigDecimal.TEN);
        req.setPrice(BigDecimal.valueOf(100));
        req.setPriceCurrency("USD");
        req.setTransactionDate(LocalDateTime.now());
        // req.setIsAssetTransaction(true); // important to trigger assetIdentifier
        // branch

        // Mock exchange rate service for fees (even if empty, assembler uses it)
        when(exchangeRateService.getExchangeRate(any(), any()))
                .thenReturn(Optional.of(new ExchangeRate(
                        ValidatedCurrency.USD,
                        ValidatedCurrency.USD,
                        BigDecimal.ONE,
                        Instant.now(),
                        "desc")));

        UpdateTransactionCommand cmd = assembler.toUpdateCommand(
                portfolioId,
                accountId,
                transactionId,
                ValidatedCurrency.USD,
                req);

        // Assertions
        // assertNotNull(cmd.assetIdentifier(), "AssetIdentifier should be populated for
        // asset transaction");
        assertEquals("WITHDRAWAL", cmd.type().name());
        assertEquals(BigDecimal.TEN, cmd.quantity());
        assertEquals(portfolioId, cmd.portfolioId().portfolioId().toString());
        assertEquals(accountId, cmd.accountId().accountId().toString());
        assertEquals(transactionId, cmd.transactionId().transactionId().toString());
    }

    @Test
    @DisplayName("toDeleteCommand maps correctly")
    void toDeleteCommand_mapsCorrectly() {
        String portfolioId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();

        DeleteTransactionCommand cmd = assembler.toDeleteCommand(portfolioId, accountId, transactionId, true, "notes");

        assertEquals(transactionId, cmd.transactionId().transactionId().toString());
        assertTrue(cmd.softDelete());
        assertEquals("notes", cmd.reason());
    }

    @Test
    @DisplayName("toHistoryQuery maps correctly")
    void toHistoryQuery_mapsCorrectly() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(3600);
        GetTransactionHistoryQuery query = assembler.toHistoryQuery(PortfolioId.randomId().portfolioId().toString(),
                AccountId.randomId().accountId().toString(), "BUY", start, end, 1, 20);
        assertEquals("BUY", query.transactionType().name());
        assertEquals(1, query.pageNumber());
        assertEquals(20, query.pageSize());
    }

    @Test
    @DisplayName("toTransactionQuery maps correctly")
    void toTransactionQuery_mapsCorrectly() {
        // Generate fixed UUIDs for consistent testing
        String portfolioId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();
        String transactionId = UUID.randomUUID().toString();

        GetTransactionByIdQuery query = assembler.toTransactionQuery(portfolioId, accountId, transactionId);

        // Assertions
        assertEquals(transactionId, query.transactionId().transactionId().toString());
        assertEquals(portfolioId, query.portfolioId().portfolioId().toString());
        assertEquals(accountId, query.accountId().accountId().toString());
    }

}
