package com.laderrco.fortunelink.portfolio_management.infrastructure.persistence.mappers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Account;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Asset;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Portfolio;
import com.laderrco.fortunelink.portfolio_management.domain.models.entities.Transaction;
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
// Note: In a real app, you'd handle merging/deleting existing accounts here

@Component
@RequiredArgsConstructor
public class PortfolioEntityMapperImpl implements PortfolioEntityMapper {
    private final AssetEntityMapperImpl assetMapper;
    private final TransactionEntityMapperImpl txMapper;

    /**
     * Maps new Portfolio domain to entity (for initial creation)
     */
    @Override
    public PortfolioEntity toEntity(Portfolio portfolio) {
        Objects.requireNonNull(portfolio, "Portfolio cannot be null");

        PortfolioEntity entity = new PortfolioEntity();
        entity.setId(portfolio.getPortfolioId().portfolioId());
        entity.setUserId(portfolio.getUserId().userId());
        entity.setCurrencyPreference(portfolio.getPortfolioCurrencyPreference().getCode());

        // Map accounts with proper bidirectional relationship
        List<AccountEntity> accountEntities = portfolio.getAccounts().stream()
                .map(acc -> mapAccountToEntity(acc, entity))
                .collect(Collectors.toList());

        entity.setAccounts(accountEntities);
        return entity;
    }

    /**
     * Maps entity to domain (reconstitution from database)
     */
    @Override
    public Portfolio toDomain(PortfolioEntity entity) {
        Objects.requireNonNull(entity, "PortfolioEntity cannot be null");

        List<Account> domainAccounts = entity.getAccounts().stream()
                .map(this::mapAccountToDomain)
                .toList();

        return Portfolio.reconstitute(
                new PortfolioId(entity.getId()),
                new UserId(entity.getUserId()),
                domainAccounts,
                entity.getName(),
                ValidatedCurrency.of(entity.getCurrencyPreference()),
                entity.getDescription(),
                Optional.ofNullable(entity.getDeleted()).orElse(false), // NULL-safe: treat NULL as false
                entity.getDeletedAt(),
                new UserId(entity.getDeletedBy()),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    /**
     * Updates existing entity from domain state
     * Handles proper merging of accounts, assets, and transactions
     */
    @Override
    public void updateEntityFromDomain(Portfolio domain, PortfolioEntity entity) {
        Objects.requireNonNull(domain, "Domain portfolio cannot be null");
        Objects.requireNonNull(entity, "Entity cannot be null");

        // Update scalar fields
        entity.setUserId(domain.getUserId().userId());
        entity.setCurrencyPreference(domain.getPortfolioCurrencyPreference().getCode());

        // Merge accounts collection
        mergeAccounts(domain.getAccounts(), entity);
    }

    /**
     * Properly merges account collection to handle adds/updates/deletes
     */
    private void mergeAccounts(List<Account> domainAccounts, PortfolioEntity entity) {
        if (entity.getAccounts() == null) {
            entity.setAccounts(new ArrayList<>());
        }

        // Create lookup map of existing accounts by ID
        Map<UUID, AccountEntity> existingAccounts = entity.getAccounts().stream()
                .collect(Collectors.toMap(AccountEntity::getId, acc -> acc));

        // Track which accounts are still present
        Set<UUID> processedIds = new HashSet<>();

        List<AccountEntity> mergedAccounts = new ArrayList<>();

        for (Account domainAccount : domainAccounts) {
            UUID accountId = domainAccount.getAccountId().accountId();
            processedIds.add(accountId);

            AccountEntity accountEntity = existingAccounts.get(accountId);

            if (accountEntity != null) {
                // Update existing account
                updateAccountEntity(domainAccount, accountEntity);
                mergedAccounts.add(accountEntity);
            } else {
                // Add new account
                AccountEntity newAccount = mapAccountToEntity(domainAccount, entity);
                mergedAccounts.add(newAccount);
            }
        }

        // Remove accounts that are no longer in domain
        entity.getAccounts().clear();
        entity.getAccounts().addAll(mergedAccounts);
    }

    /**
     * Updates existing AccountEntity from domain state
     */
    private void updateAccountEntity(Account domain, AccountEntity entity) {
        entity.setName(domain.getName());
        entity.setAccountType(domain.getAccountType());
        entity.setBaseCurrency(domain.getBaseCurrency().getCode());
        entity.setCashBalanceAmount(domain.getCashBalance().amount());
        entity.setCashBalanceCurrency(domain.getCashBalance().currency().getCode());
        entity.setActive(domain.isActive());
        entity.setClosedDate(domain.getClosedDate());
        entity.setLastUpdated(domain.getLastSystemInteraction());

        // Merge assets
        mergeAssets(domain.getAssets(), entity);

        // Merge transactions
        mergeTransactions(domain.getTransactions(), entity);
    }

    /**
     * Merges asset collection for an account
     */
    private void mergeAssets(List<Asset> domainAssets, AccountEntity accountEntity) {
        if (accountEntity.getAssets() == null) {
            accountEntity.setAssets(new ArrayList<>());
        }

        Map<UUID, AssetEntity> existingAssets = accountEntity.getAssets().stream()
                .collect(Collectors.toMap(AssetEntity::getId, Function.identity()));

        // Track domain asset IDs
        Set<UUID> domainAssetIds = domainAssets.stream()
                .map(a -> a.getAssetId().assetId())
                .collect(Collectors.toSet());

        for (Asset domainAsset : domainAssets) {
            UUID assetId = domainAsset.getAssetId().assetId();
            AssetEntity assetEntity = existingAssets.get(assetId);

            if (assetEntity != null) {
                // Update existing asset
                assetMapper.updateEntityFromDomain(domainAsset, assetEntity);
            } else {
                // Create new asset
                AssetEntity newAsset = assetMapper.toEntity(domainAsset, accountEntity);
                accountEntity.getAssets().add(newAsset);
            }
        }

        // Remove assets that no longer exist in the domain
        accountEntity.getAssets().removeIf(
                assetEntity -> !domainAssetIds.contains(assetEntity.getId()));
    }

    /**
     * Merges transaction collection for an account
     * Note: Transactions are typically immutable - we usually only add new ones
     */
    private void mergeTransactions(List<Transaction> domainTransactions, AccountEntity accountEntity) {
        if (accountEntity.getTransactions() == null) {
            accountEntity.setTransactions(new ArrayList<>());
        }

        Map<UUID, TransactionEntity> existingTxs = accountEntity.getTransactions().stream()
                .collect(Collectors.toMap(TransactionEntity::getId, tx -> tx));

        List<TransactionEntity> mergedTxs = new ArrayList<>();

        for (Transaction domainTx : domainTransactions) {
            UUID txId = domainTx.getTransactionId().transactionId();
            TransactionEntity txEntity = existingTxs.get(txId);

            if (txEntity != null) {
                // Transaction already exists - keep it
                // In most cases, transactions are immutable after creation
                mergedTxs.add(txEntity);
            } else {
                // New transaction
                TransactionEntity newTx = txMapper.toEntity(domainTx, accountEntity);
                newTx.setPortfolioId(accountEntity.getPortfolio().getId());
                mergedTxs.add(newTx);
            }
        }

        accountEntity.getTransactions().clear();
        accountEntity.getTransactions().addAll(mergedTxs);
    }

    /**
     * Maps new Account domain to entity with proper bidirectional relationships
     */
    private AccountEntity mapAccountToEntity(Account account, PortfolioEntity portfolioEntity) {
        Objects.requireNonNull(account, "Account cannot be null");

        AccountEntity entity = new AccountEntity();
        entity.setId(account.getAccountId().accountId());
        entity.setName(account.getName());
        entity.setAccountType(account.getAccountType());
        entity.setBaseCurrency(account.getBaseCurrency().getCode());
        entity.setCashBalanceAmount(account.getCashBalance().amount());
        entity.setCashBalanceCurrency(account.getCashBalance().currency().getCode());
        entity.setActive(account.isActive());
        entity.setClosedDate(account.getClosedDate());

        // Set bidirectional parent relationship
        entity.setPortfolio(portfolioEntity);

        // Map child collections
        List<AssetEntity> assetEntities = account.getAssets().stream()
                .map(asset -> assetMapper.toEntity(asset, entity))
                .collect(Collectors.toList());
        entity.setAssets(assetEntities);

        List<TransactionEntity> txEntities = account.getTransactions().stream()
                .map(tx -> txMapper.toEntity(tx, entity))
                .collect(Collectors.toList());
        entity.setTransactions(txEntities);

        return entity;
    }

    /**
     * Maps AccountEntity to domain Account
     */
    private Account mapAccountToDomain(AccountEntity entity) {
        Objects.requireNonNull(entity, "AccountEntity cannot be null");

        List<Asset> domainAssets = entity.getAssets() != null
                ? entity.getAssets().stream()
                        .map(assetMapper::toDomain)
                        .toList()
                : Collections.emptyList();

        List<Transaction> domainTransactions = entity.getTransactions() != null
                ? entity.getTransactions().stream()
                        .map(txMapper::toDomain)
                        .toList()
                : Collections.emptyList();

        Money cashBalance = new Money(
                entity.getCashBalanceAmount(),
                ValidatedCurrency.of(entity.getCashBalanceCurrency()));

        boolean isNew = entity.getCreatedDate() == null; // new entity has no persisted timestamps

        if (isNew) {
            // Use createAccount factory if you have initial data
            return Account.createAccount(
                    new AccountId(entity.getId()),
                    entity.getName(),
                    entity.getAccountType(),
                    ValidatedCurrency.of(entity.getBaseCurrency()),
                    cashBalance,
                    domainAssets,
                    domainTransactions);
        }

        return Account.reconstitute(
                new AccountId(entity.getId()),
                entity.getName(),
                entity.getAccountType(),
                ValidatedCurrency.of(entity.getBaseCurrency()),
                cashBalance,
                domainAssets,
                domainTransactions,
                entity.isActive(),
                entity.getClosedDate(),
                entity.getCreatedDate(),
                entity.getLastUpdated());
    }
}