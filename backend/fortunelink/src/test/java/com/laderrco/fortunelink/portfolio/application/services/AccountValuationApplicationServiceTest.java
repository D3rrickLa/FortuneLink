package com.laderrco.fortunelink.portfolio.application.services;

import static com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency.CAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.laderrco.fortunelink.portfolio.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio.application.repositories.AccountQueryRepository;
import com.laderrco.fortunelink.portfolio.application.views.AccountValuationView;
import com.laderrco.fortunelink.portfolio.domain.model.entities.Account;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.MarketAssetQuote;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.PercentageChange;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.services.MarketDataService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountValuationApplicationServiceTest {
  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID PORTFOLIO_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID ACCOUNT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  @Mock
  private AccountQueryRepository repository;

  @Mock
  private MarketDataService marketDataService;

  @InjectMocks
  private AccountValuationApplicationService service;

  @Test
  void shouldComputeAccountValuation() {

    Account account = mock(Account.class);
    when(account.getAccountCurrency()).thenReturn(CAD);

    Map<AssetSymbol, MarketAssetQuote> quote = new java.util.HashMap<>(Map.of());
    quote.put(new AssetSymbol("AAPL"),
        new MarketAssetQuote(new AssetSymbol("AAPL"), Price.of("100", CAD), Price.of("100", CAD),
            Price.of("100", CAD), Price.of("100", CAD), Price.of("100", CAD), PercentageChange.ZERO,
            BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TWO, "I made it up", Instant.now()));

    when(repository.findByIdWithDetails(any(), any(), any())).thenReturn(Optional.of(account));
    when(marketDataService.getBatchQuotes(any())).thenReturn(quote);
    when(repository.findByIdWithDetails(any(), any(), any())).thenReturn(Optional.of(account));

    AccountValuationView result = service.computeAccountValuation(
        new GetAccountSummaryQuery(new PortfolioId(PORTFOLIO_ID), new UserId(USER_UUID),
            new AccountId(ACCOUNT_ID)));

    assertThat(result.totalValue()).isNotNull();
    assertThat(result.currency().getCode()).isEqualTo("CAD");
  }
}