package com.laderrco.fortunelink.portfolio.application.services;

import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.laderrco.fortunelink.portfolio.application.commands.records.RecordDepositCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordFeeCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordIncomeCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordPurchaseCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordSaleCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.RecordWithdrawalCommand;
import com.laderrco.fortunelink.portfolio.application.commands.records.TransactionCommand;
import com.laderrco.fortunelink.portfolio.application.exceptions.AssetNotFoundException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InsufficientQuantityException;
import com.laderrco.fortunelink.portfolio.application.exceptions.InvalidTransactionException;
import com.laderrco.fortunelink.portfolio.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfolio.application.mappers.TransactionViewMapper;
import com.laderrco.fortunelink.portfolio.application.validators.TransactionCommandValidator;
import com.laderrco.fortunelink.portfolio.application.validators.ValidationResult;
import com.laderrco.fortunelink.portfolio.application.views.TransactionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Portfolio;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetInfo;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import com.laderrco.fortunelink.portfolio.domain.services.TransactionRecordingService;

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
        validate(command, validator::validate, "recordPurchase");
        PortfolioContext ctx = getPortfolioContext(command);

        AssetSymbol symbol = new AssetSymbol(command.symbol());
        MarketAssetInfo assetInfo = marketDataService.getAssetInfo(symbol)
                .orElseThrow(() -> new AssetNotFoundException("Unknown symbol: " + command.symbol()));

        Price price = resolvePrice(command.price(), ctx.account().getAccountCurrency());

        Transaction recordedTransaction = transactionRecordingService.recordBuy(
                ctx.account(),
                symbol,
                assetInfo.type(),
                command.quantity(),
                price,
                consolidateFees(command.fees(), ctx.account().getAccountCurrency()),
                command.transactionDate());

        portfolioRepository.save(ctx.portfolio());
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);
    }

    public TransactionView recordSale(RecordSaleCommand command) {
        validate(command, validator::validate, "recordSale");
        PortfolioContext ctx = getPortfolioContext(command);

        AssetSymbol symbol = new AssetSymbol(command.symbol());

        if (!ctx.account().hasPosition(symbol)) {
            throw new InsufficientQuantityException("No position found for: " + command.symbol());
        }

        Price price = resolvePrice(command.price(), ctx.account().getAccountCurrency());

        Transaction recordedTransaction = transactionRecordingService.recordSell(
                ctx.account(),
                symbol,
                command.quantity(),
                price,
                consolidateFees(command.fees(), ctx.account().getAccountCurrency()),
                command.transactionDate());

        portfolioRepository.save(ctx.portfolio());
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);

    }

    public TransactionView recordDeposit(RecordDepositCommand command) {
        validate(command, validator::validate, "recordDeposit");
        PortfolioContext ctx = getPortfolioContext(command);

        Transaction recordedTransaction = transactionRecordingService.recordDeposit(
                ctx.account(),
                command.amount(),
                command.transactionDate());

        portfolioRepository.save(ctx.portfolio());
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);

    }

    public TransactionView recordWithdrawal(RecordWithdrawalCommand command) {
        validate(command, validator::validate, "recordWidthdrawal");
        PortfolioContext ctx = getPortfolioContext(command);

        Transaction recordedTransaction = transactionRecordingService.recordWithdrawal(
                ctx.account(),
                command.amount(),
                command.transactionDate());

        portfolioRepository.save(ctx.portfolio());
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);
    }

    public TransactionView recordFee(RecordFeeCommand command) {
        validate(command, validator::validate, "recordFee");
        PortfolioContext ctx = getPortfolioContext(command);

        // we need another service method for 'fees'
        Transaction recordedTransaction = transactionRecordingService.recordFee(
                ctx.account(),
                command.amount(),
                command.transactionDate());

        portfolioRepository.save(ctx.portfolio());
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);
    }

    // this method deposits into the account
    public TransactionView recordDividend(RecordIncomeCommand command) {
        validate(command, validator::validate, "recordDividend");
        PortfolioContext ctx = getPortfolioContext(command);

        AssetSymbol symbol = new AssetSymbol(command.assetSymbol());

        Transaction recordedTransaction = transactionRecordingService.recordDividend(
                ctx.account(),
                symbol,
                command.amount(),
                command.transactionDate());

        portfolioRepository.save(ctx.portfolio());
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);

    }

    public TransactionView recordDividendReinvestment(RecordIncomeCommand command) {
        validate(command, validator::validate, "recordDividend");
        PortfolioContext ctx = getPortfolioContext(command);

        AssetSymbol symbol = new AssetSymbol(command.assetSymbol());

        Transaction recordedTransaction = transactionRecordingService.recordDividendReinvestment(
                ctx.account(),
                symbol,
                command.sharesReceived(),
                new Price(command.amount()),
                command.transactionDate());

        portfolioRepository.save(ctx.portfolio());
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);

    }

    private PortfolioContext getPortfolioContext(TransactionCommand command) {
        Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(), command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId().toString()));

        Account account = portfolio.getAccount(command.accountId());

        return new PortfolioContext(portfolio, account);
    }

    private Price resolvePrice(Price commandPrice, Currency accountCurrency) {
        if (commandPrice.currency().equals(accountCurrency)) {
            return commandPrice;
        }
    
        return exchangeRateService.convertToPrice(commandPrice.pricePerUnit(), accountCurrency);
    }

    private Money consolidateFees(List<Fee> fees, Currency currency) {
        // sum all fees into single Money — implementation depends on your Fee type
        if (fees == null || fees.isEmpty()) {
            return Money.ZERO(currency);
        }
        return fees.stream()
                .map(Fee::accountAmount)
                .reduce(Money::add)
                .orElse(Money.ZERO(currency));
    }

    private <T> void validate(T command, Function<T, ValidationResult> validationLogic, String methodName) {
        ValidationResult result = validationLogic.apply(command);
        if (!result.isValid()) {
            String msg = String.format("Invalid %s command", methodName);
            throw new InvalidTransactionException(msg, result.errors());
        }
    }

    private record PortfolioContext(Portfolio portfolio, Account account) {
    }
}
