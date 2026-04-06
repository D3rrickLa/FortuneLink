package com.laderrco.fortunelink.portfolio.application.queries;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GetAllAccountsQuery Unit Tests")
class GetAllAccountsQueryTest {

  private final PortfolioId portfolioId = PortfolioId.newId();
  private final UserId userId = UserId.random();

  @Test
  @DisplayName("Constructor: successfully creates query when all required fields provided")
  void createsQuerySuccessfully() {
    GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId, 1, 20);

    assertThat(query.portfolioId()).isEqualTo(portfolioId);
    assertThat(query.userId()).isEqualTo(userId);
    assertThat(query.page()).isEqualTo(1);
    assertThat(query.size()).isEqualTo(20);
  }

  @Test
  @DisplayName("Convenience Constructor: defaults page to 0 and size to 50")
  void convenienceConstructorSetsDefaultPagination() {
    GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId);

    assertThat(query.page()).isEqualTo(0);
    assertThat(query.size()).isEqualTo(50);
  }

  @Test
  @DisplayName("Validation: throws exception when portfolioId is null")
  void throwsExceptionWhenPortfolioIdIsNull() {
    assertThatThrownBy(() -> new GetAllAccountsQuery(null, userId, 0, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("PortfolioId is required");
  }

  @Test
  @DisplayName("Validation: throws exception when userId is null")
  void throwsExceptionWhenUserIdIsNull() {
    assertThatThrownBy(() -> new GetAllAccountsQuery(portfolioId, null, 0, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("UserId is required");
  }

  @Test
  @DisplayName("Validation: throws exception when page is negative")
  void throwsExceptionWhenPageIsNegative() {
    assertThatThrownBy(() -> new GetAllAccountsQuery(portfolioId, userId, -1, 10))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page cannot be negative");
  }

  @Test
  @DisplayName("Validation: throws exception when size is zero or negative")
  void throwsExceptionWhenSizeIsInvalid() {
    assertThatThrownBy(() -> new GetAllAccountsQuery(portfolioId, userId, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page size must be between 1 and 50");

    assertThatThrownBy(() -> new GetAllAccountsQuery(portfolioId, userId, 0, -5))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page size must be between 1 and 50");
  }

  @Test
  @DisplayName("Validation: throws exception when size exceeds 50")
  void throwsExceptionWhenSizeIsTooLarge() {
    assertThatThrownBy(() -> new GetAllAccountsQuery(portfolioId, userId, 0, 51))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Page size must be between 1 and 50");
  }

  @Test
  @DisplayName("pageable(): returns correctly configured Pageable object")
  void returnsCorrectPageable() {
    int page = 2;
    int size = 10;
    GetAllAccountsQuery query = new GetAllAccountsQuery(portfolioId, userId, page, size);

    Pageable pageable = query.pageable();

    assertThat(pageable.getPageNumber()).isEqualTo(page);
    assertThat(pageable.getPageSize()).isEqualTo(size);
    assertThat(pageable.getSort().getOrderFor("createdDate")).isNotNull();
    assertThat(pageable.getSort().getOrderFor("createdDate").getDirection())
        .isEqualTo(Sort.Direction.ASC);
  }
}