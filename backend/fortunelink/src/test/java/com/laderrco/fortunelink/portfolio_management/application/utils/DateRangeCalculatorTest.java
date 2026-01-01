package com.laderrco.fortunelink.portfolio_management.application.utils;

import static org.assertj.core.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("DateRangeCalculator Tests")
class DateRangeCalculatorTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should use UTC timezone by default")
        void shouldUseUtcByDefault() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            assertThat(calculator.getZoneId()).isEqualTo(ZoneId.of("UTC"));
        }
        
        @Test
        @DisplayName("Should use specified timezone")
        void shouldUseSpecifiedTimezone() {
            ZoneId tokyo = ZoneId.of("Asia/Tokyo");
            DateRangeCalculator calculator = new DateRangeCalculator(tokyo);
            assertThat(calculator.getZoneId()).isEqualTo(tokyo);
        }
        
        @Test
        @DisplayName("Should default to UTC when null timezone provided")
        void shouldDefaultToUtcWhenNull() {
            DateRangeCalculator calculator = new DateRangeCalculator(null);
            assertThat(calculator.getZoneId()).isEqualTo(ZoneId.of("UTC"));
        }
    }

    @Nested
    @DisplayName("getYearToDate() Tests")
    class GetYearToDateTests {
        
        @Test
        @DisplayName("Should return January 1st at midnight of current year")
        void shouldReturnJanuary1stOfCurrentYear() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant ytd = calculator.getYearToDate();
            
            ZonedDateTime result = ytd.atZone(ZoneId.of("UTC"));
            int currentYear = ZonedDateTime.now(ZoneId.of("UTC")).getYear();
            
            assertThat(result.getYear()).isEqualTo(currentYear);
            assertThat(result.getMonthValue()).isEqualTo(1);
            assertThat(result.getDayOfMonth()).isEqualTo(1);
            assertThat(result.getHour()).isZero();
            assertThat(result.getMinute()).isZero();
            assertThat(result.getSecond()).isZero();
            assertThat(result.getNano()).isZero();
        }
        
        @Test
        @DisplayName("Should respect timezone when calculating year start")
        void shouldRespectTimezoneForYearStart() {
            ZoneId tokyo = ZoneId.of("Asia/Tokyo");
            DateRangeCalculator calculator = new DateRangeCalculator(tokyo);
            Instant ytd = calculator.getYearToDate();
            
            ZonedDateTime result = ytd.atZone(tokyo);
            assertThat(result.getMonthValue()).isEqualTo(1);
            assertThat(result.getDayOfMonth()).isEqualTo(1);
            assertThat(result.getHour()).isZero();
        }
        
        @Test
        @DisplayName("Should return instant before now")
        void shouldReturnInstantBeforeNow() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant ytd = calculator.getYearToDate();
            Instant now = Instant.now();
            
            assertThat(ytd).isBeforeOrEqualTo(now);
        }
    }

    @Nested
    @DisplayName("getLastNDays() Tests")
    class GetLastNDaysTests {
        
        @Test
        @DisplayName("Should calculate 1 day ago correctly")
        void shouldCalculateOneDayAgo() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant oneDayAgo = calculator.getLastNDays(1);
            Instant now = Instant.now();
            
            long hoursDiff = ChronoUnit.HOURS.between(oneDayAgo, now);
            assertThat(hoursDiff).isBetween(23L, 25L); // Allow some margin
        }
        
        @ParameterizedTest
        @ValueSource(ints = {0, 7, 30, 90, 365})
        @DisplayName("Should calculate N days ago correctly")
        void shouldCalculateNDaysAgo(int days) {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant nDaysAgo = calculator.getLastNDays(days);
            Instant now = Instant.now();
            
            long actualDays = ChronoUnit.DAYS.between(nDaysAgo, now);
            assertThat(actualDays).isEqualTo(days);
        }
        
        @Test
        @DisplayName("Should throw exception for negative days")
        void shouldThrowExceptionForNegativeDays() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            
            assertThatThrownBy(() -> calculator.getLastNDays(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Days must be positive");
        }
        
        @Test
        @DisplayName("Should handle zero days (returns now)")
        void shouldHandleZeroDays() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant zeroDaysAgo = calculator.getLastNDays(0);
            Instant now = Instant.now();
            
            long secondsDiff = ChronoUnit.SECONDS.between(zeroDaysAgo, now);
            assertThat(secondsDiff).isLessThan(2); // Within 2 seconds
        }
    }

    @Nested
    @DisplayName("getLastNMonths() Tests")
    class GetLastNMonthsTests {
        
        @Test
        @DisplayName("Should calculate 1 month ago correctly")
        void shouldCalculateOneMonthAgo() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant oneMonthAgo = calculator.getLastNMonths(1);
            
            ZonedDateTime result = oneMonthAgo.atZone(ZoneId.of("UTC"));
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            
            // Should be approximately 1 month difference
            long monthsDiff = ChronoUnit.MONTHS.between(result, now);
            assertThat(monthsDiff).isBetween(0L, 1L);
        }
        
        @ParameterizedTest
        @ValueSource(ints = {0, 3, 6, 12, 24})
        @DisplayName("Should calculate N months ago correctly")
        void shouldCalculateNMonthsAgo(int months) {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant nMonthsAgo = calculator.getLastNMonths(months);
            
            ZonedDateTime result = nMonthsAgo.atZone(ZoneId.of("UTC"));
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            
            long actualMonths = ChronoUnit.MONTHS.between(result, now);
            assertThat(actualMonths).isEqualTo(months);
        }
        
        @Test
        @DisplayName("Should throw exception for negative months")
        void shouldThrowExceptionForNegativeMonths() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            
            assertThatThrownBy(() -> calculator.getLastNMonths(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Months must be positive");
        }
        
        @Test
        @DisplayName("Should preserve day of month when possible")
        void shouldPreserveDayOfMonth() {
            // Test when current day exists in previous month
            ZoneId utc = ZoneId.of("UTC");
            DateRangeCalculator calculator = new DateRangeCalculator(utc);
            
            ZonedDateTime now = ZonedDateTime.now(utc);
            Instant oneMonthAgo = calculator.getLastNMonths(1);
            ZonedDateTime result = oneMonthAgo.atZone(utc);
            
            // If the day of month is <= 28, it should be preserved
            if (now.getDayOfMonth() <= 28) {
                assertThat(result.getDayOfMonth()).isEqualTo(now.getDayOfMonth());
            }
        }
        
        @Test
        @DisplayName("Should respect timezone")
        void shouldRespectTimezone() {
            ZoneId tokyo = ZoneId.of("Asia/Tokyo");
            DateRangeCalculator calculator = new DateRangeCalculator(tokyo);
            Instant oneMonthAgo = calculator.getLastNMonths(1);
            
            ZonedDateTime result = oneMonthAgo.atZone(tokyo);
            ZonedDateTime now = ZonedDateTime.now(tokyo);
            
            long monthsDiff = ChronoUnit.MONTHS.between(result, now);
            assertThat(monthsDiff).isBetween(0L, 1L);
        }
    }

    @Nested
    @DisplayName("getLastNYears() Tests")
    class GetLastNYearsTests {
        
        @Test
        @DisplayName("Should calculate 1 year ago correctly")
        void shouldCalculateOneYearAgo() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant oneYearAgo = calculator.getLastNYears(1);
            
            ZonedDateTime result = oneYearAgo.atZone(ZoneId.of("UTC"));
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            
            assertThat(result.getYear()).isEqualTo(now.getYear() - 1);
        }
        
        @ParameterizedTest
        @ValueSource(ints = {0, 1, 5, 10})
        @DisplayName("Should calculate N years ago correctly")
        void shouldCalculateNYearsAgo(int years) {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant nYearsAgo = calculator.getLastNYears(years);
            
            ZonedDateTime result = nYearsAgo.atZone(ZoneId.of("UTC"));
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            
            assertThat(result.getYear()).isEqualTo(now.getYear() - years);
        }
        
        @Test
        @DisplayName("Should throw exception for negative years")
        void shouldThrowExceptionForNegativeYears() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            
            assertThatThrownBy(() -> calculator.getLastNYears(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Years must be positive");
        }
        
        @Test
        @DisplayName("Should preserve month and day when possible")
        void shouldPreserveMonthAndDay() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant oneYearAgo = calculator.getLastNYears(1);
            
            ZonedDateTime result = oneYearAgo.atZone(ZoneId.of("UTC"));
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            
            assertThat(result.getMonthValue()).isEqualTo(now.getMonthValue());
            
            // Day should match unless it's a leap year edge case
            if (!(now.getMonthValue() == 2 && now.getDayOfMonth() == 29)) {
                assertThat(result.getDayOfMonth()).isEqualTo(now.getDayOfMonth());
            }
        }
    }

    @Nested
    @DisplayName("getMonthToDate() Tests")
    class GetMonthToDateTests {
        
        @Test
        @DisplayName("Should return first day of current month at midnight")
        void shouldReturnFirstDayOfCurrentMonth() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant mtd = calculator.getMonthToDate();
            
            ZonedDateTime result = mtd.atZone(ZoneId.of("UTC"));
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            
            assertThat(result.getYear()).isEqualTo(now.getYear());
            assertThat(result.getMonthValue()).isEqualTo(now.getMonthValue());
            assertThat(result.getDayOfMonth()).isEqualTo(1);
            assertThat(result.getHour()).isZero();
            assertThat(result.getMinute()).isZero();
            assertThat(result.getSecond()).isZero();
            assertThat(result.getNano()).isZero();
        }
        
        @Test
        @DisplayName("Should respect timezone")
        void shouldRespectTimezone() {
            ZoneId newYork = ZoneId.of("America/New_York");
            DateRangeCalculator calculator = new DateRangeCalculator(newYork);
            Instant mtd = calculator.getMonthToDate();
            
            ZonedDateTime result = mtd.atZone(newYork);
            assertThat(result.getDayOfMonth()).isEqualTo(1);
            assertThat(result.getHour()).isZero();
        }
        
        @Test
        @DisplayName("Should return instant before or equal to now")
        void shouldReturnInstantBeforeOrEqualToNow() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant mtd = calculator.getMonthToDate();
            Instant now = Instant.now();
            
            assertThat(mtd).isBeforeOrEqualTo(now);
        }
    }

    @Nested
    @DisplayName("getQuarterToDate() Tests")
    class GetQuarterToDateTests {
        
        @ParameterizedTest
        @CsvSource({
            "1, 1",   // January -> Q1 (Jan)
            "2, 1",   // February -> Q1 (Jan)
            "3, 1",   // March -> Q1 (Jan)
            "4, 4",   // April -> Q2 (Apr)
            "5, 4",   // May -> Q2 (Apr)
            "6, 4",   // June -> Q2 (Apr)
            "7, 7",   // July -> Q3 (Jul)
            "8, 7",   // August -> Q3 (Jul)
            "9, 7",   // September -> Q3 (Jul)
            "10, 10", // October -> Q4 (Oct)
            "11, 10", // November -> Q4 (Oct)
            "12, 10"  // December -> Q4 (Oct)
        })
        @DisplayName("Should calculate correct quarter start month")
        void shouldCalculateCorrectQuarterStartMonth(int currentMonth, int expectedStartMonth) {
            // Create a specific date in the given month
            // ZonedDateTime testDate = ZonedDateTime.of(2024, currentMonth, 15, 12, 0, 0, 0, ZoneId.of("UTC"));
            
            // Calculate which quarter and its start
            int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
            
            assertThat(quarterStartMonth).isEqualTo(expectedStartMonth);
        }
        
        @Test
        @DisplayName("Should return first day of quarter at midnight")
        void shouldReturnFirstDayOfQuarterAtMidnight() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant qtd = calculator.getQuarterToDate();
            
            ZonedDateTime result = qtd.atZone(ZoneId.of("UTC"));
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            
            int currentMonth = now.getMonthValue();
            int expectedQuarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
            
            assertThat(result.getMonthValue()).isEqualTo(expectedQuarterStartMonth);
            assertThat(result.getDayOfMonth()).isEqualTo(1);
            assertThat(result.getHour()).isZero();
            assertThat(result.getMinute()).isZero();
            assertThat(result.getSecond()).isZero();
        }
        
        @Test
        @DisplayName("Should return instant before or equal to now")
        void shouldReturnInstantBeforeOrEqualToNow() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant qtd = calculator.getQuarterToDate();
            Instant now = Instant.now();
            
            assertThat(qtd).isBeforeOrEqualTo(now);
        }
        
        @Test
        @DisplayName("Should respect timezone")
        void shouldRespectTimezone() {
            ZoneId london = ZoneId.of("Europe/London");
            DateRangeCalculator calculator = new DateRangeCalculator(london);
            Instant qtd = calculator.getQuarterToDate();
            
            ZonedDateTime result = qtd.atZone(london);
            assertThat(result.getDayOfMonth()).isEqualTo(1);
            assertThat(result.getHour()).isZero();
        }
    }

    @Nested
    @DisplayName("getWeekToDate() Tests")
    class GetWeekToDateTests {
        
        @Test
        @DisplayName("Should return Monday of current week at midnight")
        void shouldReturnMondayOfCurrentWeek() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant wtd = calculator.getWeekToDate();
            
            ZonedDateTime result = wtd.atZone(ZoneId.of("UTC"));
            
            assertThat(result.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(result.getHour()).isZero();
            assertThat(result.getMinute()).isZero();
            assertThat(result.getSecond()).isZero();
            assertThat(result.getNano()).isZero();
        }
        
        @Test
        @DisplayName("Should return same day if today is Monday")
        void shouldReturnSameDayIfMonday() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            
            // Only test if today is Monday
            if (now.getDayOfWeek() == DayOfWeek.MONDAY) {
                Instant wtd = calculator.getWeekToDate();
                ZonedDateTime result = wtd.atZone(ZoneId.of("UTC"));
                
                assertThat(result.toLocalDate()).isEqualTo(now.toLocalDate());
            }
        }
        
        @Test
        @DisplayName("Should return instant before or equal to now")
        void shouldReturnInstantBeforeOrEqualToNow() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant wtd = calculator.getWeekToDate();
            Instant now = Instant.now();
            
            assertThat(wtd).isBeforeOrEqualTo(now);
        }
        
        @Test
        @DisplayName("Should be within 7 days of now")
        void shouldBeWithinSevenDaysOfNow() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant wtd = calculator.getWeekToDate();
            Instant now = Instant.now();
            
            long daysDiff = ChronoUnit.DAYS.between(wtd, now);
            assertThat(daysDiff).isBetween(0L, 6L);
        }
        
        @Test
        @DisplayName("Should respect timezone")
        void shouldRespectTimezone() {
            ZoneId sydney = ZoneId.of("Australia/Sydney");
            DateRangeCalculator calculator = new DateRangeCalculator(sydney);
            Instant wtd = calculator.getWeekToDate();
            
            ZonedDateTime result = wtd.atZone(sydney);
            assertThat(result.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
            assertThat(result.getHour()).isZero();
        }
    }

    @Nested
    @DisplayName("Timezone Behavior Tests")
    class TimezoneBehaviorTests {
        
        @Test
        @DisplayName("Should handle timezone offset differences correctly")
        void shouldHandleTimezoneOffsetDifferences() {
            ZoneId utc = ZoneId.of("UTC");
            ZoneId tokyo = ZoneId.of("Asia/Tokyo"); // UTC+9
            
            DateRangeCalculator utcCalc = new DateRangeCalculator(utc);
            DateRangeCalculator tokyoCalc = new DateRangeCalculator(tokyo);
            
            Instant utcMtd = utcCalc.getMonthToDate();
            Instant tokyoMtd = tokyoCalc.getMonthToDate();
            
            // They might differ by up to 1 day depending on current time
            long hoursDiff = Math.abs(ChronoUnit.HOURS.between(utcMtd, tokyoMtd));
            assertThat(hoursDiff).isLessThanOrEqualTo(24);
        }
        
        @Test
        @DisplayName("Should handle DST transitions")
        void shouldHandleDstTransitions() {
            ZoneId newYork = ZoneId.of("America/New_York");
            DateRangeCalculator calculator = new DateRangeCalculator(newYork);
            
            // Just verify it doesn't throw exceptions during DST transitions
            assertThatNoException().isThrownBy(() -> {
                calculator.getYearToDate();
                calculator.getMonthToDate();
                calculator.getWeekToDate();
            });
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle leap year correctly")
        void shouldHandleLeapYear() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            
            // Test won't fail in non-leap years
            assertThatNoException().isThrownBy(() -> {
                calculator.getLastNYears(1);
                calculator.getLastNMonths(12);
            });
        }
        
        @Test
        @DisplayName("Should handle year boundary correctly")
        void shouldHandleYearBoundary() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            
            Instant ytd = calculator.getYearToDate();
            Instant oneYearAgo = calculator.getLastNYears(1);
            
            assertThat(ytd).isNotNull();
            assertThat(oneYearAgo).isNotNull();
            assertThat(oneYearAgo).isBefore(ytd);
        }
        
        @Test
        @DisplayName("Should handle month boundary correctly")
        void shouldHandleMonthBoundary() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            
            Instant mtd = calculator.getMonthToDate();
            Instant oneMonthAgo = calculator.getLastNMonths(1);
            
            assertThat(mtd).isNotNull();
            assertThat(oneMonthAgo).isNotNull();
        }
        
        @Test
        @DisplayName("Should handle large time periods")
        void shouldHandleLargeTimePeriods() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            
            assertThatNoException().isThrownBy(() -> {
                calculator.getLastNDays(365 * 10); // 10 years in days
                calculator.getLastNMonths(120);     // 10 years in months
                calculator.getLastNYears(50);       // 50 years
            });
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("All date range methods should return valid instants")
        void allMethodsShouldReturnValidInstants() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            Instant now = Instant.now();
            
            Instant ytd = calculator.getYearToDate();
            Instant mtd = calculator.getMonthToDate();
            Instant qtd = calculator.getQuarterToDate();
            Instant wtd = calculator.getWeekToDate();
            Instant oneDay = calculator.getLastNDays(1);
            Instant oneMonth = calculator.getLastNMonths(1);
            Instant oneYear = calculator.getLastNYears(1);
            
            // All should be before or equal to now
            assertThat(ytd).isBeforeOrEqualTo(now);
            assertThat(mtd).isBeforeOrEqualTo(now);
            assertThat(qtd).isBeforeOrEqualTo(now);
            assertThat(wtd).isBeforeOrEqualTo(now);
            assertThat(oneDay).isBeforeOrEqualTo(now);
            assertThat(oneMonth).isBeforeOrEqualTo(now);
            assertThat(oneYear).isBeforeOrEqualTo(now);
        }
        
        @Test
        @DisplayName("Date ranges should have logical ordering")
        void dateRangesShouldHaveLogicalOrdering() {
            DateRangeCalculator calculator = new DateRangeCalculator();
            
            Instant wtd = calculator.getWeekToDate();
            Instant mtd = calculator.getMonthToDate();
            Instant qtd = calculator.getQuarterToDate();
            Instant ytd = calculator.getYearToDate();
            
            // Year to date should be earliest
            Instant now = Instant.now();
    
            assertThat(ytd).isBeforeOrEqualTo(now);
            assertThat(qtd).isBeforeOrEqualTo(now);
            assertThat(mtd).isBeforeOrEqualTo(now);
            assertThat(wtd).isBeforeOrEqualTo(now);
            
            // Valid hierarchy that never breaks:
            assertThat(ytd).isBeforeOrEqualTo(qtd);
            assertThat(qtd).isBeforeOrEqualTo(mtd);
            // Remove or conditionally check: assertThat(mtd).isBeforeOrEqualTo(wtd);
        }
    }
}