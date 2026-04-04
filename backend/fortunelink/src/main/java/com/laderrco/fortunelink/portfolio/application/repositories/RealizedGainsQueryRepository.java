package com.laderrco.fortunelink.portfolio.application.repositories;

import java.util.List;
import java.util.Optional;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;

public interface RealizedGainsQueryRepository {
    List<RealizedGainRecord> findByAccountId(AccountId accountId);
    List<RealizedGainRecord> findByAccountIdAndYear(AccountId accountId, int year);
    List<RealizedGainRecord> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol);
    List<RealizedGainRecord> findByAccountIdAndYearAndSymbol(AccountId accountId, int year, AssetSymbol symbol);

    // Lightweight, avoids loading the full account aggregate just for currency.
    Optional<String> findAccountCurrencyCode(AccountId accountId);
}