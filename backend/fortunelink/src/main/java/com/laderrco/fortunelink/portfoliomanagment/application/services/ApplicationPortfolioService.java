package com.laderrco.fortunelink.portfoliomanagment.application.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.CurrencyConversionService;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.DomainEventPublisher;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.PortfolioService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.assetobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.enums.transaction.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.LiabilityId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.liabilityobjects.LiabilityDetails;

public final class ApplicationPortfolioService implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final CurrencyConversionService currencyConversionService;
    private final MarketDataService marketDataService;
    private final DomainEventPublisher domainEventPublisher;

    

    public ApplicationPortfolioService(PortfolioRepository portfolioRepository, CurrencyConversionService currencyConversionService,  MarketDataService marketDataService, DomainEventPublisher domainEventPublisher) {
        this.portfolioRepository = portfolioRepository;
        this.currencyConversionService = currencyConversionService;
        this.marketDataService = marketDataService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public PortfolioId createPortfolio(UserId userId, String name, String description, Money initialBalance) {
        return null;
    }

    @Override
    public void recordBuy(
        PortfolioId portfolioId,
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money price,
        List<Fee> fees,
        Instant transactionDate,
        TransactionSource source
    ) {

    }

    @Override
    public void recordSell(
        PortfolioId portfolioId,
        AssetIdentifier assetIdentifier,
        BigDecimal quantity,
        Money price,
        List<Fee> fees,
        Instant transactionDate,
        TransactionSource source
    ) {
  
    }

    @Override
    public void recordCashflow(
        PortfolioId portfolioId, 
        Money amount, 
        CashflowType cashflowType, 
        TransactionSource source, 
        String description, 
        List<Fee> fees, 
        Instant transactionDate
    ) {

    }

    @Override
    public void recordLiabilityIncurrence(
        PortfolioId portfolioId, 
        LiabilityDetails details, 
        Money initialAmount, 
        TransactionSource source, 
        List<Fee> fees, 
        Instant transactionDate
    ) {

    }

    @Override
    public void recordLiabilityPayment(
        PortfolioId portfolioId, 
        LiabilityId liabilityId, 
        Money paymentAmount, 
        TransactionSource source, 
        List<Fee> fees, 
        Instant transactionDate
    ) {

    }
 
}
