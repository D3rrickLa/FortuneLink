package com.laderrco.fortunelink.portfoliomanagment.application.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.DomainEventPublisher;
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
    private final DomainEventPublisher domainEventPublisher;

    

    public ApplicationPortfolioService(PortfolioRepository portfolioRepository, DomainEventPublisher domainEventPublisher) {
        this.portfolioRepository = portfolioRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public PortfolioId createPortfolio(UserId userId, String name, String description) {
        // 1. Business Logic: The logic to create a new portfolio is in the aggregate.
        Portfolio newPortfolio = new Portfolio(userId, name, description);
        
        // 2. Orchestration: Use the repository to persist the new aggregate.
        PortfolioId newId = portfolioRepository.save(newPortfolio);

        // Optional: Publish a PortfolioCreatedEvent if needed.
        // domainEventPublisher.publish(new PortfolioCreatedEvent(newId));

        return newId;
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
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found."));

        // Delegate the specific "buy" action to the domain aggregate.
        portfolio.buyAsset(assetIdentifier, quantity, price, fees, transactionDate, source, "Buy order");

        portfolioRepository.save(portfolio);
        domainEventPublisher.publish(portfolio.getDomainEvents());
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
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found."));

        // Delegate the specific "sell" action to the domain aggregate.
        // portfolio.sellAsset(assetIdentifier, quantity, price, fees, transactionDate, source, "Sell order");

        portfolioRepository.save(portfolio);
        domainEventPublisher.publish(portfolio.getDomainEvents());
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
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found."));

        portfolio.recordCashflow(amount, cashflowType, source, description, fees, transactionDate);

        portfolioRepository.save(portfolio);
        domainEventPublisher.publish(portfolio.getDomainEvents());
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
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found."));

        portfolio.incurrNewLiability(details, initialAmount, source, fees, transactionDate);

        portfolioRepository.save(portfolio);
        domainEventPublisher.publish(portfolio.getDomainEvents());
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
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
            .orElseThrow(() -> new IllegalArgumentException("Portfolio not found."));
        
        portfolio.recordLiabilityPayment(liabilityId, paymentAmount, source, fees, transactionDate);

        portfolioRepository.save(portfolio);
        domainEventPublisher.publish(portfolio.getDomainEvents());
    }
 
}
