package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.SimpleExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.AssetType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.TransactionType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.LiabilityIncurrenceTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public class PortfolioTestPart2 {

    private Portfolio portfolio;
	private UUID userId;
	private UUID portfolioId;
	private String name;
	private String desc;
	private Money portfolioCashBalance;
	private Currency cad;
	private Currency usd;
	private ExchangeRateService exchangeRateService;
	private AssetIdentifier appleAsset;
    
    private String liabilityName;
    private String description;
    private Money originalLoanAmount;
    private Money originalLoanAmountInPortfolioCur;
    private Percentage annualInterestRate;
    private Instant maturityDate;
    private Money totalFeesInLiaCur;
    private Money totalFeesInPorfolioCur;

    private LiabilityIncurrenceTransactionDetails defaultIncurence;
    private CommonTransactionInput commonTransactionInput;

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

		appleAsset = new AssetIdentifier(
				"APPL", AssetType.STOCK, "US0378331005", "Apple", "NASDAQ", "DESCRIPTION");
        
        liabilityName = "CAR LOAN";
        description = "some car loan";
        
        originalLoanAmount = new Money("1400.54", cad);
        originalLoanAmountInPortfolioCur = new Money("1400.54", cad);
        annualInterestRate = Percentage.fromPercentage(BigDecimal.valueOf(7.32));
        maturityDate = Instant.now();
        totalFeesInLiaCur = Money.ZERO(cad);
        totalFeesInPorfolioCur = Money.ZERO(cad);
        defaultIncurence = new LiabilityIncurrenceTransactionDetails(liabilityName, description, originalLoanAmount, originalLoanAmountInPortfolioCur, annualInterestRate, maturityDate, totalFeesInLiaCur, totalFeesInPorfolioCur);

        commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), 
            null, 
            TransactionType.LIABILITY_INCURRENCE, 
            new TransactionMetadata(
                TransactionStatus.COMPLETED, 
                TransactionSource.MANUAL_INPUT, 
                "SOME DESC", 
                maturityDate, 
                maturityDate
            ), 
            null
        );

       
	}

    @Test
    void testRecordNewLiabilityPayment() {
        portfolio.recordNewLiability(defaultIncurence, commonTransactionInput, maturityDate);
        assertTrue(portfolio.getLiabilities().size()==1);
        UUID liabilityId = portfolio.getLiabilities().get(0).getLiabilityId();

        Money totalPaymentAmountInLiability = Money.of(BigDecimal.valueOf(100), cad);
        Money interestAmountInLiabilityCurrency = Money.of(BigDecimal.valueOf(5), cad);
        Money feesAmountInLiabilityCurrency = Money.ZERO(cad);
    
        Money totalPaymentAmountInPortfolioCurrency = Money.of(BigDecimal.valueOf(100), cad);
        Money interestAmountInPortfolioCurrency = Money.of(BigDecimal.valueOf(5), cad);
        Money feesAmountInPortfolioCurrency = Money.ZERO(cad);

        LiabilityPaymentTransactionDetails paymentTransactionDetails = new LiabilityPaymentTransactionDetails(liabilityId, totalPaymentAmountInLiability, interestAmountInLiabilityCurrency, feesAmountInLiabilityCurrency, totalPaymentAmountInPortfolioCurrency, interestAmountInPortfolioCurrency, feesAmountInPortfolioCurrency);

        commonTransactionInput = new CommonTransactionInput(
            UUID.randomUUID(), 
            null, 
            TransactionType.PAYMENT, 
            new TransactionMetadata(
                TransactionStatus.COMPLETED, 
                TransactionSource.MANUAL_INPUT, 
                "SOME DESC", 
                Instant.now(), 
                Instant.now()
            ), 
            null
        );

        portfolio.recordLiabilityPayment(paymentTransactionDetails, commonTransactionInput, maturityDate);
        assertTrue(portfolio.getTransactions().size() == 2);
        assertEquals(new Money("1305.54", cad), portfolio.getLiabilities().get(0).getCurrentBalance());

    }
}
