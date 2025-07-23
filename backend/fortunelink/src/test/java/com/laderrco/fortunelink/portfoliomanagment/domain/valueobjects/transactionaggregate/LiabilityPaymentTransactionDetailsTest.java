package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.LiabilityPaymentTransactionDetails;
import com.laderrco.fortunelink.shared.valueobjects.Money;

public class LiabilityPaymentTransactionDetailsTest {
    private UUID  liabilityId;
    private Money totalPaymentAmountInLiabilityCurrency;
    private Money interestAmountInLiabilityCurrency;
    private Money feesAmountInLiabilityCurrency;
    private Money totalPaymentAmountInPortfolioCurrency;
    private Money interestAmountInPortfolioCurrency;
    private Money feesAmountInPortfolioCurrency;
    Currency cad;

    @BeforeEach
    void init() {
    liabilityId = UUID.randomUUID();
    cad = Currency.getInstance("CAD");
    totalPaymentAmountInLiabilityCurrency = Money.of(BigDecimal.valueOf(1000), cad);
    interestAmountInLiabilityCurrency =  Money.of(BigDecimal.valueOf(10), cad);
    feesAmountInLiabilityCurrency =  Money.of(BigDecimal.valueOf(0.05), cad);
    totalPaymentAmountInPortfolioCurrency =  Money.of(BigDecimal.valueOf(1000), cad);
    interestAmountInPortfolioCurrency =  Money.of(BigDecimal.valueOf(10), cad);
    feesAmountInPortfolioCurrency =  Money.of(BigDecimal.valueOf(0.05), cad);
    }

    @Test
    void testConstructor() {
        LiabilityPaymentTransactionDetails liabilityPaymentTransactionDetails = new LiabilityPaymentTransactionDetails(liabilityId, totalPaymentAmountInLiabilityCurrency, interestAmountInLiabilityCurrency, feesAmountInLiabilityCurrency, totalPaymentAmountInPortfolioCurrency, interestAmountInPortfolioCurrency, feesAmountInPortfolioCurrency);
        assertEquals(liabilityId, liabilityPaymentTransactionDetails.getLiabilityId());
        assertEquals(totalPaymentAmountInLiabilityCurrency, liabilityPaymentTransactionDetails.getTotalPaymentAmountInLiabilityCurrency());
        assertEquals(interestAmountInLiabilityCurrency, liabilityPaymentTransactionDetails.getInterestAmountInLiabilityCurrency());
        assertEquals(feesAmountInLiabilityCurrency, liabilityPaymentTransactionDetails.getFeesAmountInLiabilityCurrency());
        assertEquals(totalPaymentAmountInPortfolioCurrency, liabilityPaymentTransactionDetails.getTotalPaymentAmountInPortfolioCurrency());
        assertEquals(interestAmountInPortfolioCurrency, liabilityPaymentTransactionDetails.getInterestAmountInPortfolioCurrency());
        assertEquals(feesAmountInPortfolioCurrency, liabilityPaymentTransactionDetails.getFeesAmountInPortfolioCurrency());
    }

    

}

