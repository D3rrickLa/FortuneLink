package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AccountHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.AssetHoldingHttpResponse;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.responses.PortfolioHttpResponse;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AccountView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.AssetView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioSummaryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.views.PortfolioView;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AssetType;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.AssetId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfolio_management.domain.models.valueobjects.ids.UserId;
import com.laderrco.fortunelink.shared.enums.ValidatedCurrency;
import com.laderrco.fortunelink.shared.valueobjects.Money;
import com.laderrco.fortunelink.shared.valueobjects.Percentage;

class PortfolioDtoMapperTest {

    private PortfolioDtoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PortfolioDtoMapper();
    }

    @Test
    @DisplayName("toPortfolioResponse(PortfolioView) maps all fields correctly")
    void toPortfolioResponse_portfolioView() {
        Instant now = Instant.now();

        AssetView asset = AssetView.builder()
                .assetId(AssetId.randomId())
                .symbol("AAPL")
                .type(AssetType.STOCK)
                .quantity(BigDecimal.TEN)
                .costBasis(new Money(BigDecimal.valueOf(100), ValidatedCurrency.USD))
                .averageCostPerUnit(Money.ZERO("USD"))
                .currentPrice(Money.ZERO("USD"))
                .currentValue(Money.ZERO("USD"))
                .currentValue(Money.ZERO("USD"))
                .unrealizedGain(Money.ZERO("USD"))
                .unrealizedGainPercentage(Percentage.of(1))
                .currentValue(Money.ZERO("USD"))
                .acquiredDate(now)
                .lastUpdated(now)
                .build();

        AccountView account = AccountView.builder()
                .accountId(AccountId.randomId())
                .name("Test Account")
                .type(AccountType.INVESTMENT)
                .baseCurrency(ValidatedCurrency.USD)
                .baseCurrency(ValidatedCurrency.USD)
                .cashBalance(Money.ZERO("USD"))
                .totalValue(Money.ZERO("USD"))
                .createdDate(Instant.now())
                .assets(List.of(asset))
                .build();

        PortfolioView portfolio = PortfolioView.builder()
                .portfolioId(PortfolioId.randomId())
                .userId(UserId.randomId())
                .name("My Portfolio")
                .description("Test portfolio")
                .accounts(List.of(account))
                .totalValue(new Money(BigDecimal.valueOf(1000), ValidatedCurrency.USD))
                .transactionCount(1)
                .createDate(now)
                .lastUpdated(now)
                .build();

        PortfolioHttpResponse response = mapper.toPortfolioResponse(portfolio);

        assertEquals(portfolio.portfolioId().toString(), response.id());
        assertEquals(portfolio.userId().toString(), response.userId());
        assertEquals(portfolio.name(), response.name());
        assertEquals(portfolio.description(), response.description());
        assertEquals(1, response.accounts().size());

        AccountHttpResponse acctResp = response.accounts().get(0);
        assertEquals(account.accountId().toString(), acctResp.id());
        assertEquals(response.id(), acctResp.portfolioId());
        assertEquals(account.name(), acctResp.name());
        assertEquals(account.type().name(), acctResp.accountType());

        assertEquals(1, acctResp.assets().size());
        AssetHoldingHttpResponse assetResp = acctResp.assets().get(0);
        assertEquals(asset.assetId().toString(), assetResp.id());
        assertEquals(asset.symbol(), assetResp.symbol());
        assertEquals(asset.type().toString(), assetResp.assetType());
        assertEquals(asset.quantity(), assetResp.quantity());
        assertEquals(asset.costBasis().amount(), assetResp.costBasis());
        assertEquals(LocalDateTime.ofInstant(asset.acquiredDate(), ZoneId.systemDefault()), assetResp.acquiredDate());
    }

    @Test
    @DisplayName("toPortfolioResponse(PortfolioSummaryView) maps summary correctly")
    void toPortfolioResponse_summaryView() {
        Instant now = Instant.now();

        PortfolioSummaryView summary = new PortfolioSummaryView(
                PortfolioId.randomId(),
                "My Portfolio",
                new Money(BigDecimal.valueOf(500), ValidatedCurrency.USD),
                now);

        PortfolioHttpResponse response = mapper.toPortfolioResponse(summary);

        assertEquals(summary.id().toString(), response.id());
        assertNull(response.userId());
        assertNull(response.name());
        assertNull(response.description());
        assertNull(response.accounts());
        assertEquals(summary.totalValue().amount(), response.totalValue());
        assertEquals(summary.totalValue().currency().getCode(), response.totalValueCurrency());
        assertEquals(LocalDateTime.ofInstant(summary.lastUpdated(), ZoneId.systemDefault()), response.lastUpdated());
        assertNull(response.createdDate());
    }

    @Test
    @DisplayName("toAccountResponse maps AccountView correctly")
    void toAccountResponse() {
        Instant now = Instant.now();
        AssetView asset = AssetView.builder()
                .assetId(AssetId.randomId())
                .symbol("TSLA")
                .type(AssetType.STOCK)
                .quantity(BigDecimal.valueOf(5))
                .costBasis(new Money(BigDecimal.valueOf(100), ValidatedCurrency.USD))
                .averageCostPerUnit(Money.ZERO("USD"))
                .currentPrice(Money.ZERO("USD"))
                .currentValue(Money.ZERO("USD"))
                .currentValue(Money.ZERO("USD"))
                .unrealizedGain(Money.ZERO("USD"))
                .unrealizedGainPercentage(Percentage.of(1))
                .currentValue(Money.ZERO("USD"))
                .acquiredDate(now)
                .lastUpdated(now)
                .build();

        AccountView account = AccountView.builder()
                .accountId(AccountId.randomId())
                .name("Account 1")
                .type(AccountType.INVESTMENT)
                .baseCurrency(ValidatedCurrency.USD)
                .cashBalance(Money.ZERO("USD"))
                .totalValue(Money.ZERO("USD"))
                .createdDate(Instant.now())
                .assets(List.of(asset))
                .build();

        AccountHttpResponse response = mapper.toAccountResponse("portfolio-1", account);

        assertEquals(account.accountId().toString(), response.id());
        assertEquals("portfolio-1", response.portfolioId());
        assertEquals(account.name(), response.name());
        assertEquals(account.type().name(), response.accountType());
        assertEquals(1, response.assets().size());

        AssetHoldingHttpResponse assetResp = response.assets().get(0);
        assertEquals(asset.assetId().toString(), assetResp.id());
        assertEquals(asset.symbol(), assetResp.symbol());
    }

    @Test
    @DisplayName("toAssetResponse maps AssetView correctly")
    void toAssetResponse() {
        Instant now = Instant.now();

        AssetView asset = AssetView.builder()
                .assetId(AssetId.randomId())
                .symbol("TSLA")
                .type(AssetType.STOCK)
                .quantity(BigDecimal.valueOf(5))
                .costBasis(new Money(BigDecimal.valueOf(600), ValidatedCurrency.USD))
                .averageCostPerUnit(Money.ZERO("USD"))
                .currentPrice(Money.ZERO("USD"))
                .currentValue(Money.ZERO("USD"))
                .currentValue(Money.ZERO("USD"))
                .unrealizedGain(Money.ZERO("USD"))
                .unrealizedGainPercentage(Percentage.of(1))
                .currentValue(Money.ZERO("USD"))
                .acquiredDate(now)
                .lastUpdated(now)
                .build();

        AssetHoldingHttpResponse response = mapper.toAssetResponse(asset);

        assertEquals(asset.assetId().toString(), response.id());
        assertEquals(asset.symbol(), response.symbol());
        assertEquals(asset.type().toString(), response.assetType());
        assertEquals(asset.quantity(), response.quantity());
        assertEquals(asset.costBasis().amount(), response.costBasis());
        assertEquals(LocalDateTime.ofInstant(now, ZoneId.systemDefault()), response.acquiredDate());
    }
}
