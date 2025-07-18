package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactionaggregate;

import java.time.Instant;

import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

public final class LiabilityIncurrenceTransactionDetails extends TransactionDetails {
	private final String liabilityName;
	private final String description;
	private final Money originalLoanAmount;
	private final Money originalLoanAmountInPortfolioCurrency;
	private final Percentage annualInterestRate;
	private final Instant maturityDate;
	private final Money totalFeesInPortfolioCurrency;
	private final Money totalFeesInLiabilityCurrency;

	public LiabilityIncurrenceTransactionDetails(
		String description, 
		String liabilityName, 
		Money originalLoanAmount, 
		Money originalLoanAmountInPortfolioCurrency, 
		Percentage annualInterestRate,
		Instant maturityDate, 
		Money totalFeesInLiabilityCurrency, 
		Money totalFeesInPortfolioCurrency
	) {
		this.liabilityName = liabilityName;
		this.description = description;
		this.originalLoanAmount = originalLoanAmount;
		this.originalLoanAmountInPortfolioCurrency = originalLoanAmountInPortfolioCurrency;
		this.annualInterestRate = annualInterestRate;
		this.maturityDate = maturityDate;
		this.totalFeesInPortfolioCurrency = totalFeesInPortfolioCurrency;
		this.totalFeesInLiabilityCurrency = totalFeesInLiabilityCurrency;
	}

	public String getLiabilityName() {
		return liabilityName;
	}



	public String getDescription() {
		return description;
	}

	public Money getOriginalLoanAmount() {
		return originalLoanAmount;
	}

	public Money getOriginalLoanAmountInPortfolioCurrency() {
		return originalLoanAmountInPortfolioCurrency;
	}

	public Percentage getAnnualInterestRate() {
		return annualInterestRate;
	}

	public Instant getMaturityDate() {
		return maturityDate;
	}

	public Money getTotalFeesInPortfolioCurrency() {
		return totalFeesInPortfolioCurrency;
	}

	public Money getTotalFeesInLiabilityCurrency() {
		return totalFeesInLiabilityCurrency;
	}

	

}
