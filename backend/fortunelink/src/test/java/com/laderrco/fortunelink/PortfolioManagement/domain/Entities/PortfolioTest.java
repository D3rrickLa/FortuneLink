package com.laderrco.fortunelink.PortfolioManagement.domain.Entities;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hamcrest.number.IsNaN;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.core.TransactionState;

import com.laderrco.fortunelink.portfoliomanagement.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagement.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagement.domain.services.SimpleExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

public class PortfolioTest {

    private Portfolio portfolio;
    private UUID portfolioId;
    private UUID userId;
    private String name;
    private String desc;
    private PortfolioCurrency portfolioCurrency;
    private Money portfolioCashBalance;
    private Instant createdAt;
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void init() {
        portfolioId = UUID.randomUUID();
        userId = UUID.randomUUID();
        name = "Portfolio Name";
        desc = "Portfolio desc";
        portfolioCurrency = new PortfolioCurrency(Currency.getInstance("USD"));
        portfolioCashBalance = new Money(new BigDecimal(1000), portfolioCurrency);
        createdAt = Instant.now();
        exchangeRateService = new SimpleExchangeRateService();
        portfolio = new Portfolio(portfolioId, userId, name, desc, portfolioCurrency, portfolioCashBalance, exchangeRateService, createdAt);
        new Portfolio(portfolioId, userId, name, null, portfolioCurrency, portfolioCashBalance, exchangeRateService, createdAt);
    }

    @Test
    void testConstructor() {
        PortfolioCurrency portfolioCurrency1 = new PortfolioCurrency(Currency.getInstance("CAD"));
        Money portfolioCashBalanace1 = new Money(new BigDecimal(-1), portfolioCurrency);
        Money portfolioCashBalanace2 = new Money(new BigDecimal(1000), portfolioCurrency1);

        // null pointer check for empty id
        assertThrows(NullPointerException.class,
                () -> new Portfolio(null, userId, name, desc, portfolioCurrency, portfolioCashBalance,exchangeRateService, createdAt));

        // when name is blank (i.e. \n\n\n)
        assertThrows(IllegalArgumentException.class, () -> new Portfolio(portfolioId, userId, "   \r \n   ", desc,
                portfolioCurrency1, portfolioCashBalanace1, exchangeRateService, createdAt));
        // when portfolio cash is negative, on creation
        assertThrows(IllegalArgumentException.class, () -> new Portfolio(portfolioId, userId, name, desc,
                portfolioCurrency1, portfolioCashBalanace1, exchangeRateService, createdAt));

        // when you put in cash that isn't your native currency
        assertThrows(IllegalArgumentException.class, () -> new Portfolio(portfolioId, userId, name, desc,
                portfolioCurrency, portfolioCashBalanace2, exchangeRateService, createdAt));

        assertNotNull(portfolio);
        assertEquals(portfolioId, portfolio.getPortfolioId());
        assertEquals(userId, portfolio.getUserId());
        assertEquals(name, portfolio.getPortfolioName());
        assertEquals(desc, portfolio.getPortfolioDescription());
        assertEquals(portfolioCurrency, portfolio.getPortfolioCurrencyPreference());
        assertEquals(portfolioCashBalance, portfolio.getPortfolioCashBalance());
        assertEquals(createdAt, portfolio.getCreatedAt());
        assertNotNull(portfolio.getUpdatedAt());
        // createdAt and updatedAt will be slightly different because updatedAt uses
        // Instant.now()
        // assert that updatedAt is at or after createdAt
        assertTrue(portfolio.getUpdatedAt().equals(createdAt) || portfolio.getUpdatedAt().isAfter(createdAt));

        assertTrue(portfolio.getTransactions().isEmpty());
        assertTrue(portfolio.getAssetHoldings().isEmpty());
        assertTrue(portfolio.getLiabilities().isEmpty());

    }

    @Test
    void testRecordAssetHoldingPurchase() {
        TransactionType transactionType = TransactionType.DEPOSIT;
        Money cashflowAmount = new Money(new BigDecimal(200), portfolioCurrency);
        Money cashflowAmountOther = new Money(new BigDecimal(200), new PortfolioCurrency(Currency.getInstance("CAD")));
        Instant cashflowEventDate = Instant.now();

        TransactionStatus status = TransactionStatus.COMPLETED;
        TransactionSource source = TransactionSource.MANUAL_INPUT;
        String description = "SOME METADATA";
        TransactionMetadata transactionMetadata = new TransactionMetadata(status, source, description, cashflowEventDate, cashflowEventDate);
        
        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.DEPOSIT_FEE, new Money(new BigDecimal(1), portfolioCurrency)));
        fees.add(new Fee(FeeType.FOREIGN_EXCHANGE_CONVERSION, new Money(new BigDecimal(2), new PortfolioCurrency(Currency.getInstance("CAD")))));


        // assert fails nulls
        Exception e1 = assertThrows(NullPointerException.class, () ->  portfolio.recordCashflow(null, cashflowAmount, cashflowEventDate, transactionMetadata, fees));
        Exception e2 = assertThrows(NullPointerException.class, () ->  portfolio.recordCashflow(transactionType, null, cashflowEventDate, transactionMetadata, fees));
        Exception e3 = assertThrows(NullPointerException.class, () ->  portfolio.recordCashflow(transactionType, cashflowAmount, null, transactionMetadata, fees));
        Exception e4 = assertThrows(NullPointerException.class, () ->  portfolio.recordCashflow(transactionType, cashflowAmount, cashflowEventDate, null, fees));

        assertTrue(e1.getMessage().equals("Transaction type cannot be null."));
        assertTrue(e2.getMessage().equals("Amount of cash being put in/ taken out cannot be null."));
        assertTrue(e3.getMessage().equals("Cash transaction date cannot be null."));
        assertTrue(e4.getMessage().equals("Transaction metadata cannot be null."));
        
        // assert fails, cashflow amount cannot be less than zero
        Money cashflowAmountNegative = new Money(new BigDecimal(-200), portfolioCurrency);
        Exception e5 = assertThrows(IllegalArgumentException.class, () ->  portfolio.recordCashflow(transactionType, cashflowAmountNegative, cashflowEventDate, transactionMetadata, fees));
        assertTrue(e5.getMessage().equals("Cash amount for " + transactionType + " cannot less than or equal to zero."));
        
        Money cashflowAmountZero = new Money(new BigDecimal(0), portfolioCurrency);
        Exception e6 = assertThrows(IllegalArgumentException.class, () ->  portfolio.recordCashflow(transactionType, cashflowAmountZero, cashflowEventDate, transactionMetadata, fees));
        assertTrue(e6.getMessage().equals("Cash amount for " + transactionType + " cannot less than or equal to zero."));
        
        //assert fails, type must be of the valid types
        Exception e7 = assertThrows(IllegalArgumentException.class, () ->  portfolio.recordCashflow(TransactionType.CORPORATE_ACTION, cashflowAmount, cashflowEventDate, transactionMetadata, fees));
        assertTrue(e7.getMessage().equals("Transaction Type must be either DEPOSIT, WITHDRAWAL, INTEREST, EXPENSE, FEE, or DIVIDEND."));
        
        // assert fails, must have same currency as preference
        // Money cashflowAmountWrongPref = new Money(new BigDecimal(200), new PortfolioCurrency(Currency.getInstance("CAD")));
        // Exception e8 = assertThrows(IllegalArgumentException.class, () ->  portfolio.recordCashflow(transactionType, cashflowAmountWrongPref, cashflowEventDate, transactionMetadata, fees));
        // assertTrue(e8.getMessage().equals("Cash must have the same currency preference as the portfolio."));
        
        
        //assert fails, must have competed status
        TransactionMetadata transactionMetadataFAILED = new TransactionMetadata(TransactionStatus.FAILED, source, description, cashflowEventDate, cashflowEventDate);
        Exception e9 = assertThrows(IllegalArgumentException.class, () ->  portfolio.recordCashflow(transactionType, cashflowAmount, cashflowEventDate, transactionMetadataFAILED, fees));
        assertTrue(e9.getMessage().equals("Status in metadata must be COMPLETED."));
        
        // assert fails, cash withdrawl is too large
        Money cashflowAmountLargerThanCashBal = new Money(new BigDecimal(200000), new PortfolioCurrency(Currency.getInstance("USD")));
        Exception e10 = assertThrows(IllegalArgumentException.class, () ->  portfolio.recordCashflow(TransactionType.WITHDRAWAL, cashflowAmountLargerThanCashBal, cashflowEventDate, transactionMetadata, fees));
        assertTrue(e10.getMessage().equals("Cash withdrawal is larger than what you have in this portfolio."));
        
        // asset fails, fees no the same currency as portfolio pref
        // List<Fee> feesWrongCur = new ArrayList<>();
        // feesWrongCur.add(new Fee(FeeType.DEPOSIT_FEE, new Money(new BigDecimal(1),  new PortfolioCurrency(Currency.getInstance("CAD")))));
        // feesWrongCur.add(new Fee(FeeType.DEPOSIT_FEE, new Money(new BigDecimal(1),  new PortfolioCurrency(Currency.getInstance("CAD")))));
        // Exception e11 = assertThrows(IllegalArgumentException.class, () ->  portfolio.recordCashflow(TransactionType.WITHDRAWAL, cashflowAmountLargerThanCashBal, cashflowEventDate, transactionMetadata, feesWrongCur));
        // assertTrue(e11.getMessage().equals("Error all your fees must be in the same currency as the portfolio currency preference."));
        
        

        // good assertion, need to see if the fee is og + amount deposited/withdrawn 
        portfolio.recordCashflow(transactionType, cashflowAmount, cashflowEventDate, transactionMetadata, fees);
        portfolio.recordCashflow(TransactionType.WITHDRAWAL, cashflowAmount, cashflowEventDate, transactionMetadata, null);
        portfolio.recordCashflow(TransactionType.WITHDRAWAL, cashflowAmountOther, cashflowEventDate, transactionMetadata, null);

        assertEquals(3, portfolio.getTransactions().size());
        assertEquals(new BigDecimal(849.37).setScale(2, RoundingMode.HALF_UP), portfolio.getPortfolioCashBalance().amount());
        assertTrue(portfolio.getUpdatedAt().isAfter(portfolio.getCreatedAt()));
    }

    @Test
    void testRecordAssetHoldingSale() {
        
    }

    @Test
    void testRecordCashflow() {
        
    }

    @Test
    void testRecordLiabilityPayment() {
        
    }

    @Test
    void testRecordNewLiability() {
        
    }

    @Test
    void testVoidTransaction() {
        
    }

}
