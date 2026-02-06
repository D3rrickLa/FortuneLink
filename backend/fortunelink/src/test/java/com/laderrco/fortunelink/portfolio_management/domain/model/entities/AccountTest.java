package com.laderrco.fortunelink.portfolio_management.domain.model.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio_management.domain.model.enums.AccountType;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio_management.domain.model.valueobjects.identifiers.AssetSymbol;

public class AccountTest {

    @Test
    void testConstructor_isSuccessful() {
        AccountId accountId = AccountId.newId();
        AccountType type = AccountType.CHEQUING;
        String name = "My account";
        Currency currency = Currency.CAD;

        Account account = new Account(accountId, name, type, currency);
        assertAll(
                () -> assertEquals(accountId, account.getAccountId()),
                () -> assertEquals(name, account.getName()),
                () -> assertEquals(currency, account.getAccountCurrency()),
                () -> assertEquals(type, account.getAccountType()),
                () -> assertEquals(true, account.isActive()),
                () -> assertNull(account.getCloseDate()),
                () -> assertEquals(Optional.empty(), account.getPosition(new AssetSymbol("AAPL"))),
                () -> assertEquals(Collections.EMPTY_MAP, account.getPositions()));
    }

    @Test
    void testProtectedConstructor_isSuccessful() {
        Account account = new Account();
        assertAll(
                () -> assertNull(account.getAccountId()),
                () -> assertNull(account.getCreationDate()));
    }

    @Nested
    @DisplayName("Apply cash flow tests")
    public class ApplyCashFlowTests {
        AccountId accountId;
        AccountType type;
        String name;
        Currency currency;
        Account account;

        @BeforeEach
        void setup() {
            accountId = AccountId.newId();
            type = AccountType.CHEQUING;
            name = "My account";
            currency = Currency.CAD;
            account = new Account(accountId, name, type, currency);

        }

        @Test
        void testApplyCashFlowPositiveDeposit_Successful() {
            Money cash = Money.of(500, "CAD");
            String reason = "deposit";
            account.applyCashFlow(cash, reason);

            assertAll(
                    () -> assertEquals(cash, account.getCashBalance()));
        }

        @Test
        void testApplyCashFlowEdgeCase1_ZeroDeposit() {
            Money cash = Money.of(0, "CAD");
            account.applyCashFlow(cash, "widthdrawal");
            assertAll(
                    () -> assertEquals(cash, account.getCashBalance()));
        }

        @Test
        void testApplyCashFlowWithdrawal_Successful() {
            Money cash = Money.of(1000, "CAD");
            account.applyCashFlow(cash, "deposit");

            Money withdrawal = Money.of(-250, "CAD");
            account.applyCashFlow(withdrawal, "withdrawal");

            assertAll(
                    () -> assertEquals(cash.add(withdrawal), account.getCashBalance()));
        }

        @Test
        void testApplyCashFlowWithdrawal_Fails_NotEnoughCash() {
            Money cash = Money.of(1000, "CAD");
            account.applyCashFlow(cash, "deposit");

            Money withdrawal = Money.of(-1250, "CAD");
            Exception ex = assertThrows(IllegalStateException.class,
                    () -> account.applyCashFlow(withdrawal, "widthdrawal"));
            assertTrue(ex.getLocalizedMessage().contains("Insufficient cash balance"));

        }

    }
}
