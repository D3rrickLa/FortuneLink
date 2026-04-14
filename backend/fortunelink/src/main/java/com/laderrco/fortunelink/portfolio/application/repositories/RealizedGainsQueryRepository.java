package com.laderrco.fortunelink.portfolio.application.repositories;

import com.laderrco.fortunelink.portfolio.application.utils.valueobjects.GainsAggregation;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.RealizedGainRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RealizedGainsQueryRepository {
  Page<RealizedGainRecord> findByAccountId(AccountId accountId, Pageable pageable);

  Page<RealizedGainRecord> findByAccountIdAndYear(AccountId accountId, int year, Pageable pageable);

  Page<RealizedGainRecord> findByAccountIdAndSymbol(AccountId accountId, AssetSymbol symbol,
      Pageable pageable);

  Page<RealizedGainRecord> findByAccountIdAndYearAndSymbol(AccountId accountId, int year,
      AssetSymbol symbol, Pageable pageable);

  GainsAggregation calculateTotals(AccountId accountId, Integer year, AssetSymbol symbol);

  // Lightweight, avoids loading the full account aggregate just for currency.
  Optional<String> findAccountCurrencyCode(AccountId accountId);
}