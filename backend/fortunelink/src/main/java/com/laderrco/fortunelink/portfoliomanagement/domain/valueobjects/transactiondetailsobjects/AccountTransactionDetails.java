package com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.transactiondetailsobjects;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.laderrco.fortunelink.portfoliomanagement.domain.enums.AccountMetadataKey;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.CashflowType;
import com.laderrco.fortunelink.portfoliomanagement.domain.enums.transactions.TransactionSource;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AccountEffect;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.MonetaryAmount;

/**
 * This class represents any event related to cashflow (deposits, withdrawals, transfers, etc.)
 * and income (interest, dividends, rental income, etc.)
 * 
 * If we were to do what we have been doing all along, making subclasses for each transaction,
 * we would have an explosion of clases. The solution to this is composition 
 * AccountTransactionDetails 'has-a' AccountEffect
 * 
 * 
 * CASHFLOWS WE HAVE NOT IMPELEMENTED with a factory methods -> we plan to not do those
 * - RENTAL_INCOME
 * - OTHER_INCOME 
 * - TRANSFER
 * - UNKNOWN
 * - FEE
 * 
 * 
 * 
 * NOTE: we we say net, we are refering to gross - withholding/taxes, THIS EXCLUDES ANY FEES
// Dividend example:
// Gross: $100 dividend declared
// Withholding tax: $15 (external force - IRS)
// Net in AccountEffect: $85 (what you're entitled to receive)
// Brokerage fee: $1 (your fee, stored separately in List<Fee>)
// Actual cash received: $84 (net - fees)
 * the reason we do this is because the withholding taxes are part fo the transaction itself, you have had that $15 to begin with

 */
public class AccountTransactionDetails extends TransactionDetails {
   private final AccountEffect accountEffect;

   public AccountTransactionDetails(AccountEffect accountEffect, TransactionSource source, String description, List<Fee> fees){
      super(source, description, fees);
      this.accountEffect = Objects.requireNonNull(accountEffect, "Account effect cannot be null.");
      // validateBusinessRules(accountEffect);
   }

   public static AccountTransactionDetails createDividendTransaction(
      MonetaryAmount grossDividend,
      BigDecimal withholdingTaxRate, // would this be better as a Percentage.java
      TransactionSource source, 
      String description,
      List<Fee> fees
   ) {
      return createDividendTransaction(
         grossDividend, 
         withholdingTaxRate, 
         source, 
         description, 
         fees,
         Map.of() // Empty additional metadata
      );
   }

   public static AccountTransactionDetails createDividendTransaction(
      MonetaryAmount grossDividend,
      BigDecimal withholdingTaxRate,
      TransactionSource source,
      String description, 
      List<Fee> fees,
      Map<String, String> additionalMetadata
   ) {
      MonetaryAmount withholdingTax = grossDividend.multiply(withholdingTaxRate);
      MonetaryAmount netDividend = grossDividend.subtract(withholdingTax);
      
      Map<String, String> metadata = new HashMap<>();
      metadata.put(AccountMetadataKey.TAX_YEAR.getKey(), String.valueOf(LocalDate.now().getYear()));
      metadata.put(AccountMetadataKey.GROSS_DIVIDEND.getKey(), String.format("%s %s", grossDividend.nativeAmount().amount().toString(), grossDividend.nativeAmount().currency().toString()));
      metadata.put(AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey(), withholdingTaxRate.toString());
      metadata.put(AccountMetadataKey.WITHHOLDING_TAX_AMOUNT.getKey(), String.format("%s %s", withholdingTax.nativeAmount().amount().toString(), withholdingTax.nativeAmount().currency().toString()));
      metadata.putAll(additionalMetadata);

      AccountEffect effect = new AccountEffect(withholdingTax, netDividend, CashflowType.DIVIDEND, metadata);
      return new AccountTransactionDetails(effect, source, description, fees);
   }

   // simple factory method for when you only know the net amount (broker import)
   public static AccountTransactionDetails createDividendTransactionNetOnly(
      MonetaryAmount netDividendReceived, 
      TransactionSource source,
      String description,
      List<Fee> fees
   ) {
      // When you only know net amount, set gross = net and withholding = 0
      Map<String, String> metadata = Map.of(
         AccountMetadataKey.TAX_YEAR.getKey(), String.valueOf(LocalDate.now().getYear()),
         AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey(), "0.00",
         AccountMetadataKey.GROSS_DIVIDEND.getKey(), String.format("%s %s", netDividendReceived.nativeAmount().amount().toString(), netDividendReceived.nativeAmount().currency().toString()),
         AccountMetadataKey.WITHHOLDING_UNKNOWN.getKey(), "true" // Flag for later adjustment
      );

      AccountEffect effect = new AccountEffect(
         MonetaryAmount.ZERO(netDividendReceived.nativeAmount().currency(), netDividendReceived.conversion().toCurrency()), // No withholding info available
         netDividendReceived, 
         CashflowType.DIVIDEND, 
         metadata
      );

      return new AccountTransactionDetails(effect, source, description, fees);
   }

   // foreign withholding tax as a separate transaction (ledger import)
   public static AccountTransactionDetails createForeignWithholdingTaxTransaction(
      MonetaryAmount taxAmount, // negative amout (tax paid)
      String sourceCountry,
      String relatedTransactionId, // link to original dividend
      TransactionSource source,
      String description,
      List<Fee> fees
   ) {
      if (taxAmount.isPositive()) {
         throw new IllegalArgumentException("Withholding tax amoutn must be negative (represents tax paid).");
      }

      Map<String, String> metadata = Map.of(
         AccountMetadataKey.TAX_YEAR.getKey(), String.valueOf(LocalDate.now().getYear()),
         AccountMetadataKey.SOURCE_COUNTRY.getKey(), sourceCountry,
         AccountMetadataKey.FOREIGN_TAX_CREDIT_ELIGIBLE.getKey(), String.format("%s %s",taxAmount.abs().nativeAmount().amount().toString(), taxAmount.nativeAmount().currency()),
         AccountMetadataKey.RELATED_TRANSACTION_ID.getKey(), relatedTransactionId,
         AccountMetadataKey.TAX_TYPE.getKey(), "FOREIGN_WITHHOLDING"
      );

      AccountEffect effect = new AccountEffect(
         MonetaryAmount.ZERO(taxAmount.nativeAmount().currency()), // No gross effect for tax
         taxAmount, // Negative net effect (money leaving account)
         CashflowType.OTHER_OUTFLOW,
         metadata
      );

      return new AccountTransactionDetails(effect, source, description, fees);
   }

   public static AccountTransactionDetails createDepositTransactoin(
      MonetaryAmount depositAmount,
      TransactionSource source,
      String description,
      List<Fee> fees
   ) {
      AccountEffect effect = new AccountEffect(depositAmount, depositAmount, CashflowType.DEPOSIT, Collections.emptyMap());
      return new AccountTransactionDetails(effect, source, description, fees);
   }

   public static AccountTransactionDetails createWithdrawalTransaction(
      MonetaryAmount withdrawalAmount,
      TransactionSource source,
      String description,
      List<Fee> fees
   ) {
      MonetaryAmount negativeAmount = withdrawalAmount.negate();
      // we might need to do sometihing with the fees like a subtraction?
      AccountEffect effect = new AccountEffect(negativeAmount, negativeAmount, CashflowType.WITHDRAWAL, Collections.emptyMap());
      return new AccountTransactionDetails(effect, source, description, fees);
   }

   public static AccountTransactionDetails createInterestTransaction(
      MonetaryAmount interestAmount,
      TransactionSource source,
      String description
   ) {
      AccountEffect effect = new AccountEffect(interestAmount, interestAmount, CashflowType.INTEREST, Collections.emptyMap());
      return new AccountTransactionDetails(effect, source, description, Collections.emptyList());
   }

   public AccountEffect getAccountEffect() {
      return this.accountEffect;
   }
   
   public MonetaryAmount getNetWorthImpact() {
        return accountEffect.netAmount();
   }
   
   public boolean affectsNetWorth() {
      return !accountEffect.netAmount().nativeAmount().isZero();
   }
   
   public MonetaryAmount getCashFlow() {
      return accountEffect.netAmount();
   }
   
   public MonetaryAmount getTotalFees() {
      return accountEffect.getFeeAmount();
   }
   
   public boolean isIncomeTransaction() {
      return accountEffect.cashflowType() == CashflowType.DIVIDEND || 
            accountEffect.cashflowType() == CashflowType.INTEREST ||
            accountEffect.cashflowType() == CashflowType.RENTAL_INCOME ||
            accountEffect.cashflowType() == CashflowType.OTHER_INCOME;
   }
   
   public boolean isExpenseTransaction() {
      return accountEffect.cashflowType() == CashflowType.WITHDRAWAL || 
            accountEffect.cashflowType() == CashflowType.FEE ||
            accountEffect.cashflowType() == CashflowType.OTHER_OUTFLOW;
   }
   
   public boolean requiresTaxReporting() {
      return switch (accountEffect.cashflowType()) {
         case DIVIDEND, INTEREST, RENTAL_INCOME -> true;
         case OTHER_INCOME -> hasWithholdingTax();
         case WITHDRAWAL, FEE, OTHER_OUTFLOW, DEPOSIT, TRANSFER -> false;
         case UNKNOWN -> hasWithholdingTax();
         default -> false;
      };
   }
    
   public boolean isMultiCurrency() {
      return accountEffect.grossAmount().isMultiCurrency() || 
         accountEffect.netAmount().isMultiCurrency();
   }  
   
   // Reconciliation with external data
   public boolean matchesExternalAmount(MonetaryAmount externalAmount) {
      return accountEffect.grossAmount().equals(externalAmount) ||
            accountEffect.netAmount().equals(externalAmount);
   }

   // Helper methods
   private boolean hasWithholdingTax() {
      return accountEffect.metadata().containsKey(AccountMetadataKey.WITHHOLDING_TAX_RATE.getKey());
   } 
    
}
