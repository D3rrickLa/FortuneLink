package com.laderrco.fortunelink.portfolio.application.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AssetSymbol;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class GetTransactionHistoryQueryTest {
  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final AccountId ACCOUNT_ID = AccountId.newId();
  private final AssetSymbol SYMBOL = new AssetSymbol("AAPL");

  @Nested
  @DisplayName("Constructor Validation")
  class ValidationTests {
    @Test
    @DisplayName("constructor: should throw exception when accountId is null")
    void shouldThrowWhenAccountIdIsNull() {
      assertThatThrownBy(
          () -> new GetTransactionHistoryQuery(PORTFOLIO_ID, USER_ID, null, SYMBOL, null, null, 0,
              20)).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("AccountId is required");
    }

    @Test
    @DisplayName("validatePagination: should throw exception when page is negative")
    void shouldThrowWhenPageIsNegative() {
      assertThatThrownBy(
          () -> new GetTransactionHistoryQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL, null,
              null, -1, 20)).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Page cannot be negative");
    }

    @Test
    @DisplayName("validatePagination: should throw exception when size is zero or negative")
    void shouldThrowWhenSizeIsInvalid() {

      assertThatThrownBy(
          () -> new GetTransactionHistoryQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL, null,
              null, 0, 0)).isInstanceOf(IllegalArgumentException.class);

      assertThatThrownBy(
          () -> new GetTransactionHistoryQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL, null,
              null, 0, -5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("validatePagination: should throw exception when size exceeds 100")
    void shouldThrowWhenSizeTooLarge() {
      assertThatThrownBy(
          () -> new GetTransactionHistoryQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL, null,
              null, 0, 101)).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Page size cannot exceed 100");
    }

    @Test
    @DisplayName("should succeed when parameters are valid")
    void shouldSucceedWithValidParams() {
      var query = new GetTransactionHistoryQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, SYMBOL, null,
          null, 0, 100);
      assertThat(query).isNotNull();
    }
  }

  @Nested
  @DisplayName("toPageable Conversion")
  class MappingTests {

    @Test
    @DisplayName("should correctly map to Spring Pageable with default sort")
    void shouldMapToPageable() {
      GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(PORTFOLIO_ID, USER_ID,
          ACCOUNT_ID, SYMBOL, null, null, 5, 50);

      Pageable pageable = query.toPageable();

      assertThat(pageable.getPageNumber()).isEqualTo(5);
      assertThat(pageable.getPageSize()).isEqualTo(50);
      assertThat(pageable.getSort().getOrderFor("occurredAt")).isNotNull();
      assertThat(pageable.getSort().getOrderFor("occurredAt").getDirection()).isEqualTo(
          Sort.Direction.DESC);
    }
  }
}
