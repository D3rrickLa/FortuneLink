package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.positions;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Ratio;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;

import static com.laderrco.fortunelink.portfolio.domain.utils.Guard.notNull;

public record AcbPosition(AssetSymbol symbol, AssetType type, Currency accountCurrency,
        Quantity totalQuantity, Money totalCostBasis, Instant firstAcquiredAt) implements Position {

    public AcbPosition {
        notNull(symbol, "AssetSymbol");
        notNull(type, "type");
        notNull(accountCurrency, "accountCurrency");
        notNull(totalQuantity, "totalQuantity");
        notNull(totalCostBasis, "totalCostBasis");
    }

    public static AcbPosition empty(AssetSymbol symbol, AssetType type, Currency currency) {
        return new AcbPosition(symbol, type, currency, Quantity.ZERO, Money.ZERO(currency), null);
    }

    @Override
    public ApplyResult.Purchase<AcbPosition> buy(Quantity quantity, Money totalCost, Instant at) {
        Instant newAcquiredDate = (this.totalQuantity.isZero()) ? at : this.firstAcquiredAt;

        // totalQuantity.add() -> accumulate quantity
        // totalCostBasis.add() -> net price + commission
        AcbPosition updated = new AcbPosition(symbol, type, accountCurrency,
                totalQuantity.add(quantity), totalCostBasis.add(totalCost), newAcquiredDate);

        return new ApplyResult.Purchase<>(updated);
    }

    @Override
    public ApplyResult.Sale<AcbPosition> sell(Quantity quantity, Money proceeds, Instant at) {
        if (hasInSufficientQuantity(quantity)) {
            throw new IllegalStateException("Insufficient quantity");
        }

        BigDecimal ratio = quantity.amount().divide(totalQuantity.amount(), MathContext.DECIMAL128);

        // handles ghoest rounding
        boolean isFullLiquidation = quantity.equals(totalQuantity);
        Money costBasisSold = isFullLiquidation ? totalCostBasis : totalCostBasis.multiply(ratio);

        Money newCostBasis = isFullLiquidation ? Money.ZERO(accountCurrency)
                : totalCostBasis.subtract(costBasisSold);

        Money realizedGain = proceeds.subtract(costBasisSold);

        AcbPosition updated = new AcbPosition(symbol, type, accountCurrency,
                totalQuantity.subtract(quantity), newCostBasis, firstAcquiredAt);

        return new ApplyResult.Sale<>(updated, costBasisSold, realizedGain);

    }

    @Override
    public ApplyResult.Adjustment<AcbPosition> split(Ratio ratio) {
        // Use the Ratio to calculate the new quantity precisely
        Quantity newQuantity = this.totalQuantity.multiply(BigDecimal.valueOf(ratio.numerator()))
                .divide(BigDecimal.valueOf(ratio.denominator()));

        // Cost basis doesn't change in a split
        AcbPosition updated = new AcbPosition(symbol, type, accountCurrency, newQuantity,
                totalCostBasis, firstAcquiredAt);
        return new ApplyResult.Adjustment<>(updated);
    }

    @Override
    public ApplyResult<AcbPosition> applyReturnOfCapital(Price price, Quantity heldQuantity) {
        if (!heldQuantity.equals(this.totalQuantity)) {
            throw new IllegalArgumentException(
                    "ROC heldQuantity " + heldQuantity + " does not match position quantity " + totalQuantity);
        }

        Money totalReduction = price.calculateValue(heldQuantity);

        Money newCostBasis;
        Money excessCapitalGain;

        if (totalReduction.isAtLeast(totalCostBasis)) {
            excessCapitalGain = totalReduction.subtract(totalCostBasis);
            newCostBasis = Money.ZERO(accountCurrency);
        } else {
            excessCapitalGain = Money.ZERO(accountCurrency);
            newCostBasis = totalCostBasis.subtract(totalReduction);
        }

        AcbPosition updated = new AcbPosition(
                symbol,
                type,
                accountCurrency,
                totalQuantity,
                newCostBasis,
                firstAcquiredAt);

        if (excessCapitalGain.isPositive()) {
            return new ApplyResult.RocAdjustment<>(updated, excessCapitalGain);
        }

        return new ApplyResult.Adjustment<>(updated);
    }

    @Override
    public Money costPerUnit() {
        return isEmpty() ? Money.ZERO(accountCurrency)
                : totalCostBasis.divide(totalQuantity.amount());
    }

    @Override
    public Money currentValue(Money currentPrice) {
        return currentPrice.multiply(totalQuantity.amount());

    }

    public Money calculateUnrealizedGain(Money currentPrice) {
        return currentValue(currentPrice).subtract(totalCostBasis);
    }

}
