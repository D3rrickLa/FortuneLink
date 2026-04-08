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

@DisplayName("GetRealizedGainsQuery Unit Tests")
class GetRealizedGainsQueryTest {
  private final PortfolioId PORTFOLIO_ID = PortfolioId.newId();
  private final UserId USER_ID = UserId.random();
  private final AccountId ACCOUNT_ID = AccountId.newId();

  @Test
  @DisplayName("Constructor: successfully creates query when all required fields provided")
  void createsQuerySuccessfully() {
    GetRealizedGainsQuery query = new GetRealizedGainsQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, 2023,
        new AssetSymbol("AAPL"), -1, 20);

    assertThat(query.portfolioId()).isEqualTo(PORTFOLIO_ID);
    assertThat(query.taxYear()).isEqualTo(2023);
    assertThat(query.page()).isEqualTo(0);
    assertThat(query.size()).isEqualTo(20);
  }

  @Test
  @DisplayName("Constructor: allows null taxYear and symbol")
  void allowsNullOptionalFields() {
    GetRealizedGainsQuery query = new GetRealizedGainsQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, null,
        null, 0, 0);

    assertThat(query.taxYear()).isNull();
    assertThat(query.symbol()).isNull();
    assertThat(query.page()).isEqualTo(0);
    assertThat(query.size()).isEqualTo(20);
  }

  @Nested
  @DisplayName("Validation Failures")
  class ValidationTests {
    @Test
    @DisplayName("Constructor: throws exception when portfolioId is null")
    void throwsOnNullPortfolioId() {
      assertThatThrownBy(
          () -> new GetRealizedGainsQuery(null, USER_ID, ACCOUNT_ID, null, null, 0, 10))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("PortfolioId required");
    }

    @Test
    @DisplayName("Constructor: throws exception when userId is null")
    void throwsOnNullUserId() {
      assertThatThrownBy(
          () -> new GetRealizedGainsQuery(PORTFOLIO_ID, null, ACCOUNT_ID, null, null, 0, 10))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("UserId required");
    }

    @Test
    @DisplayName("Constructor: throws exception when accountId is null")
    void throwsOnNullAccountId() {
      assertThatThrownBy(
          () -> new GetRealizedGainsQuery(PORTFOLIO_ID, USER_ID, null, null, null, 0, 10))
          .isInstanceOf(IllegalArgumentException.class).hasMessage("AccountId required");
    }

    @Test
    @DisplayName("Constructor: caps maximum size to 100")
    void capsMaximumSize() {
      var query = new GetRealizedGainsQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, null, null, 0, 500);

      assertThat(query.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("toPageable: creates correct PageRequest with sorting")
    void createsCorrectPageRequest() {
      var query = new GetRealizedGainsQuery(PORTFOLIO_ID, USER_ID, ACCOUNT_ID, null, null, 2, 50);
      Pageable pageable = query.toPageable();

      assertThat(pageable.getPageNumber()).isEqualTo(2);
      assertThat(pageable.getPageSize()).isEqualTo(50);
      assertThat(pageable.getSort().getOrderFor("occurredAt")).isNotNull();
      assertThat(pageable.getSort().getOrderFor("occurredAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
  }
}