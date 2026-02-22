package com.laderrco.fortunelink.portfolio.application.services;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfolio.domain.exceptions.AccountNotFoundException;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
@Transactional
public class PositionRecalculationService {
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionRecordingService transactionRecordingService;

    public void scheduleRecalculation(PortfolioId portfolioId, UserId userId, AccountId accountId, AssetSymbol symbol) {
        List<Transaction> active = transactionRepository
                .findByAccountIdAndSymbol(accountId, symbol)
                .stream()
                .filter(tx -> !tx.isExcluded())
                .sorted(Comparator.comparing(tx -> tx.occurredAt().timestamp()))
                .toList();

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(portfolioId, userId)
                .orElseThrow(() -> new AccountNotFoundException(accountId, portfolioId));

        Account account = portfolio.getAccount(accountId);
        account.clearPosition(symbol); // reset to zero
        active.forEach(tx -> transactionRecordingService.replayTransaction(account, tx));
        portfolioRepository.save(portfolio); // persist corrected state
    }
}
