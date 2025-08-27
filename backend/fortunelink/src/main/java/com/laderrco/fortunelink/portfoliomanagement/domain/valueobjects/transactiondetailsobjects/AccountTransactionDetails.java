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

   public AccountTransactionDetails(AccountEffect accountEffect, TransactionSource source, String description, List<Fee> fees){
    super(source, description, fees);
    this.accountEffect = accountEffect;
   }

   
    
}
