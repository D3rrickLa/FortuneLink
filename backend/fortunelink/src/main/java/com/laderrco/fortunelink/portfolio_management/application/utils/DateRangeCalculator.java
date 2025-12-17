package com.laderrco.fortunelink.portfolio_management.application.utils;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
// we honestly don't even use this, but might be needed
/**
 * Utility class for calculating date ranges commonly used in portfolio analysis.
 * Provides methods to calculate start dates for various time periods.
 * All methods return Instant representing the start of the period.
 * 
 * USE CASE EXAMPLE:
    DateRangeCalculator calculator = new DateRangeCalculator();

    // Get performance for last 30 days
    Instant startDate = calculator.getLastNDays(30);
    List<Transaction> recentTransactions = transactionRepo.findByDateRange(portfolioId, startDate, Instant.now());

    // Get year-to-date performance
    Instant ytdStart = calculator.getYearToDate();
 */
public class DateRangeCalculator {
    
    private final ZoneId zoneId;
    
    /**
     * Creates a DateRangeCalculator with UTC timezone.
     */
    public DateRangeCalculator() {
        this.zoneId = ZoneId.of("UTC");
    }
    
    /**
     * Creates a DateRangeCalculator with a specific timezone.
     * 
     * @param zoneId The timezone to use for calculations
     */
    public DateRangeCalculator(ZoneId zoneId) {
        this.zoneId = zoneId != null ? zoneId : ZoneId.of("UTC");
    }
    
    /**
     * Calculates the start of the current year (Year-To-Date).
     * Returns January 1st of the current year at 00:00:00.
     * 
     * @return Instant representing the start of the current year
     */
    public Instant getYearToDate() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime startOfYear = now
                .with(TemporalAdjusters.firstDayOfYear())
                .with(LocalTime.MIN);
        
        return startOfYear.toInstant();
    }
    
    /**
     * Calculates the date N days ago from now.
     * 
     * @param days Number of days to subtract (must be positive)
     * @return Instant representing the date N days ago
     * @throws IllegalArgumentException if days is negative
     */
    public Instant getLastNDays(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("Days must be positive, got: " + days);
        }
        
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }
    
    /**
     * Calculates the date N months ago from now.
     * Uses the same day of month if possible, otherwise uses the last valid day.
     * For example, Jan 31 minus 1 month = Dec 31 (not Jan 3).
     * 
     * @param months Number of months to subtract (must be positive)
     * @return Instant representing the date N months ago
     * @throws IllegalArgumentException if months is negative
     */
    public Instant getLastNMonths(int months) {
        if (months < 0) {
            throw new IllegalArgumentException("Months must be positive, got: " + months);
        }
        
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime nMonthsAgo = now.minusMonths(months);
        
        return nMonthsAgo.toInstant();
    }
    
    /**
     * Calculates the date N years ago from now.
     * Handles leap years correctly (e.g., Feb 29 minus 1 year = Feb 28).
     * 
     * @param years Number of years to subtract (must be positive)
     * @return Instant representing the date N years ago
     * @throws IllegalArgumentException if years is negative
     */
    public Instant getLastNYears(int years) {
        if (years < 0) {
            throw new IllegalArgumentException("Years must be positive, got: " + years);
        }
        
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime nYearsAgo = now.minusYears(years);
        
        return nYearsAgo.toInstant();
    }
    
    /**
     * Calculates the start of the current month (Month-To-Date).
     * Returns the 1st day of the current month at 00:00:00.
     * 
     * @return Instant representing the start of the current month
     */
    public Instant getMonthToDate() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime startOfMonth = now
                .with(TemporalAdjusters.firstDayOfMonth())
                .with(LocalTime.MIN);
        
        return startOfMonth.toInstant();
    }
    
    /**
     * Calculates the start of the current quarter (Quarter-To-Date).
     * Q1: Jan 1, Q2: Apr 1, Q3: Jul 1, Q4: Oct 1
     * 
     * @return Instant representing the start of the current quarter
     */
    public Instant getQuarterToDate() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        int currentMonth = now.getMonthValue();
        
        // Calculate which quarter we're in and get the first month of that quarter
        int quarterStartMonth = ((currentMonth - 1) / 3) * 3 + 1;
        
        ZonedDateTime startOfQuarter = now
                .withMonth(quarterStartMonth)
                .with(TemporalAdjusters.firstDayOfMonth())
                .with(LocalTime.MIN);
        
        return startOfQuarter.toInstant();
    }
    
    /**
     * Calculates the start of the current week (Week-To-Date).
     * Returns Monday of the current week at 00:00:00.
     * 
     * @return Instant representing the start of the current week
     */
    public Instant getWeekToDate() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime startOfWeek = now
                .with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .with(LocalTime.MIN);
        
        return startOfWeek.toInstant();
    }
    
    /**
     * Gets the configured timezone for this calculator.
     * 
     * @return The ZoneId used for calculations
     */
    public ZoneId getZoneId() {
        return zoneId;
    }
}
