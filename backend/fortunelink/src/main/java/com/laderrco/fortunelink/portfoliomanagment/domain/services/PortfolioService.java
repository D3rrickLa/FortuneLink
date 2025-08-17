package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;

// defines user cases of your application in terms of your domain -> verbs that represent a user's intent
public interface PortfolioService {
    /*
     * user intent: 
     * 
     * make portfolio
     * delete portfolio
     * edit portfolio
     * 
     * record a trade
     * record cashflow
     * record liability
     * record payment
     * etc.
     */

    public PortfolioId createPortfolio(UserId userId, String name, String description, Money initialBalance);
    public void recordBuy(PortfolioId portfolioId, AssetIdentifier assetIdentifier, BigDecimal quantity, Money price, List<Fee> fees, Instant transactionDate, TransactionSource source);
    public void recordSell(PortfolioId portfolioId, AssetIdentifier assetIdentifier, BigDecimal quantity, Money price, List<Fee> fees, Instant transactionDate, TransactionSource source);
    public void recordCashflow(PortfolioId portfolioId, Money amount, CashflowType cashflowType, TransactionSource source, String description, List<Fee> fees, Instant transactionDate);
    public void recordLiabilityIncurrence(PortfolioId portfolioId, LiabilityDetails details, Money initialAmount, TransactionSource source, List<Fee> fees, Instant transactionDate);
    public void recordLiabilityPayment(PortfolioId portfolioId, LiabilityId liabilityId, Money paymentAmount, TransactionSource source, List<Fee> fees, Instant transactionDate);
}