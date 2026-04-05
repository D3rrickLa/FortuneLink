package com.laderrco.fortunelink.portfolio.application.commands.records;

import com.laderrco.fortunelink.portfolio.application.utils.annotations.AdditionalInfoTransactionCommand;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Fee;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

// String symbol is fine, we are just recording the name, not the entity
// asset symbol - name
// asset entity - your identifier calss, the acutal holding

/**
 * @param assetType Client-supplied type hint. Used only when the symbol is not yet cached in
 *                  market_asset_info. The service validates and sanitizes this value — never pass
 *                  it directly to domain logic.
 * 
 * @param skipCashCheck true for historical/import flow, default is false
 */
@Builder
public record RecordPurchaseCommand(
    PortfolioId portfolioId,
    UserId userId,
    AccountId accountId,
    String symbol,
    AssetType assetType,
    Quantity quantity,
    Price price,
    List<Fee> fees,
    Instant transactionDate,
    String notes,
    boolean skipCashCheck) implements AdditionalInfoTransactionCommand {
  public Money totalFees(Currency currency) {
    return Fee.totalInAccountCurrency(fees, currency);
  }
}
