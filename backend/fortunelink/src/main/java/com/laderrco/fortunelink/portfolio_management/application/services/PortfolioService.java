package com.laderrco.fortunelink.portfolio_management.application.services;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// combo of the old app service and query service
// ONLY PORTFOLIO LIFECYCLE STUFF, NOT TRANSACTION
@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioService {
    private final PortfolioRepository portfolioRepository;

    public void createPortfolio() {}

    public void updatePortfolio() {}
    
    public void deletePortfolio() {}
    
    public void createAccount(AddAccountCommand command) {}

    public void updateAccount() {}

    public void deleteAccount() {}
}
