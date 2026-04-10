package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.laderrco.fortunelink.portfolio.application.exceptions.CsvImportCommitException;
import com.laderrco.fortunelink.portfolio.application.services.CsvImportService.ParsedRow;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Csv Import Service Unit Tests")
class CsvImportServiceTest {
  @Mock
  private TransactionService transactionService;
  @InjectMocks
  private CsvImportService csvImportService;

  private final PortfolioId PID = PortfolioId.newId();
  private final UserId UID = UserId.random();
  private final AccountId AID = AccountId.newId();
  // Currency must be 3 chars, AssetType must match Enum name
  private final String HDR = "date,type,symbol,asset_type,quantity,price,currency,notes\n";

  private MockMultipartFile csv(String content) {
    return new MockMultipartFile("f", "t.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
  }

  @Nested
  @DisplayName("Validation & Parsing")
  class Validation {
    @ParameterizedTest
    @CsvSource({
        "'15-01-2024,BUY,AAPL,STOCK,1,1,USD,n', Invalid date",
        "'2024-01-01,BUY,,STOCK,1,1,USD,n', Symbol required",
        "'2024-01-01,BUY,AAPL,STOCK,,1,USD,n', Quantity required",
        "'2024-01-01,DEPOSIT,,CASH,, ,USD,n', Price/amount required",
        "'2024-01-01,MAGIC,AAPL,STOCK,1,1,USD,n', Invalid value",
        "'2024-01-01,BUY,AAPL,INVALID,1,1,USD,n', Invalid value"
    })
    void validationBranches(String row, String expectedError) {
      var res = csvImportService.importTransactions(csv(HDR + row), PID, UID, AID);
      assertThat(res.success()).isFalse();
      assertThat(res.errors().get(0).message()).contains(expectedError);
    }
  }

  @Nested
  @DisplayName("Execution")
  class Execution {
    @Test
    void recordsAllTypes() {
      String data = HDR + """
          2024-01-01,BUY,AAPL,STOCK,1,1,USD,n
          2024-01-02,SELL,AAPL,STOCK,1,1,USD,n
          2024-01-03,DEPOSIT,,CASH,,1,USD,n
          2024-01-04,WITHDRAWAL,,CASH,,1,USD,n
          2024-01-05,DIVIDEND,AAPL,STOCK,,1,USD,n
          """;
      var result = csvImportService.importTransactions(csv(data), PID, UID, AID);
      assertThat(result.success()).isTrue();
      assertThat(result.rowsCommitted()).isEqualTo(5);
    }

    @Test
    void rollsBackOnServiceFailure() {
      // Ensure the data here is valid so it passes Phase 1 (Parsing)
      String validRow = "2024-01-01,BUY,AAPL,STOCK,1,100.00,USD,note";
      doThrow(new RuntimeException("DB Down")).when(transactionService).recordPurchase(any());

      assertThatThrownBy(() -> csvImportService.importTransactions(csv(HDR + validRow), PID, UID, AID))
          .isInstanceOf(CsvImportCommitException.class)
          .hasMessageContaining("Row 1 failed on commit");
    }

    @Test
    void reflectionEdgeCases() {
      // Null type check
      ParsedRow nullRow = new ParsedRow(1, Instant.now(), null, "A", AssetType.STOCK, BigDecimal.ONE, BigDecimal.ONE,
          "USD", "n");
      assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(csvImportService, "executeRow", nullRow, PID, UID, AID))
          .isExactlyInstanceOf(NullPointerException.class);

      // Out of bounds ordinal check
      TransactionType mockType = mock(TransactionType.class);
      when(mockType.ordinal()).thenReturn(999);
      ParsedRow badRow = new ParsedRow(1, Instant.now(), mockType, "A", AssetType.STOCK, BigDecimal.ONE, BigDecimal.ONE,
          "USD", "n");

      assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(csvImportService, "executeRow", badRow, PID, UID, AID))
          .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("executeRow: throws IllegalArgumentException for unimplemented transaction types")
    void executeRowThrowsOnUnimplementedType() {
      ParsedRow unhandledRow = new ParsedRow(
          1,
          Instant.now(),
          TransactionType.TRANSFER_IN,
          "AAPL",
          AssetType.STOCK,
          BigDecimal.ONE,
          BigDecimal.TEN,
          "USD",
          "Testing default branch");

      assertThatThrownBy(
          () -> ReflectionTestUtils.invokeMethod(csvImportService, "executeRow", unhandledRow, PID, UID, AID))
          .isExactlyInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Unsupported transaction type in CSV: TRANSFER_IN");
    }
  }
}