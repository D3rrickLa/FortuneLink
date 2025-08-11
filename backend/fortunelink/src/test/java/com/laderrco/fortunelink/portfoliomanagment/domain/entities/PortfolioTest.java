package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
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

public class PortfolioTest {
    private Currency USD = Currency.getInstance("USD");
    // tests for liability
    private Portfolio createTestPortfolio() {
        return new Portfolio(
                User.createNew(new UserId(UUID.randomUUID()), "NAME", USD),
                "Test Portfolio",
                "Testing liabilities",
                new Money(new BigDecimal("10000"), USD),
                new SimpleCurrencyService());
    }

    private LiabilityId createAndAddLiability(Portfolio portfolio) {
        LiabilityId id = new LiabilityId(UUID.randomUUID());
        Liability liability = new Liability(
            id,
            portfolio.getPortfolioId(),
            new LiabilityDetails("Loan", "Updated Loan", LiabilityType.OTHER, new Percentage(BigDecimal.valueOf(0.05)), Instant.now()),
            new Money(new BigDecimal("1000"), USD),
            Instant.now()
        );  
        portfolio.getLiabilities().put(id, liability);
        return id;
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
        assertEquals(1, portfolio.getDomainEvents().size());
        assertEquals(1, portfolio.getLiabilities().size());
    }

    @Test
    public void testRecordLiabilityPayment_updatesCashBalanceAndCreatesTransaction() {
        Portfolio portfolio = createTestPortfolio();
        LiabilityId liabilityId = createAndAddLiability(portfolio);
        Money paymentAmount = new Money(new BigDecimal("500"), USD);
        List<Fee> fees = List.of(new Fee(FeeType.ACCOUNT_MAINTENANCE, new Money(new BigDecimal("10"), USD), "DESC", Instant.now()));
        Instant now = Instant.now();

        portfolio.recordLiabilityPayment(
                liabilityId,
                paymentAmount,
                TransactionSource.SYSTEM,
                fees,
                now);

        assertEquals(1, portfolio.getTransactions().size());
        assertEquals(1, portfolio.getDomainEvents().size());
    }

    @Test
    public void testUpdateLiability_changesDetails() {
        Portfolio portfolio = createTestPortfolio();
        LiabilityId liabilityId = createAndAddLiability(portfolio);

        LiabilityDetails newDetails = new LiabilityDetails("Loan", "Updated Loan", LiabilityType.OTHER, new Percentage(BigDecimal.valueOf(0.07)), Instant.now());
        portfolio.updateLiability(liabilityId, newDetails);

        assertEquals("Updated Loan", portfolio.getLiabilities().get(liabilityId).getDetails().description());
    }
}
