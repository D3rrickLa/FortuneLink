package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AccountMetadataKey;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AccountEffect;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.CurrencyConversion;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Money;

/**
 * This class represents any event related to cashflow (deposits, withdrawals, transfers, etc.)
 * and income (interest, dividends, rental income, etc.)
 * 
 * If we were to do what we have been doing all along, making subclasses for each transaction,
 * we would have an explosion of clases. The solution to this is composition 
 * AccountTransactionDetails 'has-a' AccountEffect
 */
public class AccountTransactionDetails extends TransactionDetails {
    private final AccountEffect accountEffect;

    protected AccountTransactionDetails(AccountEffect accountEffect, TransactionSource source, String description, List<Fee> fees) {
        super(source, description, fees);
        this.accountEffect = Objects.requireNonNull(accountEffect, "Account effect cannot be null.");
        validateFeeConsistency(fees, accountEffect);
        validateBusinessRules(accountEffect);        
    }

    // Factory methods for common transaction types
    public static AccountTransactionDetails createDividendTransaction(
        TransactionSource source,
        String description,
        List<Fee> fees,
        MonetaryAmount grossDividend,
        BigDecimal withholdingTaxRate
    ) {
        
        MonetaryAmount withholdingTax = grossDividend.multiply(withholdingTaxRate);
        MonetaryAmount netDividend = grossDividend.subtract(withholdingTax);
        
        Map<String, String> metadata = Map.of(
            AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey(), withholdingTaxRate.toString(),
            AccountMetadataKey.TAX_YEAR.getKey(), String.valueOf(LocalDate.now().getYear())
        );
        
        AccountEffect effect = new AccountEffect(
            grossDividend, 
            netDividend, 
            CashflowType.DIVIDEND, 
            metadata
        );
        
        return new AccountTransactionDetails(effect, source, description, fees);
    }
    
    public static AccountTransactionDetails createSimpleDepositTransaction(
        TransactionSource source,
        String description,
        MonetaryAmount depositAmount
    ) {
        
        AccountEffect effect = new AccountEffect(
            depositAmount,
            depositAmount, // No fees for simple deposit
            CashflowType.DEPOSIT,
            Collections.emptyMap()
        );
        
        return new AccountTransactionDetails(effect, source, description, Collections.emptyList());
    }

    public static AccountTransactionDetails createWithdrawalTransaction(
        TransactionSource source,
        String description,
        MonetaryAmount withdrawalAmount,
        List<Fee> fees) {
        
        // Withdrawals are negative impacts
        MonetaryAmount negativeAmount = withdrawalAmount.negate();
        
        AccountEffect effect = new AccountEffect(
            negativeAmount,
            negativeAmount, // Assuming no additional fees beyond the fee list
            CashflowType.WITHDRAWAL,
            Collections.emptyMap()
        );
        
        return new AccountTransactionDetails(effect, source, description, fees);
    }

    public static AccountTransactionDetails createInterestTransaction(
        TransactionSource source,
        String description,
        MonetaryAmount interestAmount) {
        
        AccountEffect effect = new AccountEffect(
            interestAmount,
            interestAmount,
            CashflowType.INTEREST,
            Map.of(AccountMetadataKey.TAX_YEAR.getKey(), String.valueOf(LocalDate.now().getYear()))
        );
        
        return new AccountTransactionDetails(effect, source, description, Collections.emptyList());
    }


    
    public AccountEffect getAccountEffect() {
        return accountEffect;
    }

    public MonetaryAmount getNetWorthImpact() {
        return accountEffect.netAmount();
    }
    
    public boolean affectsNetWorth() {
        return !accountEffect.netAmount().nativeAmount().isZero();
    }
    
    public boolean isIncomeTransaction() {
        return accountEffect.isIncomeTransaction();
    }
    
    public boolean isExpenseTransaction() {
        return accountEffect.isExpenseTransaction();
    }
    
    public boolean requiresTaxReporting() {
        return accountEffect.requiresTaxReporting();
    }
    
    public boolean isMultiCurrency() {
        return accountEffect.isMultiCurrency();
    }
    
    public MonetaryAmount getCashFlow() {
        return accountEffect.netAmount();
    }
    
    public MonetaryAmount getTotalFees() {
        return accountEffect.getFeeAmount();
    }

    // reconciliation with extenral data
    public boolean matchesExternalAmount(MonetaryAmount externalAmount) {
        return accountEffect.grossAmount().equals(externalAmount) ||
            accountEffect.netAmount().equals(externalAmount);
    }
    
    private void validateFeeConsistency(List<Fee> transactionFees, AccountEffect accountEffect) {
        if (transactionFees == null || transactionFees.isEmpty()) {
            return;
        }

        // sum up all transaction fees
        MonetaryAmount totalTransactionFees = transactionFees.stream()
            .map(fee -> convertFeeToMonetaryAmount(fee, accountEffect.grossAmount().nativeAmount().currency()))
            .reduce(MonetaryAmount.ZERO(accountEffect.grossAmount().nativeAmount().currency()), 
                   MonetaryAmount::add);
            
        MonetaryAmount effectFees = accountEffect.getFeeAmount();
        
        // Allow some tolerance for rounding differences
        if (!areAmountsEqual(totalTransactionFees, effectFees)) {
            throw new IllegalArgumentException(
                String.format("Fee amounts inconsistent: transaction fees=%s, effect fees=%s", 
                    totalTransactionFees, effectFees));
        }        
    }

    private void validateBusinessRules(AccountEffect effect) {
        if (!effect.isValidForCashflowType()) {
            throw new IllegalArgumentException(String.format("Amount direction inconsistent with cashflow type: %s", effect.cashflowType()));
        }        
    }

    private MonetaryAmount convertFeeToMonetaryAmount(Fee fee, Currency targetCurrency) {
        Money feeAmount = fee.amount().nativeAmount();
        if (feeAmount.currency().equals(targetCurrency)) {
            return MonetaryAmount.of(feeAmount, CurrencyConversion.identity(targetCurrency));
        }    
        else {
            // Need currency conversion - this requires ExchangeRateService
            throw new UnsupportedOperationException("Cross-currency fee conversion requires ExchangeRateService");
        }
    }

    private boolean areAmountsEqual(MonetaryAmount a, MonetaryAmount b) {
        MonetaryAmount difference = a.subtract(b).abs();
        
        // Better approach - use currency-specific precision
        Currency currency = a.nativeAmount().currency();
        int precision = currency.getDefaultFractionDigits();
        BigDecimal threshold = BigDecimal.ONE.scaleByPowerOfTen(-precision); // 0.01 for USD, 1 for JPY, etc.
        
        return difference.nativeAmount().amount().compareTo(threshold) <= 0;
    }    
    
}
