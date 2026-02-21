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

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(), command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId().toString()));

        // account handles own try-catch
        Account account = portfolio.getAccount(command.accountId());

        AssetSymbol symbol = new AssetSymbol(command.symbol());
        MarketAssetInfo assetInfo = marketDataService.getAssetInfo(symbol)
                .orElseThrow(() -> new AssetNotFoundException("Unknown symbol: " + command.symbol()));

        Price price = resolvePrice(command.price(), account.getAccountCurrency());

        Transaction recordedTransaction = transactionRecordingService.recordBuy(
                account,
                symbol,
                assetInfo.type(),
                command.quantity(),
                price,
                consolidateFees(command.fees()),
                command.transactionDate());

        portfolioRepository.save(portfolio);
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);
    }

    public TransactionView recordSale(RecordSaleCommand command) {
        validate(command, validator::validate, "recordSale");

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(), command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId()));

        Account account = portfolio.getAccount(command.accountId());

        AssetSymbol symbol = new AssetSymbol(command.symbol());

        if (!account.hasPosition(symbol)) {
            throw new InsufficientQuantityException("No posotion found for: " + command.symbol());
        }

        Price price = resolvePrice(command.price(), account.getAccountCurrency());

        Transaction recordedTransaction = transactionRecordingService.recordSell(
                account,
                symbol,
                command.quantity(),
                price,
                consolidateFees(command.fees()),
                command.transactionDate());

        portfolioRepository.save(portfolio);
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);

    }

    public TransactionView recordDeposit(RecordDepositCommand command) {
        validate(command, validator::validate, "recordDeposit");

        Portfolio portfolio = portfolioRepository.findByIdAndUserId(command.portfolioId(), command.userId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId()));

        Account account = portfolio.getAccount(command.accountId());

        Transaction recordedTransaction = transactionRecordingService.recordDeposit(
                account,
                command.amount(),
                command.transactionDate());

        portfolioRepository.save(portfolio);
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);

    }

    public TransactionView recordWidthdrawal(RecordWithdrawalCommand command) {
        validate(command, validator::validate, "recordWidthdrawal");
        // NOTE: we can simplify the 'gathering of portfolio and account through a DRY
        // method'
        Portfolio portfolio = portfolioRepository.findById(command.portfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId()));

        Account account = portfolio.getAccount(command.accountId());

        Transaction recordedTransaction = transactionRecordingService.recordWithdrawal(
                account,
                command.amount(),
                command.transactionDate());

        portfolioRepository.save(portfolio);
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);
    }

    public TransactionView recordFee(RecordFeeCommand command) {
        validate(command, validator::validate, "recordFee");

        Portfolio portfolio = portfolioRepository.findById(command.portfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId()));

        Account account = portfolio.getAccount(command.accountId());

        // we need another service method for 'fees'
        Transaction recordedTransaction = transactionRecordingService.recordFee(
                account,
                command.totalAmount(),
                command.transactionDate());

        portfolioRepository.save(portfolio);
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);
    }

    // this method deposits into the account
    public TransactionView recordDividend(RecordIncomeCommand command) {
        validate(command, validator::validate, "recordDividend");

        if (command.isDrip()) {
            throw new IllegalStateException("Drip is turned on, please use the recordDividendReinvestment method.");
        }

        Portfolio portfolio = portfolioRepository.findById(command.portfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId()));

        Account account = portfolio.getAccount(command.accountId());

        AssetSymbol symbol = new AssetSymbol(command.assetSymbol());

        Transaction recordedTransaction = transactionRecordingService.recordDividend(
                account,
                symbol,
                command.amount(),
                command.transactionDate());

        portfolioRepository.save(portfolio);
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);

    }

    public TransactionView recordDividendReinvestment(RecordIncomeCommand command) {
        validate(command, validator::validate, "recordDividend");

        if (!command.isDrip()) {
            throw new IllegalStateException("Drip is turned off, please use the recordDividend method.");
        }

        Portfolio portfolio = portfolioRepository.findById(command.portfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId()));

        Account account = portfolio.getAccount(command.accountId());

        AssetSymbol symbol = new AssetSymbol(command.assetSymbol());

        Transaction recordedTransaction = transactionRecordingService.recordDividendReinvestment(
                account,
                symbol,
                command.sharesReceived(),
                new Price(command.amount()),
                command.transactionDate());

        portfolioRepository.save(portfolio);
        transactionRepository.save(recordedTransaction);

        return transactionViewMapper.toView(recordedTransaction);

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

    private <T> void validate(T command, Function<T, ValidationResult> validationLogic, String methodName) {
        ValidationResult result = validationLogic.apply(command);
        if (!result.isValid()) {
            String msg = String.format("Invalid %s command", methodName);
            throw new InvalidTransactionException(msg, result.errors());
        }
    }
}
