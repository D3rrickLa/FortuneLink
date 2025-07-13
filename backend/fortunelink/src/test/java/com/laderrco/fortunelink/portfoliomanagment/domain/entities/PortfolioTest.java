package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.SimpleExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.FeeType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.CashflowTransactionDetails;
import com.laderrco.fortunelink.shared.valueobjects.ExchangeRate;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class PortfolioTest {
    private Portfolio portfolio;
    private UUID userId;
    private UUID portfolioId;
    private String name;
    private String desc;
    private Money portfolioCashBalance; 
    private Currency cad;
    private Currency usd;
    private ExchangeRateService exchangeRateService; 

    @BeforeEach
    void init() {
        userId = UUID.randomUUID();
        portfolioId = UUID.randomUUID();
        name = "Portfolio name ";
        desc = "some desc";
        
        cad = Currency.getInstance("CAD");
        usd = Currency.getInstance("USD");

        portfolioCashBalance = new Money(12000, cad);
        exchangeRateService = new SimpleExchangeRateService();

        portfolio = new Portfolio(
            portfolioId, userId, name, desc, portfolioCashBalance, cad, exchangeRateService);
    }

    @Test
    void testRecordCashflow() {
        // transaction details setup
        Money orignalMoney = new Money(2000, cad);
        Money convertedMoney = orignalMoney;
        Money totalConversionFee = new Money(0, cad);
        ExchangeRate exchangeRate = null;

        CashflowTransactionDetails cashflowTransactionDetails = new CashflowTransactionDetails(orignalMoney, convertedMoney, totalConversionFee, exchangeRate);

        // common transaction setup
        List<Fee> fees = new ArrayList<>();
        fees.add(new Fee(FeeType.DEPOSIT_FEE, new Money(0.05, cad)));
        TransactionMetadata transactionMetadata = new TransactionMetadata(
            TransactionStatus.COMPLETED, 
            TransactionSource.MANUAL_INPUT, 
            "DEPOSITED MONEY", Instant.now(), 
            Instant.now()
        );

        UUID correlationId = UUID.randomUUID();
        UUID parentId = null;
        TransactionType transactionType = TransactionType.DEPOSIT;

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(correlationId, parentId, transactionType, transactionMetadata, fees);

        portfolio.recordCashflow(cashflowTransactionDetails, commonTransactionInput, Instant.now());
    
        assertEquals(new Money(13999.95, cad), portfolio.getPortfolioCashBalance());
        assertEquals(1, portfolio.getFees().size());
    }
}
