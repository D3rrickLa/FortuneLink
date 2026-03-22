package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.identifiers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.TransactionId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Identifier Tests: For Ids and stuff like that")
public class AllIdentifierTest {
  @Test
  @DisplayName("accountId: should round-trip successfully from string")
  void accountIdFromStringRoundTripSuccess() {
    AccountId accountId = AccountId.newId();
    AccountId fromId = AccountId.fromString(accountId.toString());
    assertEquals(accountId, fromId);
  }

  @Test
  @DisplayName("portfolioId: should round-trip successfully from string")
  void portfolioIdFromStringRoundTripSuccess() {
    PortfolioId id = PortfolioId.newId();
    PortfolioId fromId = PortfolioId.fromString(id.toString());
    assertEquals(id, fromId);
  }

  @Test
  @DisplayName("transactionId: should round-trip successfully from string")
  void transactionIdFromStringRoundTripSuccess() {
    TransactionId transactionId = TransactionId.newId();
    TransactionId fromId = TransactionId.fromString(transactionId.toString());
    assertEquals(transactionId, fromId);
  }

  @Test
  @DisplayName("userId: should round-trip successfully from string")
  void userIdFromStringRoundTripSuccess() {
    UserId userId = UserId.random();
    UserId fromId = UserId.fromString(userId.toString());
    assertEquals(userId, fromId);
  }

  @Nested
  @DisplayName("AssetSymbol Validation")
  class AssetSymbolTests {

    @Test
    @DisplayName("assetSymbol: successfully creates with valid ticker")
    void assetSymbolValidTickerSuccess() {
      AssetSymbol symbol = new AssetSymbol("AAPL");
      assertEquals("AAPL", symbol.value());
    }

    @Test
    @DisplayName("assetSymbol: throws exception for special characters")
    void assetSymbolInvalidCharactersThrowsException() {
      assertThrows(IllegalArgumentException.class, () -> new AssetSymbol("AAPL%$#("));
    }

    @Test
    @DisplayName("assetSymbol: throws exception when symbol exceeds length limit")
    void assetSymbolTooLongThrowsException() {
      String longSymbol = "A".repeat(50);
      assertThrows(IllegalArgumentException.class, () -> new AssetSymbol(longSymbol));
    }
  }
}
