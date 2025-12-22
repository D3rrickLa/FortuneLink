package com.laderrco.fortunelink.portfolio_management.application.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("PaginationHelper Tests")
class PaginationHelperTest {

    @Nested
    @DisplayName("calculateOffset() Tests")
    class CalculateOffsetTests {
        
        @Test
        @DisplayName("Should calculate offset correctly for first page")
        void shouldCalculateOffsetForFirstPage() {
            int offset = PaginationHelper.calculateOffset(0, 20);
            assertThat(offset).isEqualTo(0);
        }
        
        @ParameterizedTest
        @CsvSource({
            "0, 20, 0",
            "1, 20, 20",
            "2, 20, 40",
            "5, 10, 50",
            "10, 5, 50"
        })
        @DisplayName("Should calculate offset correctly for various pages")
        void shouldCalculateOffsetCorrectly(int pageNumber, int pageSize, int expectedOffset) {
            int offset = PaginationHelper.calculateOffset(pageNumber, pageSize);
            assertThat(offset).isEqualTo(expectedOffset);
        }
        
        @Test
        @DisplayName("Should throw exception for negative page number")
        void shouldThrowExceptionForNegativePageNumber() {
            assertThatThrownBy(() -> PaginationHelper.calculateOffset(-1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page number must be greater than or equal to 0");
        }
        
        @Test
        @DisplayName("Should throw exception for invalid page size")
        void shouldThrowExceptionForInvalidPageSize() {
            assertThatThrownBy(() -> PaginationHelper.calculateOffset(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be at least 1");
        }
    }

    @Nested
    @DisplayName("validatePageParameters() Tests")
    class ValidatePageParametersTests {
        
        @Test
        @DisplayName("Should pass validation for valid parameters")
        void shouldPassValidationForValidParameters() {
            assertThatNoException()
                .isThrownBy(() -> PaginationHelper.validatePageParameters(0, 20));
        }
        
        @Test
        @DisplayName("Should throw exception when page number is negative")
        void shouldThrowExceptionForNegativePageNumber() {
            assertThatThrownBy(() -> PaginationHelper.validatePageParameters(-1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page number must be greater than or equal to 0");
        }
        
        @ParameterizedTest
        @ValueSource(ints = {0, -1, -10})
        @DisplayName("Should throw exception for invalid page sizes")
        void shouldThrowExceptionForInvalidPageSize(int pageSize) {
            assertThatThrownBy(() -> PaginationHelper.validatePageParameters(0, pageSize))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be at least 1");
        }
        
        @Test
        @DisplayName("Should throw exception when page size exceeds maximum")
        void shouldThrowExceptionForPageSizeExceedingMax() {
            assertThatThrownBy(() -> PaginationHelper.validatePageParameters(0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size cannot exceed 100");
        }
        
        @Test
        @DisplayName("Should accept maximum page size")
        void shouldAcceptMaximumPageSize() {
            assertThatNoException()
                .isThrownBy(() -> PaginationHelper.validatePageParameters(0, 100));
        }
    }

    @Nested
    @DisplayName("calculateTotalPages() Tests")
    class CalculateTotalPagesTests {
        
        @ParameterizedTest
        @CsvSource({
            "0, 20, 0",
            "1, 20, 1",
            "19, 20, 1",
            "20, 20, 1",
            "21, 20, 2",
            "40, 20, 2",
            "41, 20, 3",
            "100, 10, 10",
            "99, 10, 10"
        })
        @DisplayName("Should calculate total pages correctly")
        void shouldCalculateTotalPagesCorrectly(int total, int pageSize, int expectedPages) {
            int totalPages = PaginationHelper.calculateTotalPages(total, pageSize);
            assertThat(totalPages).isEqualTo(expectedPages);
        }
        
        @Test
        @DisplayName("Should return 0 for negative total")
        void shouldReturnZeroForNegativeTotal() {
            int totalPages = PaginationHelper.calculateTotalPages(-1, 20);
            assertThat(totalPages).isZero();
        }
        
        @Test
        @DisplayName("Should throw exception for non-positive page size")
        void shouldThrowExceptionForNonPositivePageSize() {
            assertThatThrownBy(() -> PaginationHelper.calculateTotalPages(100, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Page size must be positive");
        }
    }

    @Nested
    @DisplayName("calculateItemsOnPage() Tests")
    class CalculateItemsOnPageTests {
        
        @ParameterizedTest
        @CsvSource({
            "100, 0, 20, 20",
            "100, 4, 20, 20",
            "100, 5, 20, 0",
            "95, 4, 20, 15",
            "50, 0, 20, 20",
            "15, 0, 20, 15",
            "0, 0, 20, 0"
        })
        @DisplayName("Should calculate items on page correctly")
        void shouldCalculateItemsOnPageCorrectly(int total, int pageNumber, int pageSize, int expectedItems) {
            int itemsOnPage = PaginationHelper.calculateItemsOnPage(total, pageNumber, pageSize);
            assertThat(itemsOnPage).isEqualTo(expectedItems);
        }
        
        @Test
        @DisplayName("Should return 0 when offset exceeds total")
        void shouldReturnZeroWhenOffsetExceedsTotal() {
            int itemsOnPage = PaginationHelper.calculateItemsOnPage(50, 10, 20);
            assertThat(itemsOnPage).isZero();
        }
    }

    @Nested
    @DisplayName("hasNextPage() Tests")
    class HasNextPageTests {
        
        @ParameterizedTest
        @CsvSource({
            "0, 5, true",
            "4, 5, false",
            "3, 5, true",
            "0, 1, false",
            "0, 0, false"
        })
        @DisplayName("Should determine next page availability correctly")
        void shouldDetermineNextPageCorrectly(int pageNumber, int totalPages, boolean expected) {
            boolean hasNext = PaginationHelper.hasNextPage(pageNumber, totalPages);
            assertThat(hasNext).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("hasPreviousPage() Tests")
    class HasPreviousPageTests {
        
        @ParameterizedTest
        @CsvSource({
            "0, false",
            "1, true",
            "5, true",
            "100, true"
        })
        @DisplayName("Should determine previous page availability correctly")
        void shouldDeterminePreviousPageCorrectly(int pageNumber, boolean expected) {
            boolean hasPrevious = PaginationHelper.hasPreviousPage(pageNumber);
            assertThat(hasPrevious).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("createPageInfo() Tests")
    class CreatePageInfoTests {
        
        @Test
        @DisplayName("Should create page info string with correct format")
        void shouldCreatePageInfoWithCorrectFormat() {
            String pageInfo = PaginationHelper.createPageInfo(100, 0, 20);
            
            assertThat(pageInfo)
                .contains("Page 1 of 5")
                .contains("showing 20 of 100 items")
                .contains("page size: 20")
                .contains("hasNext: true")
                .contains("hasPrevious: false");
        }
        
        @Test
        @DisplayName("Should handle last page correctly")
        void shouldHandleLastPageCorrectly() {
            String pageInfo = PaginationHelper.createPageInfo(95, 4, 20);
            
            assertThat(pageInfo)
                .contains("Page 5 of 5")
                .contains("showing 15 of 95 items")
                .contains("hasNext: false")
                .contains("hasPrevious: true");
        }
        
        @Test
        @DisplayName("Should throw exception for negative total")
        void shouldThrowExceptionForNegativeTotal() {
            assertThatThrownBy(() -> PaginationHelper.createPageInfo(-1, 0, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total must be non-negative");
        }
    }

    @Nested
    @DisplayName("createPageMetadata() Tests")
    class CreatePageMetadataTests {
        
        @Test
        @DisplayName("Should create complete metadata for first page")
        void shouldCreateCompleteMetadataForFirstPage() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(100, 0, 20);
            
            assertThat(metadata.pageNumber()).isZero();
            assertThat(metadata.pageSize()).isEqualTo(20);
            assertThat(metadata.totalPages()).isEqualTo(5);
            assertThat(metadata.totalItems()).isEqualTo(100);
            assertThat(metadata.offset()).isZero();
            assertThat(metadata.itemsOnPage()).isEqualTo(20);
            assertThat(metadata.hasNext()).isTrue();
            assertThat(metadata.hasPrevious()).isFalse();
        }
        
        @Test
        @DisplayName("Should create complete metadata for last page")
        void shouldCreateCompleteMetadataForLastPage() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(95, 4, 20);
            
            assertThat(metadata.pageNumber()).isEqualTo(4);
            assertThat(metadata.pageSize()).isEqualTo(20);
            assertThat(metadata.totalPages()).isEqualTo(5);
            assertThat(metadata.totalItems()).isEqualTo(95);
            assertThat(metadata.offset()).isEqualTo(80);
            assertThat(metadata.itemsOnPage()).isEqualTo(15);
            assertThat(metadata.hasNext()).isFalse();
            assertThat(metadata.hasPrevious()).isTrue();
        }
        
        @Test
        @DisplayName("Should create metadata for empty results")
        void shouldCreateMetadataForEmptyResults() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(0, 0, 20);
            
            assertThat(metadata.totalItems()).isZero();
            assertThat(metadata.totalPages()).isZero();
            assertThat(metadata.itemsOnPage()).isZero();
            assertThat(metadata.hasNext()).isFalse();
            assertThat(metadata.hasPrevious()).isFalse();
        }

        @Test
        void shouldThrowErrorIfTotalIsLessThan0() {
           assertThrows(IllegalArgumentException.class, ()->PaginationHelper.createPageMetadata(-1, 0, 20)); 
        }
    }

    @Nested
    @DisplayName("PageMetadata Methods Tests")
    class PageMetadataMethodsTests {
        
        @Test
        @DisplayName("Should identify first page correctly")
        void shouldIdentifyFirstPage() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(100, 0, 20);
            
            assertThat(metadata.isFirstPage()).isTrue();
            assertThat(metadata.isLastPage()).isFalse();
        }
        
        @Test
        @DisplayName("Should identify last page correctly")
        void shouldIdentifyLastPage() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(100, 4, 20);
            
            assertThat(metadata.isFirstPage()).isFalse();
            assertThat(metadata.isLastPage()).isTrue();
        }
        
        @Test
        @DisplayName("Should return correct next page number")
        void shouldReturnCorrectNextPageNumber() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(100, 2, 20);
            
            assertThat(metadata.getNextPageNumber()).isEqualTo(3);
        }
        
        @Test
        @DisplayName("Should return -1 when no next page exists")
        void shouldReturnNegativeOneWhenNoNextPage() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(100, 4, 20);
            
            assertThat(metadata.getNextPageNumber()).isEqualTo(-1);
        }
        
        @Test
        @DisplayName("Should return correct previous page number")
        void shouldReturnCorrectPreviousPageNumber() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(100, 2, 20);
            
            assertThat(metadata.getPreviousPageNumber()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Should return -1 when no previous page exists")
        void shouldReturnNegativeOneWhenNoPreviousPage() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(100, 0, 20);
            
            assertThat(metadata.getPreviousPageNumber()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {
        
        @Test
        @DisplayName("Should return correct default page size")
        void shouldReturnDefaultPageSize() {
            assertThat(PaginationHelper.getDefaultPageSize()).isEqualTo(20);
        }
        
        @Test
        @DisplayName("Should return correct max page size")
        void shouldReturnMaxPageSize() {
            assertThat(PaginationHelper.getMaxPageSize()).isEqualTo(100);
        }
        
        @ParameterizedTest
        @CsvSource({
            "0, 1",
            "1, 1",
            "50, 50",
            "100, 100",
            "101, 100",
            "1000, 100",
            "-5, 1"
        })
        @DisplayName("Should normalize page size correctly")
        void shouldNormalizePageSize(int input, int expected) {
            int normalized = PaginationHelper.normalizePageSize(input);
            assertThat(normalized).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {
        
        @Test
        @DisplayName("Should handle single item across single page")
        void shouldHandleSingleItem() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(1, 0, 20);
            
            assertThat(metadata.totalPages()).isEqualTo(1);
            assertThat(metadata.itemsOnPage()).isEqualTo(1);
            assertThat(metadata.hasNext()).isFalse();
            assertThat(metadata.hasPrevious()).isFalse();
        }
        
        @Test
        @DisplayName("Should handle exact page boundary")
        void shouldHandleExactPageBoundary() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(100, 0, 20);
            
            assertThat(metadata.totalPages()).isEqualTo(5);
            assertThat(metadata.itemsOnPage()).isEqualTo(20);
        }
        
        @Test
        @DisplayName("Should handle page size of 1")
        void shouldHandlePageSizeOfOne() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(10, 5, 1);
            
            assertThat(metadata.totalPages()).isEqualTo(10);
            assertThat(metadata.itemsOnPage()).isEqualTo(1);
            assertThat(metadata.offset()).isEqualTo(5);
        }
        
        @Test
        @DisplayName("Should handle maximum page size")
        void shouldHandleMaximumPageSize() {
            PaginationHelper.PageMetadata metadata = 
                PaginationHelper.createPageMetadata(1000, 0, 100);
            
            assertThat(metadata.totalPages()).isEqualTo(10);
            assertThat(metadata.itemsOnPage()).isEqualTo(100);
        }
    }
}