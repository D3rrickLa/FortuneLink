package com.laderrco.fortunelink.portfolio_management.application.services;

import java.util.List;

import javax.security.auth.login.AccountNotFoundException;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio_management.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio_management.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio_management.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio_management.application.validators.TransactionCommandValidator;
import com.laderrco.fortunelink.portfolio_management.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio_management.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio_management.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio_management.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio_management.domain.services.TransactionRecordingService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

// COMBO of a new Transaction service -> those from old portfolioappservice

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService {
    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionViewMapper transactionViewMapper;
    private final TransactionCommandValidator validator;

    private final MarketDataService marketDataService;
    private final ExchangeRateService exchangeRateService;
    private final TransactionRecordingService transactionRecordingService;

    public TransactionView recordPurchase(RecordPurchaseCommand command) {
        // validation here
        AssetSymbol symbol = new AssetSymbol(command.symbol());
        MarketAssetInfo assetInfo = marketDataService.getAssetInfo(symbol)
                .orElseThrow(() -> new AssetNotFoundException("Unknown symbol: " + command.symbol()));

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(), command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId().toString()));

        // account handles own try-catch
        Account account = portfolio.getAccount(command.accountId());

        Price price = resolvePrice(command.price(), account.getAccountCurrency());

        transactionRecordingService.recordBuy(
                account,
                symbol,
                assetInfo.type(),
                command.quantity(),
                price,
                consolidateFees(command.fees()),
                command.transactionDate());

        portfolioRepository.save(portfolio);

        Transaction recorded = transactionRepository
                .findByAccountId(account.getAccountId())
                .stream()
                .reduce((first, second) -> second)
                .orElseThrow();

        // return transactionviewmapper.totransactionview(recorded);
        return null;
    }

    private Price resolvePrice(Price commandPrice, Currency accountCurrency) {
        if (commandPrice.currency().equals(accountCurrency)) {
            return commandPrice;
        }
        // code smell, this is dumb
        return new Price(exchangeRateService.convert(commandPrice.pricePerUnit(), accountCurrency));
    }

    private Money consolidateFees(List<Fee> fees) {
        // sum all fees into single Money — implementation depends on your Fee type
        return null;
    }
}
