package com.laderrco.fortunelink.portfolio.domain.services;

import java.time.Instant;
import java.util.List;

import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Transaction;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

// Mutate the account 'state' as in update positions
public interface TransactionRecordingService {
	Transaction recordBuy(Account account, AssetSymbol symbol, AssetType type, Quantity quantity,
			Price price, List<Fee> fees, String notes, Instant date);

	Transaction recordSell(Account account, AssetSymbol symbol, Quantity quantity, Price price,
			List<Fee> fees, String notes, Instant date);

	Transaction recordDeposit(Account account, Money amount, String notes, Instant date);

	Transaction recordWithdrawal(Account account, Money amount, String notes, Instant date);

	Transaction recordFee(Account account, Money amount, String notes, Instant date);

	Transaction recordInterest(Account account, AssetSymbol symbol, Money amount, String notes,
			Instant date);

	Transaction recordDividend(Account account, AssetSymbol symbol, Money amount, String notes,
			Instant date);

	Transaction recordDividendReinvestment(Account account, AssetSymbol symbol, Quantity quantity,
			Price price, String notes, Instant date);

	Transaction recordReturnOfCapital(Account account, AssetSymbol symbol, Quantity quantity,
			Price distPerUnitPrice, String notes, Instant date);

	/**
	 * Position-only replay. Cash state is NOT touched. Use for exclude/restore recalculation on a
	 * single symbol. Caller must call account.clearPosition(symbol) before invoking.
	 */
	void replayTransaction(Account account, Transaction tx);

	/**
	 * Full replay — applies both position AND cash effects. Use ONLY for full account reconstruction
	 * (migration, corruption recovery).
	 * 
	 * Caller MUST reset both positions and cash to zero before invoking. Calling this without
	 * resetting first will double-count everything.
	 * 
	 * Does NOT return a Transaction - it is applying an existing one, not recording a new one.
	 */
	void replayFullTransaction(Account account, Transaction tx);

}
