package com.laderrco.fortunelink.portfolio.application.utils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio.application.mappers.PortfolioViewMapper;
import com.laderrco.fortunelink.portfolio.application.views.AccountView;
import com.laderrco.fortunelink.portfolio.application.views.PositionView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;
import com.laderrco.fortunelink.portfolio.domain.services.PortfolioValuationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountViewBuilder {
    private final PortfolioValuationService portfolioValuationService;
    private final TransactionRepository transactionRepository;
    private final PortfolioViewMapper portfolioViewMapper;

    public AccountView build(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        // We extract fees from transaction history for DISPLAY PURPOSES ONLY.
        // Note: These fees are already embedded in
        // entry.getValue().getTotalCostBasis().
        Map<AssetSymbol, Money> feeBreakdownBySymbol = computeFeesBySymbol(account);

        List<PositionView> positionViews = account.getPositionEntries().stream()
                .map(entry -> {
                    AssetSymbol symbol = entry.getKey();
                    // This is now a "breakdown" value, not an "additive" value
                    Money feesIncurred = feeBreakdownBySymbol
                            .getOrDefault(symbol, Money.ZERO(account.getAccountCurrency()));
                    return portfolioViewMapper.toPositionView(
                            entry.getValue(),
                            quoteCache.get(symbol),
                            feesIncurred);
                })
                .toList();

        Money totalValue = portfolioValuationService.calculateAccountValue(account, quoteCache);
        Money cashBalance = account.getCashBalance();

        return portfolioViewMapper.toAccountView(account, positionViews, totalValue, cashBalance);
    }

    /**
     * Builds an AccountView without fee data - for summary screens where tax
     * breakdown is not needed. Avoids the extra transaction fetch.
     * totalFeesIncurred will be Price.ZERO on all PositionViews.
     */
    public AccountView buildSummary(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        List<PositionView> positionViews = account.getPositionEntries().stream()
                .map(entry -> portfolioViewMapper
                        .toPositionView(entry.getValue(), quoteCache.get(entry.getKey())))
                .toList();

        Money totalValue = portfolioValuationService.calculateAccountValue(account, quoteCache);
        Money cashBalance = account.getCashBalance();

        return portfolioViewMapper.toAccountView(account, positionViews, totalValue, cashBalance);
    }

    /**
     * Sums fees per symbol from all non-excluded BUY transactions on this account.
     *
     * Only BUY fees are included — SELL fees reduce proceeds and are already
     * reflected in the realized gain via cashDelta. Including them here would
     * double-count their impact on the effective ACB display.
     *
     * Excluded transactions are skipped — they don't contribute to current
     * position state, so they shouldn't contribute to displayed fee totals either.
     */
    private Map<AssetSymbol, Money> computeFeesBySymbol(Account account) {
        List<Transaction> transactions = transactionRepository.findByAccountId(account.getAccountId());

        return transactions.stream()
                .filter(tx -> !tx.isExcluded())
                .filter(tx -> tx.transactionType() == TransactionType.BUY)
                .filter(tx -> tx.execution() != null)
                .filter(tx -> !tx.fees().isEmpty())
                .collect(Collectors.toMap(
                        tx -> tx.execution().asset(),
                        tx -> Fee.totalInAccountCurrency(tx.fees(), account.getAccountCurrency()),
                        Money::add)); // merge: sum fees when same symbol appears multiple times
    }
}