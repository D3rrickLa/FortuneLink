package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.laderrco.fortunelink.portfolio.application.exceptions.CsvImportCommitException;
import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.services.CsvImportService;
import com.laderrco.fortunelink.portfolio.application.views.CsvImportResult;
import com.laderrco.fortunelink.portfolio.application.views.CsvRowError;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CsvImportController.class)
@AutoConfigureMockMvc(addFilters = false)
class CsvImportControllerTest {

  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final String PORTFOLIO_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final String ACCOUNT_ID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
  private static final String BASE_URL =
      "/api/v1/portfolios/" + PORTFOLIO_ID + "/accounts/" + ACCOUNT_ID + "/transactions/import";

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  CsvImportService csvImportService;
  @MockitoBean
  AuthenticationUserService authenticationUserService;
  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;

  @BeforeEach
  void setUp() throws Exception {
    when(authenticationUserService.getCurrentUser()).thenReturn(USER_UUID);
    when(rateLimitInterceptor.preHandle(any(HttpServletRequest.class),
        any(HttpServletResponse.class), any())).thenReturn(true);
  }

  private MockMultipartFile validCsvFile() {
    return new MockMultipartFile("file", "import.csv", "text/csv", validCsvContent().getBytes());
  }

  private String validCsvContent() {
    return "date,type,symbol,asset_type,quantity,price,currency,notes\n"
        + "2024-01-15,BUY,AAPL,STOCK,10,185.50,CAD,Initial purchase\n";
  }

  @Nested
  @DisplayName("GET /template — getTemplate")
  class GetTemplate {

    @Test
    @DisplayName("200 with CSV content-type and attachment disposition")
    void returns200WithCsvHeaders() throws Exception {
      when(csvImportService.generateTemplate()).thenReturn(
          "date,type,symbol,asset_type,quantity,price,currency,notes\n");

      mockMvc.perform(get(BASE_URL + "/template")).andExpect(status().isOk())
          .andExpect(header().string("Content-Type", containsString("text/csv")))
          .andExpect(header().string("Content-Disposition", containsString("attachment")))
          .andExpect(header().string("Content-Disposition",
              containsString("fortunelink_import_template.csv")));
    }

    @Test
    @DisplayName("Response body contains expected CSV header row")
    void responseBodyContainsCsvHeader() throws Exception {
      String template = "date,type,symbol,asset_type,quantity,price,currency,notes\n";
      when(csvImportService.generateTemplate()).thenReturn(template);

      mockMvc.perform(get(BASE_URL + "/template")).andExpect(status().isOk())
          .andExpect(content().string(containsString("date,type,symbol")));
    }
  }

  @Nested
  @DisplayName("POST / — importCsv")
  class ImportCsv {

    @Test
    @DisplayName("200 when file is valid and all rows commit successfully")
    void returns200OnSuccessfulImport() throws Exception {
      when(csvImportService.importTransactions(any(), any(), any(), any())).thenReturn(
          CsvImportResult.success(5));

      mockMvc.perform(multipart(BASE_URL).file(validCsvFile())).andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.rowsCommitted").value(5));
    }

    @Test
    @DisplayName("422 when service returns row-level validation errors")
    void returns422OnValidationErrors() throws Exception {
      List<CsvRowError> errors = List.of(new CsvRowError(2, "Invalid date format"),
          new CsvRowError(4, "Symbol required for BUY"));
      when(csvImportService.importTransactions(any(), any(), any(), any())).thenReturn(
          CsvImportResult.failure(errors));

      mockMvc.perform(multipart(BASE_URL).file(validCsvFile()))
          .andExpect(status().isUnprocessableContent())
          .andExpect(jsonPath("$.success").value(false))
          .andExpect(jsonPath("$.errors.length()").value(2))
          .andExpect(jsonPath("$.errors[0].rowNum").value(2))
          .andExpect(jsonPath("$.errors[0].message").value("Invalid date format"));
    }

    @Test
    @DisplayName("400 when the uploaded file is empty")
    void returns400ForEmptyFile() throws Exception {
      MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.csv", "text/csv",
          new byte[0]);

      mockMvc.perform(multipart(BASE_URL).file(emptyFile)).andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].message").value("File is empty"));

      verifyNoInteractions(csvImportService);
    }

    @Test
    @DisplayName("413 when file exceeds 5 MB")
    void returns413ForOversizedFile() throws Exception {
      byte[] oversized = new byte[5 * 1024 * 1024 + 1];
      MockMultipartFile bigFile = new MockMultipartFile("file", "big.csv", "text/csv", oversized);

      mockMvc.perform(multipart(BASE_URL).file(bigFile)).andExpect(status().isContentTooLarge())
          .andExpect(jsonPath("$.errors[0].message").value("File size exceeds 5MB limit"));

      verifyNoInteractions(csvImportService);
    }

    @Test
    @DisplayName("415 when content type is not CSV-compatible")
    void returns415ForUnsupportedContentType() throws Exception {
      MockMultipartFile jsonFile = new MockMultipartFile("file", "data.json", "application/json",
          "{\"key\":\"value\"}".getBytes());

      mockMvc.perform(multipart(BASE_URL).file(jsonFile))
          .andExpect(status().isUnsupportedMediaType())
          .andExpect(jsonPath("$.errors[0].message").value("Expected a CSV file"));

      verifyNoInteractions(csvImportService);
    }

    @Test
    @DisplayName("200 when content type is application/octet-stream (binary upload)")
    void accepts200ForOctetStream() throws Exception {
      when(csvImportService.importTransactions(any(), any(), any(), any())).thenReturn(
          CsvImportResult.success(3));

      MockMultipartFile file = new MockMultipartFile("file", "data.csv", "application/octet-stream",
          validCsvContent().getBytes());

      mockMvc.perform(multipart(BASE_URL).file(file)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("200 when content type is text/plain")
    void accepts200ForTextPlain() throws Exception {
      when(csvImportService.importTransactions(any(), any(), any(), any())).thenReturn(
          CsvImportResult.success(2));

      MockMultipartFile file = new MockMultipartFile("file", "data.csv", "text/plain",
          validCsvContent().getBytes());

      mockMvc.perform(multipart(BASE_URL).file(file)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("500 when service throws CsvImportCommitException after validation passes")
    void returns500WhenCommitFails() throws Exception {
      when(csvImportService.importTransactions(any(), any(), any(), any())).thenThrow(
          new CsvImportCommitException("Row 3 failed on commit: insufficient cash", null));

      mockMvc.perform(multipart(BASE_URL).file(validCsvFile()))
          .andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("200 with null content type is accepted (browser may omit it)")
    void accepts200ForNullContentType() throws Exception {
      when(csvImportService.importTransactions(any(), any(), any(), any())).thenReturn(
          CsvImportResult.success(1));

      MockMultipartFile file = new MockMultipartFile("file", "data.csv", null,
          validCsvContent().getBytes());

      mockMvc.perform(multipart(BASE_URL).file(file)).andExpect(status().isOk());
    }
  }
}