package com.laderrco.fortunelink.portfoliomanagment.domain.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfoliomanagment.domain.repositories.PortfolioRepository;
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
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'createPortfolio'");
    }

    @Override
    public void recordTrade(PortfolioId portfolioId, AssetIdentifier assetIdentifier, BigDecimal quantity, Money price,
            List<Fee> fees, Instant transactionDate, TransactionSource source) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordTrade'");
    }

    @Override
    public void recordCashflow(PortfolioId portfolioId, Money amount, CashflowType cashflowType,
            TransactionSource source, String description, List<Fee> fees, Instant transactionDate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordCashflow'");
    }

    @Override
    public void recordLiabilityIncurrence(PortfolioId portfolioId, LiabilityDetails details, Money initialAmount,
            TransactionSource source, List<Fee> fees, Instant transactionDate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordLiabilityIncurrence'");
    }

    @Override
    public void recordLiabilityPayment(PortfolioId portfolioId, LiabilityId liabilityId, Money paymentAmount,
            TransactionSource source, List<Fee> fees, Instant transactionDate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'recordLiabilityPayment'");
    }
 
}
