// this class is meant for to be flexxible in terms of storing a specific transaction type (i.e. liability vs. asset transaction)

package com.laderrco.fortunelink.PortfolioManagement.domain.factories;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.TransactionStatus;

import com.laderrco.fortunelink.PortfolioManagement.domain.Entities.Transaction;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetIdentifier;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.AssetTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.CashTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.LiabilityTransactionDetails;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionSource;
import com.laderrco.fortunelink.PortfolioManagement.domain.ValueObjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;

public class TransactionFactory {
    // include everything for transaction + things related to the
    // entityTransactionDetails
    
}