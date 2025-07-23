package com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects;

import java.time.Instant;
import java.util.UUID;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Percentage;

public final class LiabilityIncurrenceTransactionDetails extends TransactionDetails {
	private final UUID liabilityId;
	private final String liabilityName;
	private final String description;
	private final Money originalLoanAmountInLiabilityCurrency;
	private final Money originalLoanAmountInPortfolioCurrency;
	private final Percentage annualInterestRate;
	private final Instant maturityDate;
	private final Money totalFeesInLiabilityCurrency;
	private final Money totalFeesInPortfolioCurrency;

	public LiabilityIncurrenceTransactionDetails(
		UUID liabilityId,
		String liabilityName, 
		String description, 
		Money originalLoanAmountInLiabilityCurrency, 
		Money originalLoanAmountInPortfolioCurrency, 
		Percentage annualInterestRate,
		Instant maturityDate, 
		Money totalFeesInLiabilityCurrency, 
		Money totalFeesInPortfolioCurrency
	) {
		this.liabilityId = liabilityId;
		this.liabilityName = liabilityName;
		this.description = description;
		this.originalLoanAmountInLiabilityCurrency = originalLoanAmountInLiabilityCurrency;
		this.originalLoanAmountInPortfolioCurrency = originalLoanAmountInPortfolioCurrency;
		this.annualInterestRate = annualInterestRate;
		this.maturityDate = maturityDate;
		this.totalFeesInLiabilityCurrency = totalFeesInLiabilityCurrency;
		this.totalFeesInPortfolioCurrency = totalFeesInPortfolioCurrency;
	}

	

	public String getLiabilityName() {
		return liabilityName;
	}

	public String getDescription() {
		return description;
	}

	public Money getOriginalLoanAmountInLiabilityCurrency() {
		return originalLoanAmountInLiabilityCurrency;
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

	public UUID getLiabilityId() {
		return liabilityId;
	}

	

}
