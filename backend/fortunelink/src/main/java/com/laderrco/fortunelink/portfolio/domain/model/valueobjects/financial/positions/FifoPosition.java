package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.*;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

/**
 * FIFO position for future USD/US-tax-reporting support. NOT wired into any
 * active account creation
 * path as of v8. Do not instantiate except in unit tests.
 */
public record FifoPosition(AssetSymbol symbol, AssetType type, Currency accountCurrency,
		List<TaxLot> lots) implements Position {

	public FifoPosition {
		notNull(symbol, "AssetSymbol");
		notNull(type, "type");
		notNull(accountCurrency, "accountCurrency");
		lots = lots == null ? List.of() : List.copyOf(lots);
	}

	public static FifoPosition empty(AssetSymbol symbol, AssetType type, Currency accountCurrency) {
		return new FifoPosition(symbol, type, accountCurrency, List.of());
	}

	@Override
	public ApplyResult<FifoPosition> buy(Quantity quantity, Money totalCost, Instant at) {
		TaxLot newLot = new TaxLot(quantity, totalCost, at);

		List<TaxLot> updatedLots = new ArrayList<>(lots);
		updatedLots.add(newLot);

		return new ApplyResult.Purchase<>(new FifoPosition(symbol, type, accountCurrency, updatedLots));
	}

	@Override
	public ApplyResult<FifoPosition> sell(Quantity quantity, Money proceeds, Instant at) {
		if (hasInSufficientQuantity(quantity)) {
			throw new IllegalStateException("Insufficient quantity");
		}

		Quantity remainingToSell = quantity;
		Money costBasisSold = Money.ZERO(accountCurrency);
		List<TaxLot> remainingLots = new ArrayList<>();

		for (TaxLot lot : lots) {
			if (remainingToSell.isZero()) {
				remainingLots.add(lot);
				continue;
			}

			if (lot.quantity().compareTo(remainingToSell) <= 0) {
				// Consume entire lot
				costBasisSold = costBasisSold.add(lot.costBasis());
				remainingToSell = remainingToSell.subtract(lot.quantity());
			} else {
				Money consumedCost = lot.proportionalCost(remainingToSell);
				costBasisSold = costBasisSold.add(consumedCost);
				remainingLots.add(lot.remainingAfter(remainingToSell));
				remainingToSell = Quantity.ZERO;
			}
		}

		Money realizedGainLoss = proceeds.subtract(costBasisSold);

		return new ApplyResult.Sale<>(new FifoPosition(symbol, type, accountCurrency, remainingLots),
				costBasisSold, realizedGainLoss);
	}

	@Override
	public ApplyResult<FifoPosition> split(Ratio ratio) {
		List<TaxLot> splitLots = lots.stream().map(lot -> lot.split(ratio)).toList();

		return new ApplyResult.Adjustment<>(new FifoPosition(symbol, type, accountCurrency, splitLots));
	}

	@Override
	public ApplyResult<FifoPosition> applyReturnOfCapital(Price price, Quantity heldQuantity) {
		if (!heldQuantity.equals(totalQuantity())) {
			throw new IllegalArgumentException(
					"ROC heldQuantity " + heldQuantity + " does not match position quantity " + totalQuantity());
		}

		Money totalReduction = price.calculateValue(heldQuantity);
		Money totalCostBasis = totalCostBasis();

		Money excessGain = Money.ZERO(accountCurrency);

		if (totalReduction.isAtLeast(totalCostBasis)) {
			excessGain = totalReduction.subtract(totalCostBasis);
			totalReduction = totalCostBasis;
		}

		List<TaxLot> newLots = new ArrayList<>();

		for (TaxLot lot : lots) {

			BigDecimal ratio = lot.costBasis()
					.amount()
					.divide(totalCostBasis.amount(), MathContext.DECIMAL128);

			Money lotReduction = totalReduction.multiply(ratio);

			Money newCostBasis = lot.costBasis().subtract(lotReduction);

			if (newCostBasis.isNegative()) {
				newCostBasis = Money.ZERO(accountCurrency);
			}

			newLots.add(new TaxLot(
					lot.quantity(),
					newCostBasis,
					lot.acquiredDate()));
		}

		FifoPosition updated = new FifoPosition(
				symbol,
				type,
				accountCurrency,
				newLots);

		if (excessGain.isPositive()) {
			return new ApplyResult.RocAdjustment<>(updated, excessGain);
		}

		return new ApplyResult.Adjustment<>(updated);
	}

	@Override
	public Quantity totalQuantity() {
		return lots.stream().map(TaxLot::quantity).reduce(Quantity.ZERO, Quantity::add);
	}

	@Override
	public Money totalCostBasis() {
		return lots.stream().map(TaxLot::costBasis).reduce(Money.ZERO(accountCurrency), Money::add);
	}

	@Override
	public Money costPerUnit() {
		return isEmpty() ? Money.ZERO(accountCurrency) : totalCostBasis().divide(totalQuantity());
	}

	@Override
	public Money currentValue(Money currentPrice) {
		return currentPrice.multiply(totalQuantity());
	}

}
