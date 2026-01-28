package com.laderrco.fortunelink.portfolio_management.application.utils;

/**
 * Utility class for pagination operations.
 * Provides methods to calculate offsets, validate parameters, and create page information.
 * 
 * USE CASE EXAMPLE:
    // Basic offset calculation
    int offset = PaginationHelper.calculateOffset(2, 20); // Returns 40

    // Validate before processing
    PaginationHelper.validatePageParameters(pageNum, pageSize);

    // Get complete metadata for API response
    PageMetadata metadata = PaginationHelper.createPageMetadata(150, 2, 20);
    // metadata.totalPages() = 8
    // metadata.hasNext() = true
    // metadata.itemsOnPage() = 20

    // Create human-readable info
    String info = PaginationHelper.createPageInfo(150, 2, 20);
    // "Page 3 of 8 (showing 20 of 150 items, page size: 20, hasNext: true, hasPrevious: true)"
 */
public class PaginationHelper {
    
    // Constants for pagination limits
    private static final int MIN_PAGE_NUMBER = 0;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;
    
    private PaginationHelper() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Calculates the offset for database queries based on page number and size.
     * Uses zero-based page numbering (page 0 is the first page).
     * 
     * Formula: offset = pageNumber * pageSize
     * 
     * @param pageNumber The page number (zero-based)
     * @param pageSize The number of items per page
     * @return The offset value for the query
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static int calculateOffset(int pageNumber, int pageSize) {
        validatePageParameters(pageNumber, pageSize);
        return pageNumber * pageSize;
    }
    
    /**
     * Validates pagination parameters.
     * 
     * @param pageNumber The page number to validate (must be >= 0)
     * @param pageSize The page size to validate (must be between 1 and MAX_PAGE_SIZE)
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static void validatePageParameters(int pageNumber, int pageSize) {
        if (pageNumber < MIN_PAGE_NUMBER) {
            throw new IllegalArgumentException(
                "Page number must be greater than or equal to " + MIN_PAGE_NUMBER + ", got: " + pageNumber
            );
        }
        
        if (pageSize < MIN_PAGE_SIZE) {
            throw new IllegalArgumentException(
                "Page size must be at least " + MIN_PAGE_SIZE + ", got: " + pageSize
            );
        }
        
        if (pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                "Page size cannot exceed " + MAX_PAGE_SIZE + ", got: " + pageSize
            );
        }
    }
    
    /**
     * Creates a formatted page information string with metadata.
     * Returns JSON-like string with page details.
     * 
     * @param total Total number of items across all pages
     * @param pageNumber Current page number (zero-based)
     * @param pageSize Number of items per page
     * @return Formatted string with page information
     */
    public static String createPageInfo(int total, int pageNumber, int pageSize) {
        validatePageParameters(pageNumber, pageSize);
        
        if (total < 0) {
            throw new IllegalArgumentException("Total must be non-negative, got: " + total);
        }
        
        int totalPages = calculateTotalPages(total, pageSize);
        int itemsOnCurrentPage = calculateItemsOnPage(total, pageNumber, pageSize);
        boolean hasNext = hasNextPage(pageNumber, totalPages);
        boolean hasPrevious = hasPreviousPage(pageNumber);
        
        return String.format(
            "Page %d of %d (showing %d of %d items, page size: %d, hasNext: %s, hasPrevious: %s)",
            pageNumber + 1, // Display as 1-based for humans
            totalPages,
            itemsOnCurrentPage,
            total,
            pageSize,
            hasNext,
            hasPrevious
        );
    }
    
    /**
     * Calculates the total number of pages needed for the given total items.
     * 
     * @param total Total number of items
     * @param pageSize Number of items per page
     * @return Total number of pages
     */
    public static int calculateTotalPages(int total, int pageSize) {
        if (total <= 0) {
            return 0;
        }
        
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        
        // Ceiling division: (total + pageSize - 1) / pageSize
        return (total + pageSize - 1) / pageSize;
    }
    
    /**
     * Calculates how many items are on the current page.
     * 
     * @param total Total number of items
     * @param pageNumber Current page number (zero-based)
     * @param pageSize Number of items per page
     * @return Number of items on the current page
     */
    public static int calculateItemsOnPage(int total, int pageNumber, int pageSize) {
        if (total <= 0) {
            return 0;
        }
        
        int offset = calculateOffset(pageNumber, pageSize);
        
        // If offset is beyond total, no items on this page
        if (offset >= total) {
            return 0;
        }
        
        // Calculate remaining items from offset
        int remainingItems = total - offset;
        
        // Return the minimum of page size or remaining items
        return Math.min(pageSize, remainingItems);
    }
    
    /**
     * Checks if there is a next page available.
     * 
     * @param pageNumber Current page number (zero-based)
     * @param totalPages Total number of pages
     * @return true if there is a next page, false otherwise
     */
    public static boolean hasNextPage(int pageNumber, int totalPages) {
        return pageNumber + 1 < totalPages;
    }
    
    /**
     * Checks if there is a previous page available.
     * 
     * @param pageNumber Current page number (zero-based)
     * @return true if there is a previous page, false otherwise
     */
    public static boolean hasPreviousPage(int pageNumber) {
        return pageNumber > 0;
    }
    
    /**
     * Creates a PageMetadata object with all pagination information.
     * 
     * @param total Total number of items
     * @param pageNumber Current page number (zero-based)
     * @param pageSize Number of items per page
     * @return PageMetadata object containing all pagination info
     */
    public static PageMetadata createPageMetadata(int total, int pageNumber, int pageSize) {
        validatePageParameters(pageNumber, pageSize);
        
        if (total < 0) {
            throw new IllegalArgumentException("Total must be non-negative, got: " + total);
        }
        
        int totalPages = calculateTotalPages(total, pageSize);
        int offset = calculateOffset(pageNumber, pageSize);
        int itemsOnPage = calculateItemsOnPage(total, pageNumber, pageSize);
        boolean hasNext = hasNextPage(pageNumber, totalPages);
        boolean hasPrevious = hasPreviousPage(pageNumber);
        
        return new PageMetadata(
            pageNumber,
            pageSize,
            totalPages,
            total,
            offset,
            itemsOnPage,
            hasNext,
            hasPrevious
        );
    }
    
    /**
     * Gets the default page size.
     * 
     * @return Default page size
     */
    public static int getDefaultPageSize() {
        return DEFAULT_PAGE_SIZE;
    }
    
    /**
     * Gets the maximum allowed page size.
     * 
     * @return Maximum page size
     */
    public static int getMaxPageSize() {
        return MAX_PAGE_SIZE;
    }
    
    /**
     * Normalizes page size to be within valid bounds.
     * If size is too small, returns minimum. If too large, returns maximum.
     * 
     * @param pageSize The requested page size
     * @return Normalized page size within valid bounds
     */
    public static int normalizePageSize(int pageSize) {
        if (pageSize < MIN_PAGE_SIZE) {
            return MIN_PAGE_SIZE;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            return MAX_PAGE_SIZE;
        }
        return pageSize;
    }
    
    /**
     * Record class containing all pagination metadata.
     */
    public record PageMetadata(
        int pageNumber,
        int pageSize,
        int totalPages,
        int totalItems,
        int offset,
        int itemsOnPage,
        boolean hasNext,
        boolean hasPrevious
    ) {
        /**
         * Checks if the current page is the first page.
         * @return true if first page
         */
        public boolean isFirstPage() {
            return pageNumber == 0;
        }
        
        /**
         * Checks if the current page is the last page.
         * @return true if last page
         */
        public boolean isLastPage() {
            return !hasNext;
        }
        
        /**
         * Gets the next page number, or -1 if there is no next page.
         * @return next page number or -1
         */
        public int getNextPageNumber() {
            return hasNext ? pageNumber + 1 : -1;
        }
        
        /**
         * Gets the previous page number, or -1 if there is no previous page.
         * @return previous page number or -1
         */
        public int getPreviousPageNumber() {
            return hasPrevious ? pageNumber - 1 : -1;
        }
    }
}