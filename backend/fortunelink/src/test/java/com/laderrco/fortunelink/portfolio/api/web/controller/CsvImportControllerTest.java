package com.laderrco.fortunelink.portfolio.api.web.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.application.services.CsvImportService;
import com.laderrco.fortunelink.portfolio.application.views.CsvImportResult;
import com.laderrco.fortunelink.portfolio.application.views.CsvRowError;
import com.laderrco.fortunelink.portfolio.infrastructure.config.SecurityTestsSuite.TestSecurityConfig;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;

import io.github.bucket4j.distributed.proxy.ProxyManager;

@WebMvcTest(CsvImportController.class)
@Import(TestSecurityConfig.class)
@DisplayName("CsvImportController Tests")
class CsvImportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private CsvImportService csvImportService;

  // Reuse these to prevent the 500/context errors we saw earlier
  @MockitoBean
  private AuthenticationUserService authenticationUserService;
  @MockitoBean
  private RateLimitInterceptor rateLimitInterceptor;
  @MockitoBean
  private ProxyManager<String> proxyManager;

  private static final String BASE_URL = "/api/v1/portfolios/p1/accounts/a1/transactions/import";

  @Test
  @DisplayName("GET /template → should return csv file")
  void getTemplateReturnsCsv() throws Exception {
    when(csvImportService.generateTemplate()).thenReturn("Date,Symbol,Quantity,Price\n");

    mockMvc.perform(get(BASE_URL + "/template"))
        .with(jwt())
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv"))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
  }

  @Test
  @DisplayName("POST / → should import valid CSV")
  void importCsv_success() throws Exception {
    // Create a mock multipart file
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "test.csv",
        "text/csv",
        "2023-01-01,AAPL,10,150.00".getBytes());

    CsvImportResult successResult = CsvImportResult.success(1);
    when(csvImportService.importTransactions(any(), any(), any(), any()))
        .thenReturn(successResult);

    mockMvc.perform(multipart(BASE_URL)
        .file(file)
        .with(jwt())) // Still need this to satisfy security!
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  @DisplayName("POST / → 415 Unsupported Media Type for wrong file types")
  void importCsv_wrongContentType() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "image.png",
        "image/png",
        "fake-image-data".getBytes());

    mockMvc.perform(multipart(BASE_URL)
        .file(file)
        .with(jwt()))
        .andExpect(status().isUnsupportedMediaType());

    verifyNoInteractions(csvImportService);
  }

  @Test
  @DisplayName("POST / → 422 Unprocessable Content when service returns failure")
  void importCsv_validationError() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "bad data".getBytes());

    CsvImportResult failureResult = CsvImportResult.failure(List.of(new CsvRowError(1, "Invalid Price")));
    when(csvImportService.importTransactions(any(), any(), any(), any()))
        .thenReturn(failureResult);

    mockMvc.perform(multipart(BASE_URL)
        .file(file)
        .with(jwt()))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.success").value(false));
  }
}