package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.LiabilityType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.services.SimpleCurrencyService;
import com.laderrco.fortunelink.portfoliomanagment.infrastructure.services.SimpleMaketDataService;

public class PortfolioTest {
    private Currency USD = Currency.getInstance("USD");
    // tests for liability
    private Portfolio createTestPortfolio() {
        return new Portfolio(
                new UserId(UUID.randomUUID()),
                "Test Portfolio",
                "Testing liabilities",
                new Money(new BigDecimal("10000"), USD),
                new SimpleCurrencyService(),
                new SimpleMaketDataService()
                );
    }

    @Test
    public void testIncurrNewLiability_createsLiabilityAndTransaction() {
        Portfolio portfolio = createTestPortfolio();
        Money liabilityAmount = new Money(new BigDecimal("1000"), USD);
        List<Fee> fees = List.of(new Fee(FeeType.REGULATORY, new Money(new BigDecimal("50"), USD), "desc", Instant.now()));
        Instant now = Instant.now();

        portfolio.incurrNewLiability(
            new LiabilityDetails("Loan", "Loan Desc", LiabilityType.OTHER, new Percentage(BigDecimal.valueOf(0.05)), Instant.now()),
                liabilityAmount,
                TransactionSource.SYSTEM,
                fees,
                now);

        assertEquals(1, portfolio.getTransactions().size());
        assertEquals(2, portfolio.getDomainEvents().size());
        assertEquals(1, portfolio.getLiabilities().size());
    }

    @Test
    public void testRecordLiabilityPayment_updatesCashBalanceAndCreatesTransaction() {
        Portfolio portfolio = createTestPortfolio();
        LiabilityDetails details = new LiabilityDetails(
            "Loan", 
            "DESC",
            LiabilityType.AUTO_LOAN,
            new Percentage(BigDecimal.valueOf(0.05)), 
            Instant.now()
            
        );
        
        // 1. Incur the liability using a public method on the Portfolio aggregate.
        LiabilityId liabilityId =   portfolio.incurrNewLiability(
            details, 
            new Money(new BigDecimal("1000"), USD), 
            TransactionSource.MANUAL,
            Collections.emptyList(),
            Instant.now()
            );
            
        Money paymentAmount = new Money(new BigDecimal("500"), USD);
        List<Fee> fees = List.of(new Fee(FeeType.ACCOUNT_MAINTENANCE, new Money(new BigDecimal("10"), USD), "DESC", Instant.now()));
        Instant now = Instant.now();
        

        // 2. Now, record the payment using another public method.
        portfolio.recordLiabilityPayment(
            liabilityId,
            paymentAmount,
            TransactionSource.SYSTEM,
            fees,
            now);

        assertEquals(2, portfolio.getTransactions().size());
        assertEquals(3, portfolio.getDomainEvents().size());
    }

    @Test 
    void test_RecordLiabilityPaymentExceptionThrowWhenCurrenyNotMatch() {
        Portfolio portfolio = createTestPortfolio();
        Money liabilityAmount = new Money(new BigDecimal("1000"), USD);
        List<Fee> fees = List.of(new Fee(FeeType.REGULATORY, new Money(new BigDecimal("50"), USD), "desc", Instant.now()));
        Instant now = Instant.now();

        LiabilityId liabilityId = portfolio.incurrNewLiability(
            new LiabilityDetails("Loan", "Loan Desc", LiabilityType.OTHER, new Percentage(BigDecimal.valueOf(0.05)), Instant.now()),
                liabilityAmount,
                TransactionSource.SYSTEM,
                fees,
                now);

        assertThrows(IllegalArgumentException.class, () -> portfolio.recordLiabilityPayment(liabilityId, Money.of(20, Currency.getInstance("CAD")), TransactionSource.SYSTEM, null, now));
    }

    @Test
    public void testUpdateLiability_changesDetails() {
        Portfolio portfolio = createTestPortfolio();
        LiabilityDetails details = new LiabilityDetails(
            "Loan", 
            "DESC",
            LiabilityType.AUTO_LOAN,
            new Percentage(BigDecimal.valueOf(0.05)), 
            Instant.now()
            
        );
        
        // 1. Incur the liability using a public method on the Portfolio aggregate.
        LiabilityId liabilityId =   portfolio.incurrNewLiability(
            details, 
            new Money(new BigDecimal("1000"), USD), 
            TransactionSource.MANUAL,
            Collections.emptyList(),
            Instant.now()
            );

        LiabilityDetails newDetails = new LiabilityDetails("Loan", "Updated Loan", LiabilityType.OTHER, new Percentage(BigDecimal.valueOf(0.07)), Instant.now());
        portfolio.updateLiability(liabilityId, newDetails);

        assertEquals("Updated Loan", portfolio.getLiabilities().get(liabilityId).getDetails().description());
    }
    
    @Test
    public void testUpdateLiability_NothingHappensWhenLiabilityDetailsIsNull() {
        Portfolio portfolio = createTestPortfolio();
        LiabilityDetails details = new LiabilityDetails(
            "Loan", 
            "DESC",
            LiabilityType.AUTO_LOAN,
            new Percentage(BigDecimal.valueOf(0.05)), 
            Instant.now()
            
        );
        
        // 1. Incur the liability using a public method on the Portfolio aggregate.
        LiabilityId liabilityId =   portfolio.incurrNewLiability(
            details, 
            new Money(new BigDecimal("1000"), USD), 
            TransactionSource.MANUAL,
            Collections.emptyList(),
            Instant.now()
            );

        // LiabilityDetails newDetails = new LiabilityDetails("Loan", "Updated Loan", LiabilityType.OTHER, new Percentage(BigDecimal.valueOf(0.07)), Instant.now());
        portfolio.updateLiability(liabilityId, null);

        assertNotEquals("Updated Loan", portfolio.getLiabilities().get(liabilityId).getDetails().description());
    }

    @Test 
    void testUpdatePortfolioDetails_Correct() {
        Portfolio portfolio = createTestPortfolio();
        portfolio.updatePortfolioDetails("someting else", "new thing");
        assertAll(
            () -> assertEquals("someting else", portfolio.getPortfolioName()),
            () ->assertEquals("new thing", portfolio.getPortfolioDescription())
        );
    }

    @Test
    void testValidatePortfolioName_Invalid() {
        Portfolio portfolio = createTestPortfolio();
        assertThrows(IllegalArgumentException.class, 
            () -> portfolio.updatePortfolioDetails("\n\n\n\n", null));
        assertThrows(IllegalArgumentException.class, 
            () -> portfolio.updatePortfolioDetails("\n\n\n\n", "null"));
        assertThrows(IllegalArgumentException.class, 
            () -> portfolio.updatePortfolioDetails("100charactersexceeded100charactersexceeded100charactersexceeded100charactersexceeded100charactersexceeded100charactersexceeded100charactersexceeded100charactersexceeded100charactersexceeded100charactersexceeded100charactersexceeded", 
            "null"));

        assertDoesNotThrow(() -> portfolio.updatePortfolioDetails(null, null));
    };

    @Test 
    void testClearDomainEvents() {
        Portfolio portfolio = createTestPortfolio();
        portfolio.clearDomainEvents();
        assertEquals(0, portfolio.getDomainEvents().size());
    }

    @Test 
    void testValidateTransactionDate() {
        Portfolio portfolio = createTestPortfolio();

      LiabilityDetails details = new LiabilityDetails(
            "Loan", 
            "DESC",
            LiabilityType.AUTO_LOAN,
            new Percentage(BigDecimal.valueOf(0.05)), 
            Instant.now()
            
        );
        
        
        assertThrows(IllegalArgumentException.class, ()-> portfolio.incurrNewLiability(
            details, 
            new Money(new BigDecimal("1000"), USD), 
            TransactionSource.MANUAL,
            Collections.emptyList(),
            Instant.now().plusSeconds(300)
            ) );
    }

    @Test
    void updateLiability_ThrowsExceptionWithUnknownLiability() {
        Portfolio portfolio = createTestPortfolio();
        LiabilityDetails details = new LiabilityDetails(
            "Loan", 
            "DESC",
            LiabilityType.AUTO_LOAN,
            new Percentage(BigDecimal.valueOf(0.05)), 
            Instant.now()
            
        );
        
        assertThrows(IllegalStateException.class, () -> portfolio.updateLiability(new LiabilityId(UUID.randomUUID()), details));
    }

    
}
