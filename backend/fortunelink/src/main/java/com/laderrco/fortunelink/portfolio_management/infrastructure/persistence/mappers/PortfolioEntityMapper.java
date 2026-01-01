package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AccountEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.AssetEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.PortfolioEntity;
import com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.entities.TransactionEntity;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;

import lombok.RequiredArgsConstructor;

// We are mapping/converting between domai nobjects, with has things like Money and Asset to 
// JPA entities whtih contains primitives only and vice versa
// The portfolio rpeo will call this, then the mapper, this, will delegate the heavy lifting to the other mappers if needs be (i.e. asset)

@Component
@RequiredArgsConstructor
public class PortfolioEntityMapper {
    private final AssetMapper assetMapper;
    private final TransactionEntityMapper txMapper; // Added injection

    public PortfolioEntity toEntity(Portfolio portfolio) {
        PortfolioEntity entity = new PortfolioEntity();
        entity.setId(portfolio.getPortfolioId().portfolioId()); 
        entity.setUserId(portfolio.getUserId().userId());
        entity.setCurrencyPreference(portfolio.getPortfolioCurrencyPreference().getSymbol());
        
        List<AccountEntity> accountEntities = portfolio.getAccounts().stream()
            .map(acc -> mapAccountToEntity(acc, entity)) 
            .collect(Collectors.toList());
            
        entity.setAccounts(accountEntities);
        return entity;
    }

    public Portfolio toDomain(PortfolioEntity entity) {
        List<Account> domainAccounts = entity.getAccounts().stream()
        .map(this::mapAccountToDomain)
        .toList();
        
        return Portfolio.reconstitute(
            new PortfolioId(entity.getId()),
            new UserId(entity.getUserId()),
            domainAccounts,
            ValidatedCurrency.of(entity.getCurrencyPreference()),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public void updateEntityFromDomain(Portfolio domain, PortfolioEntity entity) {
        entity.setUserId(domain.getUserId().userId());
        entity.setCurrencyPreference(domain.getPortfolioCurrencyPreference().getSymbol());
        
        // Handle the List update without breaking the reference
        if (entity.getAccounts() == null) {
            entity.setAccounts(new ArrayList<>());
        }
        // We update the collection of AccountEntities
        // Note: In a real app, you'd handle merging/deleting existing accounts here
        List<AccountEntity> accountEntities = domain.getAccounts().stream()
            .map(acc -> mapAccountToEntity(acc, entity))
            .toList();
            
        entity.getAccounts().clear();
        entity.getAccounts().addAll(accountEntities);
    }

    private AccountEntity mapAccountToEntity(Account account, PortfolioEntity portfolioEntity) {
        AccountEntity entity = new AccountEntity();
        entity.setId(account.getAccountId().accountId());
        entity.setName(account.getName());
        entity.setAccountType(account.getAccountType().name()); // Use .name() for enums
        entity.setBaseCurrency(account.getBaseCurrency().getSymbol());
        entity.setCashBalanceAmount(account.getCashBalance().amount());
        entity.setCashBalanceCurrency(account.getCashBalance().currency().getSymbol()); // Added missing currency set
        
        entity.setPortfolio(portfolioEntity); 

        // Map Assets
        List<AssetEntity> assetEntities = account.getAssets().stream()
            .map(asset -> assetMapper.toEntity(asset, entity))
            .collect(Collectors.toList());
        entity.setAssets(assetEntities);

        // Map Transactions (Crucial missing piece)
        List<TransactionEntity> txEntities = account.getTransactions().stream()
            .map(tx -> txMapper.toEntity(tx)) // Assuming txMapper has toEntity
            .peek(txEnt -> txEnt.setAccountId(entity.getId())) // Ensure FK link
            .collect(Collectors.toList());
        entity.setTransactions(txEntities);

        return entity;
    }

    public Account mapAccountToDomain(AccountEntity entity) {
        List<Asset> domainAssets = entity.getAssets().stream()
            .map(assetMapper::toDomain)
            .toList();
            
        List<Transaction> domainTransactions = entity.getTransactions().stream()
            .map(txMapper::toDomain)
            .toList();

        // Convert String from DB back to Domain Enum
        AccountType type = AccountType.valueOf(entity.getAccountType());

        return Account.reconstitute(
            new AccountId(entity.getId()),
            entity.getName(),
            type,
            ValidatedCurrency.of(entity.getBaseCurrency()),
            new Money(entity.getCashBalanceAmount(), ValidatedCurrency.of(entity.getCashBalanceCurrency())),
            domainAssets,
            domainTransactions,
            entity.isActive(),
            entity.getClosedDate(),
            entity.getCreatedAt(),
            entity.getLastUpdated()
        );
    }
}