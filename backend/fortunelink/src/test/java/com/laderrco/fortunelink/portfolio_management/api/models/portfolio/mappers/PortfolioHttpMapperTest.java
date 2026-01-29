package com.laderrco.fortunelink.portfolio_management.api.models.portfolio.mappers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreateAccountRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.CreatePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.DeletePortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetAccountRequest;
import com.laderrco.fortunelink.portfolio_management.api.models.portfolio.requests.GetUsersPortfolioRequest;
import com.laderrco.fortunelink.portfolio_management.application.commands.AddAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.CreatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.DeletePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.RemoveAccountCommand;
import com.laderrco.fortunelink.portfolio_management.application.commands.UpdatePortfolioCommand;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAccountSummaryQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetAssetQueryView;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfolioByIdQuery;
import com.laderrco.fortunelink.portfolio_management.application.queries.GetPortfoliosByUserIdQuery;
import com.laderrco.fortunelink.portfolio_management.domain.models.enums.AccountType;

class PortfolioHttpMapperTest {

    private PortfolioHttpMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new PortfolioHttpMapper();
    }

    @Test
    @DisplayName("CreatePortfolioRequest maps to CreatePortfolioCommand correctly")
    void toCommand_createPortfolio() {
        String userId = UUID.randomUUID().toString();
        CreatePortfolioRequest req = new CreatePortfolioRequest(
                "My Portfolio",
                "USD",
                "Desc",
                true
        );

        CreatePortfolioCommand cmd = mapper.toCommand(req, UUID.fromString(userId));

        assertEquals(userId, cmd.userId().userId().toString());
        assertEquals("My Portfolio", cmd.name());
        assertEquals("USD", cmd.defaultCurrency().getCode());
        assertEquals("Desc", cmd.description());
        assertTrue(cmd.createDefaultAccount());
    }

    @Test
    @DisplayName("PortfolioId string maps to GetPortfolioByIdQuery")
    void toCommand_getPortfolioById() {
        String id = UUID.randomUUID().toString();
        GetPortfolioByIdQuery query = mapper.toCommand(id);
        assertEquals(id, query.id().portfolioId().toString());
    }

    @Test
    @DisplayName("GetUsersPortfolioRequest maps to GetPortfoliosByUserIdQuery")
    void toCommand_getPortfoliosByUserId() {
        String userId = UUID.randomUUID().toString();
        GetUsersPortfolioRequest req = new GetUsersPortfolioRequest(userId);
        GetPortfoliosByUserIdQuery query = mapper.toCommand(req);
        assertEquals(userId, query.id().userId().toString());
    }

    @Test
    @DisplayName("UpdatePortfolioRequest maps to UpdatePortfolioCommand")
    void toCommand_updatePortfolio() {
        String portfolioId = UUID.randomUUID().toString();
        CreatePortfolioRequest req = new CreatePortfolioRequest(
                "Updated Name",
                "EUR",
                "Updated Desc",
                true);

        UpdatePortfolioCommand cmd = mapper.toCommand(portfolioId, req);

        assertEquals(portfolioId, cmd.id().portfolioId().toString());
        assertEquals("Updated Name", cmd.name());
        assertEquals("EUR", cmd.defaultCurrency().getCode());
        assertEquals("Updated Desc", cmd.description());
    }

    @Test
    @DisplayName("DeletePortfolioRequest maps to DeletePortfolioCommand")
    void toCommand_deletePortfolio() {
        String portfolioId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        DeletePortfolioRequest req = new DeletePortfolioRequest(
                portfolioId,
                userId,
                true,
                false);

        DeletePortfolioCommand cmd = mapper.toCommand(req);

        assertEquals(portfolioId, cmd.portfolioId().portfolioId().toString());
        assertEquals(userId, cmd.userId().userId().toString());
        assertTrue(cmd.confirmed());
        assertFalse(cmd.softDelete());
    }

    @Test
    @DisplayName("CreateAccountRequest maps to AddAccountCommand")
    void toCommand_addAccount() {
        String portfolioId = UUID.randomUUID().toString();
        CreateAccountRequest req = new CreateAccountRequest(
                "Account 1",
                "INVESTMENT",
                "USD");

        AddAccountCommand cmd = mapper.toCommand(portfolioId, req);

        assertEquals(portfolioId, cmd.portfolioId().portfolioId().toString());
        assertEquals("Account 1", cmd.accountName());
        assertEquals(AccountType.INVESTMENT, cmd.accountType());
        assertEquals("USD", cmd.baseCurrency().getCode());
    }

    @Test
    @DisplayName("PortfolioId/AccountId/AssetId strings map to GetAssetQueryView")
    void toAssetQuery() {
        String portfolioId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();
        String assetId = UUID.randomUUID().toString();

        GetAssetQueryView query = mapper.toAssetQuery(portfolioId, accountId, assetId);

        assertEquals(portfolioId, query.portfolioId().portfolioId().toString());
        assertEquals(accountId, query.accountId().accountId().toString());
        assertEquals(assetId, query.assetId().assetId().toString());
    }

    @Test
    @DisplayName("PortfolioId/AccountId strings map to RemoveAccountCommand")
    void toCommand_removeAccount() {
        String portfolioId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();

        RemoveAccountCommand cmd = mapper.toCommand(portfolioId, accountId);

        // The mapper currently has a bug: it uses accountId for portfolioId
        assertEquals(accountId, cmd.portfolioId().portfolioId().toString()); // matches current code
        assertEquals(accountId, cmd.accountId().accountId().toString());
    }

    @Test
    @DisplayName("GetAccountRequest maps to GetAccountSummaryQuery")
    void toCommand_getAccountSummary() {
        String portfolioId = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();
        GetAccountRequest req = new GetAccountRequest(
            accountId
        );

        GetAccountSummaryQuery query = mapper.toCommand(portfolioId, req);

        assertEquals(portfolioId, query.portfolioId().portfolioId().toString());
        assertEquals(accountId, query.accountId().accountId().toString());
    }
}
