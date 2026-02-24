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

    /**
     * Builds an AccountView with position-level fee data populated.
     *
     * Fee display (Option A):
     * Fees are NOT stored in the position cost basis. To display
     * totalFeesIncurred per symbol, we fetch all transactions for this
     * account, filter to non-excluded BUY transactions, then sum their
     * fees per symbol. This is the only place in the read path that
     * combines position state with transaction fee history.
     *
     * The resulting fee totals are passed into toPositionView() so the
     * UI can render:
     * - Holdings screen: totalCostBasis (gross), unrealizedPnL
     * - Tax/ACB screen: effectiveAcb = totalCostBasis + totalFeesIncurred
     *
     * This incurs one extra repository call per account view. For MVP with
     * a single portfolio and a handful of accounts this is fine. If profiling
     * shows it's a hotspot, add a sumFeesByAccountId() query to the repo.
     */
    public AccountView build(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        // Compute per-symbol fee totals from transaction history
        Map<AssetSymbol, Money> feesBySymbol = computeFeesBySymbol(account);

        List<PositionView> positionViews = account.getPositionEntries().stream()
                .map(entry -> {
                    AssetSymbol symbol = entry.getKey();
                    Money feesForSymbol = feesBySymbol.getOrDefault(symbol, Money.ZERO(account.getAccountCurrency()));
                    return portfolioViewMapper.toPositionView(
                            entry.getValue(),
                            quoteCache.get(symbol),
                            feesForSymbol);
                })
                .toList();

        Money totalValue = portfolioValuationService.calculateAccountValue(account, quoteCache);
        Money cashBalance = account.getCashBalance();

        return portfolioViewMapper.toAccountView(account, positionViews, totalValue, cashBalance);
    }

    /**
     * Builds an AccountView without fee data — for summary screens where tax
     * breakdown is not needed. Avoids the extra transaction fetch.
     * totalFeesIncurred will be Price.ZERO on all PositionViews.
     */
    public AccountView buildSummary(Account account, Map<AssetSymbol, MarketAssetQuote> quoteCache) {
        List<PositionView> positionViews = account.getPositionEntries().stream()
                .map(entry -> portfolioViewMapper.toPositionView(entry.getValue(), quoteCache.get(entry.getKey())))
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