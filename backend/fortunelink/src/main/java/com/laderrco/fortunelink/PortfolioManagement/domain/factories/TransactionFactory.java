package com.laderrco.fortunelink.portfoliomanagement.domain.factories;

// this class is meant for to be flexxible in terms of storing a specific transaction type (i.e. liability vs. asset transaction)
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagement.domain.entities.Transaction;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetIdentifier;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.AssetTransferDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.CashflowTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.CorporateActionTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Fee;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.LiabilityPaymentDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.TransactionMetadata;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.VoidTransactionDetails;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.DecimalPrecision;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionStatus;
import com.laderrco.fortunelink.portfoliomanagement.domain.valueobjects.Enums.TransactionType;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.Money;
import com.laderrco.fortunelink.sharedkernel.ValueObjects.PortfolioCurrency;

// Precision used in Money class and Quantities variables
// totalTransactionAmount should always reflect the net cash change: Positive for inflow, Negative for outflow.
/*
 * NOTE: so when we look at actual transaction in a brokage account, all values are positive expect for 'fees' or 'cash back'
 * this system, internal, deals with signed values which is more accurate in terms of money management. We would use a display layer
 * to actual show positive/abs values
 * TODO: FINAL all the variables in the headers, mainly the UUIDs ALSO we need to remove all logic in the methods, the builder is for making Transaction, no logic
 */
public class TransactionFactory {
     
}