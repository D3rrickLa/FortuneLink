package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import org.junit.jupiter.api.*;

import com.laderrco.fortunelink.portfolio.domain.exceptions.CurrencyMismatchException;
import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;
import java.math.BigDecimal;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;

@DisplayName("ExchangeRate Value Object Unit Tests")
class ExchangeRateTest {
	private final Currency USD = Currency.of("USD");
	private final Currency EUR = Currency.of("EUR");
	private final Instant NOW = Instant.now();
	private final BigDecimal VALID_RATE = new BigDecimal("0.8500");

	@Nested
	@DisplayName("Constructor and Validation")
	class ConstructorTests {

		@Test
		@DisplayName("constructor_success_ValidInitialization")
		void constructor_success_ValidInitialization() {
			ExchangeRate rate = new ExchangeRate(USD, EUR, VALID_RATE, NOW);
			assertThat(rate.from()).isEqualTo(USD);
			assertThat(rate.to()).isEqualTo(EUR);
			// rate is set to FOREX scale automatically
			assertThat(rate.rate()).isEqualByComparingTo(VALID_RATE);
		}

		@Test
		@DisplayName("constructor_fail_NullParameters")
		void constructor_fail_NullParameters() {
			assertThatThrownBy(() -> new ExchangeRate(null, EUR, VALID_RATE, NOW))
					.isInstanceOf(DomainArgumentException.class);
		}

		@Test
		@DisplayName("constructor_fail_NonPositiveRate")
		void constructor_fail_NonPositiveRate() {
			assertThatThrownBy(() -> new ExchangeRate(USD, EUR, BigDecimal.ZERO, NOW))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("must be positive");

			assertThatThrownBy(() -> new ExchangeRate(USD, EUR, new BigDecimal("-1.5"), NOW))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	@Nested
	@DisplayName("Conversion Logic")
	class ConversionTests {

		@Test
		@DisplayName("convert_success_CorrectCalculation")
		void convert_success_CorrectCalculation() {
			ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("0.90"), NOW);
			Money hundredUsd = Money.of(100, "USD");

			Money result = rate.convert(hundredUsd);

			assertThat(result.currency()).isEqualTo(EUR);
			assertThat(result.amount()).isEqualByComparingTo("90.00");
		}

		@Test
		@DisplayName("convert_fail_CurrencyMismatch")
		void convert_fail_CurrencyMismatch() {
			ExchangeRate rate = new ExchangeRate(USD, EUR, VALID_RATE, NOW);
			Money gbpMoney = Money.of(100, "GBP");

			assertThatThrownBy(() -> rate.convert(gbpMoney))
					.isInstanceOf(CurrencyMismatchException.class);
		}

		@Test
		@DisplayName("convert_fail_NullMoney")
		void convert_fail_NullMoney() {
			ExchangeRate rate = new ExchangeRate(USD, EUR, VALID_RATE, NOW);
			assertThatThrownBy(() -> rate.convert(null))
					.isInstanceOf(DomainArgumentException.class);
		}
	}

	@Nested
	@DisplayName("Identity and Inverse")
	class IdentityAndInverseTests {

		@Test
		@DisplayName("identity_success_RateIsOne")
		void identity_success_RateIsOne() {
			ExchangeRate identity = ExchangeRate.identity(USD, NOW);

			assertThat(identity.from()).isEqualTo(USD);
			assertThat(identity.to()).isEqualTo(USD);
			assertThat(identity.rate()).isEqualByComparingTo(BigDecimal.ONE);
		}

		@Test
		@DisplayName("inverse_success_FlipsCurrenciesAndRate")
		void inverse_success_FlipsCurrenciesAndRate() {
			// Rate 2.0: 1 USD = 2 EUR
			ExchangeRate rate = new ExchangeRate(USD, EUR, new BigDecimal("2.0"), NOW);
			ExchangeRate inverse = rate.inverse();

			assertThat(inverse.from()).isEqualTo(EUR);
			assertThat(inverse.to()).isEqualTo(USD);
			// Inverse Rate: 1 / 2.0 = 0.5
			assertThat(inverse.rate()).isEqualByComparingTo("0.5");
		}
	}
}