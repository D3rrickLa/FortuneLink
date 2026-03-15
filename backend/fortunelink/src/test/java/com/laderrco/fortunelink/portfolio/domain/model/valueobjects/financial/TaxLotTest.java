package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TaxLot Value Object Unit Tests")
class TaxLotTest {
	private final Instant ACQUIRED_DATE = Instant.parse("2023-01-01T10:00:00Z");
	private final Quantity TEN_SHARES = new Quantity(new BigDecimal("10.00"));
	private final Money THOUSAND_USD = Money.of(1000.00, "USD");

	@Nested
	@DisplayName("Creation and Validation")
	class CreationTests {

		@Test
		@DisplayName("constructor_success_ValidInitialization")
		void constructor_success_ValidInitialization() {
			TaxLot lot = new TaxLot(TEN_SHARES, THOUSAND_USD, ACQUIRED_DATE);
			assertThat(lot.quantity()).isEqualTo(TEN_SHARES);
			assertThat(lot.costBasis()).isEqualTo(THOUSAND_USD);
		}

		@Test
		@DisplayName("constructor_fail_NegativeCostBasis")
		void constructor_fail_NegativeCostBasis() {
			Money negativeMoney = Money.of(-100.00, "USD");
			assertThatThrownBy(() -> new TaxLot(TEN_SHARES, negativeMoney, ACQUIRED_DATE))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("Proportional Logic")
	class ProportionalTests {

		@Test
		@DisplayName("proportionalCost_success_PartialSaleMath")
		void proportionalCost_success_PartialSaleMath() {
			TaxLot lot = new TaxLot(TEN_SHARES, THOUSAND_USD, ACQUIRED_DATE);
			Quantity fourShares = new Quantity(new BigDecimal("4.00"));

			// 4/10 = 40% of $1000 = $400
			Money result = lot.proportionalCost(fourShares);
			assertThat(result.amount()).isEqualByComparingTo("400.00");
		}

		@Test
		@DisplayName("proportionalCost_success_ZeroQuantity")
		void proportionalCost_success_ZeroQuantity() {
			TaxLot lot = new TaxLot(TEN_SHARES, THOUSAND_USD, ACQUIRED_DATE);
			assertThat(lot.proportionalCost(Quantity.ZERO).isZero()).isTrue();
		}

		@Test
		@DisplayName("proportionalCost_fail_SaleExceedsLot")
		void proportionalCost_fail_SaleExceedsLot() {
			TaxLot lot = new TaxLot(TEN_SHARES, THOUSAND_USD, ACQUIRED_DATE);
			Quantity elevenShares = new Quantity(new BigDecimal("11.00"));

			assertThatThrownBy(() -> lot.proportionalCost(elevenShares))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("Lot Evolution")
	class EvolutionTests {

		@Test
		@DisplayName("remainingAfter_success_ReducesStateCorrectly")
		void remainingAfter_success_ReducesStateCorrectly() {
			TaxLot lot = new TaxLot(TEN_SHARES, THOUSAND_USD, ACQUIRED_DATE);
			Quantity fourShares = new Quantity(new BigDecimal("4.00"));

			TaxLot remaining = lot.remainingAfter(fourShares);

			assertThat(remaining.quantity().amount()).isEqualByComparingTo("6.00");
			assertThat(remaining.costBasis().amount()).isEqualByComparingTo("600.00");
			assertThat(remaining.acquiredDate()).isEqualTo(ACQUIRED_DATE); // Date persists
		}

		@Test
		void remainingAfter_success_quantityIs0() {
			TaxLot lot = new TaxLot(TEN_SHARES, THOUSAND_USD, ACQUIRED_DATE);
			Quantity fourShares = new Quantity(new BigDecimal("0.00"));

			TaxLot lot1 = lot.remainingAfter(fourShares);
			assertThat(lot1).isEqualTo(lot);
		}

		@Test
		void remainingAfter_fail_SoldQuantityGreaterThanQuantity() {
			TaxLot lot = new TaxLot(TEN_SHARES, THOUSAND_USD, ACQUIRED_DATE);
			Quantity fourShares = new Quantity(new BigDecimal("1000000.00"));

			assertThrows(IllegalArgumentException.class, () -> lot.remainingAfter(fourShares));
		}

		@Test
		@DisplayName("split_success_AdjustsQuantityOnly")
		void split_success_AdjustsQuantityOnly() {
			TaxLot lot = new TaxLot(TEN_SHARES, THOUSAND_USD, ACQUIRED_DATE);
			// 2-for-1 split (ratio 2.0)
			Ratio ratio = new Ratio(2, 1);
			TaxLot splitLot = lot.split(ratio);

			assertThat(splitLot.quantity().amount()).isEqualByComparingTo("20.00");
			assertThat(splitLot.costBasis()).isEqualTo(THOUSAND_USD); // Cost basis stays same in a split
		}
	}

	@Nested
	@DisplayName("Tax Calculations")
	class TaxTimingTests {

		@Test
		@DisplayName("isLongTerm_success_TrueAfterOneYear")
		void isLongTerm_success_TrueAfterOneYear() {
			TaxLot lot = new TaxLot(TEN_SHARES, THOUSAND_USD, ACQUIRED_DATE);

			Instant exactlyOneYear = ACQUIRED_DATE.plus(365, ChronoUnit.DAYS);
			Instant oneDayShort = ACQUIRED_DATE.plus(364, ChronoUnit.DAYS);

			assertThat(lot.isLongTerm(exactlyOneYear)).isTrue();
			assertThat(lot.isLongTerm(oneDayShort)).isFalse();
		}

		@Test
		@DisplayName("getHoldingPeriodDays_success_CalculatesRange")
		void getHoldingPeriodDays_success_CalculatesRange() {
			TaxLot lot = new TaxLot(TEN_SHARES, THOUSAND_USD, ACQUIRED_DATE);
			Instant tenDaysLater = ACQUIRED_DATE.plus(10, ChronoUnit.DAYS);

			assertThat(lot.getHoldingPeriodDays(tenDaysLater)).isEqualTo(10);
		}
	}
}