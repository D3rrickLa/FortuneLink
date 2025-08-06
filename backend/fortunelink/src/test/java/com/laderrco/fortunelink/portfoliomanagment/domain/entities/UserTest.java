package com.laderrco.fortunelink.portfoliomanagment.domain.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Currency;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.PortfolioId;
import com.laderrco.fortunelink.portfoliomanagment.domain.valueobjects.ids.UserId;

public class UserTest {
    private final UserId userId = new UserId(UUID.randomUUID());
    private final String displayName = "Derrick";
    private final Currency currency = Currency.getInstance("USD");
    private final PortfolioId portfolioId1 = new PortfolioId(UUID.randomUUID());
    private final PortfolioId portfolioId2 = new PortfolioId(UUID.randomUUID());

    @Test
    void createNew_shouldInitializeUserWithEmptyPortfolioSet() {
        User user = User.createNew(userId, displayName, currency);

        assertEquals(userId, user.getId());
        assertEquals(displayName, user.getDisplayName());
        assertEquals(currency, user.getReportingCurrencyPreference());
        assertTrue(user.getPortfolioIds().isEmpty());
    }

    @Test
    void addPortfolio_shouldAddPortfolioIdToSet() {
        User user = User.createNew(userId, displayName, currency);
        user.addPortfolio(portfolioId1);

        assertTrue(user.getPortfolioIds().contains(portfolioId1));
    }

    @Test
    void removePortfolio_shouldRemovePortfolioIdFromSet() {
        User user = User.createNew(userId, displayName, currency);
        user.addPortfolio(portfolioId1);
        user.removePortfolio(portfolioId1);

        assertFalse(user.getPortfolioIds().contains(portfolioId1));
    }

    @Test
    void updateReportingCurrencyPreference_shouldChangeCurrency() {
        User user = User.createNew(userId, displayName, currency);
        Currency newCurrency = Currency.getInstance("EUR");

        user.updateReportingCurrencyPreference(newCurrency);

        assertEquals(newCurrency, user.getReportingCurrencyPreference());
    }

    @Test
    void updateDisplayName_shouldChangeName_whenValid() {
        User user = User.createNew(userId, displayName, currency);
        String newName = "D-Rock";

        user.updateDisplayName(newName);

        assertEquals(newName, user.getDisplayName());
    }

    @Test
    void updateDisplayName_shouldThrowException_whenBlank() {
        User user = User.createNew(userId, displayName, currency);

        Executable executable = () -> user.updateDisplayName("  ");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, executable);
        assertEquals("Display name cannot be blank", exception.getMessage());
    }

    @Test
    void addPortfolio_shouldThrowException_whenNull() {
        User user = User.createNew(userId, displayName, currency);

        Executable executable = () -> user.addPortfolio(null);
        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals("PortfolioId cannot be null", exception.getMessage());
    }

    @Test
    void removePortfolio_shouldThrowException_whenNull() {
        User user = User.createNew(userId, displayName, currency);

        Executable executable = () -> user.removePortfolio(null);
        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals("PortfolioId cannot be null", exception.getMessage());
    }

    @Test
    void updateReportingCurrencyPreference_shouldThrowException_whenNull() {
        User user = User.createNew(userId, displayName, currency);

        Executable executable = () -> user.updateReportingCurrencyPreference(null);
        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals("Reporting currency preference cannot be null", exception.getMessage());
    }

    @Test
    void updateDisplayName_shouldThrowException_whenNull() {
        User user = User.createNew(userId, displayName, currency);

        Executable executable = () -> user.updateDisplayName(null);
        NullPointerException exception = assertThrows(NullPointerException.class, executable);
        assertEquals("Display name cannot be null", exception.getMessage());
    }

    @Test
    void getPortfolioIds_shouldReturnUnmodifiableSet() {
        User user = User.createNew(userId, displayName, currency);
        user.addPortfolio(portfolioId1);

        Set<PortfolioId> ids = user.getPortfolioIds();
        assertThrows(UnsupportedOperationException.class, () -> ids.add(portfolioId2));
    }

    @Test
    void equalsAndHashCode_shouldBeBasedOnUserIdOnly() {
        UserId sameId = new UserId(userId.userId());
        User user1 = User.createNew(userId, displayName, currency);
        User user2 = User.createNew(sameId, "Another Name", Currency.getInstance("EUR"));

        assertEquals(user1, user2);
        assertEquals(user1.hashCode(), user2.hashCode());
    }

}
