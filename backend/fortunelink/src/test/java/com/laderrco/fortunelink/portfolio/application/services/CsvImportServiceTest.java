package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.laderrco.fortunelink.portfolio.application.exceptions.CsvImportCommitException;
import com.laderrco.fortunelink.portfolio.application.services.CsvImportService.ParsedRow;
import com.laderrco.fortunelink.portfolio.application.views.CsvImportResult;
import com.laderrco.fortunelink.portfolio.application.views.CsvRowError;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
import org.springframework.web.multipart.MultipartFile;

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
      
      String validRow = "2024-01-01,BUY,AAPL,STOCK,1,100.00,USD,note";
      doThrow(new RuntimeException("DB Down")).when(transactionService).recordPurchase(any());

      assertThatThrownBy(() -> csvImportService.importTransactions(csv(HDR + validRow), PID, UID, AID))
          .isInstanceOf(CsvImportCommitException.class)
          .hasMessageContaining("Row 1 failed on commit");
    }

    @Test
    void reflectionEdgeCases() {
      
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
    @DisplayName("executeRow: throws IllegalStateException for unimplemented transaction types")
    void executeRowThrowsOnUnimplementedType() {
      ParsedRow unhandledRow = new ParsedRow(
          1,
          Instant.now(),
          TransactionType.TRANSFER_IN, // Not in SUPPORTED_CSV_TYPES
          "AAPL",
          AssetType.STOCK,
          BigDecimal.ONE,
          BigDecimal.TEN,
          "USD",
          "Testing default branch");

      assertThatThrownBy(
          () -> ReflectionTestUtils.invokeMethod(csvImportService, "executeRow", unhandledRow, PID, UID, AID))
          .isExactlyInstanceOf(IllegalStateException.class)
          .hasMessageContaining(
              "Validation-Execution mismatch: Type TRANSFER_IN passed validation but has no execution logic.");
    }
  }

  @Nested
  @DisplayName("Parsing and Validation Branches")
  class ParsingBranches {

    @Test
    @DisplayName("parseRow: handles comment lines and missing columns")
    void parseRowHandlesCommentsAndShortLines() {
      List<CsvRowError> errors = new ArrayList<>();
      List<ParsedRow> rows = new ArrayList<>();

      
      ReflectionTestUtils.invokeMethod(csvImportService, "parseRow", "# This is a comment", 1, errors, rows);
      assertThat(errors).isEmpty();
      assertThat(rows).isEmpty();

      
      ReflectionTestUtils.invokeMethod(csvImportService, "parseRow", "2024-01-01,BUY,AAPL", 2, errors, rows);
      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).message()).contains("Expected 8 columns, found 3");
    }

    @Test
    @DisplayName("parseRow: rejects unsupported transaction types")
    void parseRowRejectsUnsupportedTypes() {
      List<CsvRowError> errors = new ArrayList<>();
      List<ParsedRow> rows = new ArrayList<>();
      
      String csvLine = "2024-01-01,TRANSFER_IN,AAPL,STOCK,10,100,USD,notes";

      ReflectionTestUtils.invokeMethod(csvImportService, "parseRow", csvLine, 1, errors, rows);

      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).message()).contains("is not supported in CSV import");
      assertThat(rows).isEmpty();
    }

    @Test
    @DisplayName("parseRow: requires quantity for SELL transactions")
    void parseRowRequiresQuantityForSell() {
      List<CsvRowError> errors = new ArrayList<>();
      List<ParsedRow> rows = new ArrayList<>();
      
      String csvLine = "2024-01-01,SELL,AAPL,STOCK,,150.00,USD,notes";

      ReflectionTestUtils.invokeMethod(csvImportService, "parseRow", csvLine, 1, errors, rows);

      assertThat(errors).hasSize(1);
      assertThat(errors.get(0).message()).isEqualTo("Quantity required for SELL");
    }

    @ParameterizedTest
    @CsvSource({
        "'', STOCK", // isBlank() -> STOCK
        "CASH, STOCK", // equals('CASH') -> STOCK
        "ETF, ETF", // Specific type -> ETF
        "CRYPTO, CRYPTO" // Specific type -> CRYPTO
    })
    @DisplayName("parseRow: correctly resolves asset type and fallbacks")
    void parseRowResolvesAssetType(String inputAssetType, AssetType expected) {
      List<CsvRowError> errors = new ArrayList<>();
      List<ParsedRow> rows = new ArrayList<>();

      
      String csvLine = String.format("2024-01-01,BUY,AAPL,%s,10,150.00,USD,notes", inputAssetType);

      ReflectionTestUtils.invokeMethod(csvImportService, "parseRow", csvLine, 1, errors, rows);

      assertThat(errors).isEmpty();
      assertThat(rows.get(0).assetType()).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("Import Transactions Workflow")
  class ImportWorkflow {

    @Test
    @DisplayName("importTransactions: handles empty file")
    void handlesEmptyFile() {
      MockMultipartFile file = new MockMultipartFile("file", "".getBytes());

      CsvImportResult result = csvImportService.importTransactions(file, PID, UID, AID);

      assertThat(result.success()).isFalse();
      assertThat(result.errors().get(0).message()).isEqualTo("File is empty");
    }

    @Test
    @DisplayName("importTransactions: enforces MAX_ROWS limit")
    void enforcesMaxRows() throws Exception {
      
      StringBuilder sb = new StringBuilder("date,type,symbol,asset_type,quantity,price,currency,notes\n");
      for (int i = 0; i < 5005; i++) {
        sb.append("2024-01-01,BUY,AAPL,STOCK,1,100,USD,note\n");
      }
      MockMultipartFile file = new MockMultipartFile("file", sb.toString().getBytes());

      CsvImportResult result = csvImportService.importTransactions(file, PID, UID, AID);

      assertThat(result.success()).isFalse();
      assertThat(result.errors().stream().anyMatch(e -> e.message().contains("File exceeds maximum"))).isTrue();
    }

    @Test
    @DisplayName("importTransactions: catches IO exceptions during read")
    void handlesIoException() throws Exception {
      MultipartFile file = mock(MultipartFile.class);
      when(file.getInputStream()).thenThrow(new RuntimeException("Disk failure"));

      CsvImportResult result = csvImportService.importTransactions(file, PID, UID, AID);

      assertThat(result.success()).isFalse();
      assertThat(result.errors().get(0).message()).contains("File could not be read: Disk failure");
    }

    @Test
    @DisplayName("importTransactions: skips blank lines and processes valid rows")
    void skipsBlankLines() throws Exception {
      String csvContent = """
          date,type,symbol,asset_type,quantity,price,currency,notes

          2024-01-15,DEPOSIT,,CASH,,1000.00,USD,Valid Deposit


          2024-01-16,WITHDRAWAL,,CASH,,500.00,USD,Valid Withdrawal
          """;
      MockMultipartFile file = new MockMultipartFile("file", csvContent.getBytes());

      CsvImportResult result = csvImportService.importTransactions(file, PID, UID, AID);

      assertThat(result.success()).isTrue();
      assertThat(result.rowsCommitted()).isEqualTo(2); // Only the 2 non-blank rows
      verify(transactionService, times(1)).recordDeposit(any());
      verify(transactionService, times(1)).recordWithdrawal(any());
    }
  }

  @Test
  @DisplayName("generateTemplate: returns valid header and content")
  void testGenerateTemplate() {
    String template = csvImportService.generateTemplate();

    assertThat(template).contains("date,type,symbol,asset_type,quantity,price,currency,notes");
    assertThat(template).contains("BUY,AAPL,STOCK");
    // Ensure it starts with a comment/header
    assertThat(template.trim()).startsWith("#");
  }
}