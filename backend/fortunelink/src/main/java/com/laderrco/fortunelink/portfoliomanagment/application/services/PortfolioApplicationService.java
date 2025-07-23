package com.laderrco.fortunelink.portfoliomanagment.application.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.laderrco.fortunelink.portfoliomanagment.application.commands.AccrueInterestCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordAssetAcquisitionCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordAssetDisposalCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordCashDepositCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordCashWithdrawalCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordLiabilityPaymentCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.RecordNewLiabilityCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.commands.ReverseTransactionCommand;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.AssetAllocationDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.LiabilityCreatedDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.MoneyDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.PortfolioCreatedDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.PortfolioDetailsDto;
import com.laderrco.fortunelink.portfoliomanagment.application.dtos.TransactionConfirmationDto;
import com.laderrco.fortunelink.portfoliomanagment.application.exceptions.PortfolioNotFoundException;
import com.laderrco.fortunelink.portfoliomanagment.application.queries.GetAssetAllocationQuery;
import com.laderrco.fortunelink.portfoliomanagment.application.queries.GetPortfolioDetailsQuery;
import com.laderrco.fortunelink.portfoliomanagment.application.queries.GetTotalPortfolioValueQuery;
import com.laderrco.fortunelink.portfoliomanagment.application.queries.GetUnrealizedGainsQuery;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.AssetHolding;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Liability;
import com.laderrco.fortunelink.portfoliomanagment.domain.entities.Portfolio;
import com.laderrco.fortunelink.portfoliomanagment.domain.repositories.PortfolioRepository;
import com.laderrco.fortunelink.portfoliomanagment.domain.services.ExchangeRateService;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.AssetAllocation;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.CommonTransactionInput;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.Money;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.transactiondetailsobjects.AssetTransactionDetails;

import lombok.AllArgsConstructor;

// changes system's state

@Service
@AllArgsConstructor
public class PortfolioApplicationService {
    private final PortfolioRepository portfolioRepository;
    private final ExchangeRateService exchangeRateService;

    @Transactional
    public PortfolioCreatedDto handleCreatePortfolio(CreatePortfolioCommand command) {
        Portfolio portfolio = new Portfolio(
            UUID.randomUUID(), 
            command.userId(), 
            command.name(),
            command.description(), 
            command.initialBalance(), 
            command.currencyPerference(), 
            exchangeRateService
        );
        portfolioRepository.save(portfolio);
        return new PortfolioCreatedDto(portfolio.getPortfolioId(), portfolio.getPortfolioName());

    }

    public TransactionConfirmationDto handleRecordAssetAcquisition(RecordAssetAcquisitionCommand command) {
        Portfolio portfolio = portfolioRepository.findById(command.portfolioId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId().toString()));
        
        Money purchasePriceInPortfolioCurrency = exchangeRateService.convert(command.purchasePrice(), portfolio.getCurrencyPreference());

        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
            null, 
            null,
            purchasePriceInPortfolioCurrency,
            purchasePriceInPortfolioCurrency, 
            purchasePriceInPortfolioCurrency, 
            purchasePriceInPortfolioCurrency, 
            purchasePriceInPortfolioCurrency, 
            purchasePriceInPortfolioCurrency, 
            purchasePriceInPortfolioCurrency
        );

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            null,
            null,
            null,
            null, 
            null
        );

        portfolio.recordAssetPurchase(assetTransactionDetails, commonTransactionInput, command.transactionDate());
        portfolioRepository.save(portfolio);
        // You'll need to get the transaction ID from the Portfolio after the operation
        // Assuming recordAssetAcquisition returns the new TransactionId or adds it to Portfolio
        UUID newTransactionId = portfolio.getTransactions().get(portfolio.getTransactions().size()-1).getTransactionId(); // Example method
        return new TransactionConfirmationDto(newTransactionId, "Asset acquisition recorded successfully.");
    }

    public TransactionConfirmationDto handleRecordAssetDisposal(RecordAssetDisposalCommand command) {
        Portfolio portfolio = portfolioRepository.findById(command.portfolioId())
            .orElseThrow(() -> new PortfolioNotFoundException(command.portfolioId().toString()));

        Money salePriceInPortfolioCurrency = exchangeRateService.convert(command.salePrice(), portfolio.getCurrencyPreference());
        Money feesInPortfolioCurrency = exchangeRateService.convert(command.fees().iterator().next(), portfolio.getCurrencyPreference());

        AssetTransactionDetails assetTransactionDetails = new AssetTransactionDetails(
            null, 
            null,
            salePriceInPortfolioCurrency,
            salePriceInPortfolioCurrency, 
            salePriceInPortfolioCurrency, 
            salePriceInPortfolioCurrency, 
            salePriceInPortfolioCurrency, 
            salePriceInPortfolioCurrency, 
            salePriceInPortfolioCurrency
        );

        CommonTransactionInput commonTransactionInput = new CommonTransactionInput(
            null,
            null,
            null,
            null, 
            null
        );

        portfolio.recordAssetSale(assetTransactionDetails, commonTransactionInput, command.transactionDate());

        portfolioRepository.save(portfolio);
        UUID newTransactionId = portfolio.getTransactions().get(portfolio.getTransactions().size()-1).getTransactionId();
        return new TransactionConfirmationDto(newTransactionId, "Asset disposal recorded successfully.");
    }

    public TransactionConfirmationDto handleRecordCashDeposit(RecordCashDepositCommand command) {return null;}
    public TransactionConfirmationDto handleRecordCashWithdrawal(RecordCashWithdrawalCommand command) {return null;}
    public LiabilityCreatedDto handleRecordNewLiability(RecordNewLiabilityCommand command) {return null;}
    public TransactionConfirmationDto handleRecordLiabilityPayment(RecordLiabilityPaymentCommand command) {return null;}
    public TransactionConfirmationDto handleReverseTransaction(ReverseTransactionCommand command) {return null;}
    public TransactionConfirmationDto handleAccrueInterest(AccrueInterestCommand command) {return null;}
    public PortfolioDetailsDto handleGetPortfolioDetails(GetPortfolioDetailsQuery query) {return null;}
    public AssetAllocationDto handleGetAssetAllocation(GetAssetAllocationQuery query) {return null;}
    public MoneyDto  handleGetTotalPortfolioValue(GetTotalPortfolioValueQuery query) {return null;}
    public MoneyDto handleGetUnrealizedGains(GetUnrealizedGainsQuery query) {return null;}
}
