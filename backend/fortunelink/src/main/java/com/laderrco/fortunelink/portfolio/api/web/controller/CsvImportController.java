package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.application.services.CsvImportService;
import com.laderrco.fortunelink.portfolio.application.views.CsvImportResult;
import com.laderrco.fortunelink.portfolio.application.views.CsvRowError;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * CSV import endpoints.
 * <p>
 * GET /template , download the CSV template POST / , upload a CSV file for import
 * <p>
 * The import is intentionally synchronous for MVP. At scale you'd want to push the file to S3 and
 * process it async via SQS/queue. For the user base you're targeting (hundreds, not millions),
 * synchronous is fine and far simpler to debug and reason about.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/transactions/import")
public class CsvImportController {

  private final CsvImportService csvImportService;

  /**
   * Returns a pre-filled CSV template the user can download, populate, and upload. No auth required
   * on the template , it contains no user data.
   */
  @GetMapping("/template")
  public ResponseEntity<byte[]> getTemplate() {
    byte[] content = csvImportService.generateTemplate().getBytes(StandardCharsets.UTF_8);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv"));
    headers.setContentDisposition(
        ContentDisposition.attachment().filename("fortunelink_import_template.csv").build());

    return ResponseEntity.ok().headers(headers).body(content);
  }

  /**
   * Accepts a multipart CSV upload, validates all rows, then commits.
   *
   * <p>
   * Returns 200 with a summary on full success. Returns 422 with row-level errors if any row fails
   * validation. Returns 500 if validation passes but commit fails (domain invariant violation).
   *
   * <p>
   * File size is enforced by Spring's multipart config (spring.servlet.multipart.max-file-size).
   * Set this to something sane , 2MB is enough for 50k rows of text.
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<CsvImportResult> importCsv(@PathVariable String portfolioId,
      @AuthenticatedUser UserId userId, @PathVariable String accountId,
      @RequestParam("file") MultipartFile file) {

    if (file.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(CsvImportResult.failure(java.util.List.of(new CsvRowError(0, "File is empty"))));
    }

    if (file.getSize() > 5 * 1024 * 1024) {
      return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(
          CsvImportResult.failure(List.of(new CsvRowError(0, "File size exceeds 5MB limit"))));
    }

    String contentType = file.getContentType();
    if (contentType != null && !contentType.isBlank() && !contentType.contains("text/csv")
        && !contentType.contains("application/octet-stream") && !contentType.contains(
        "text/plain")) {
      return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(
          CsvImportResult.failure(java.util.List.of(new CsvRowError(0, "Expected a CSV file"))));
    }

    CsvImportResult result = csvImportService.importTransactions(file,
        PortfolioId.fromString(portfolioId), userId, AccountId.fromString(accountId));

    if (result.success()) {
      return ResponseEntity.ok(result);
    }

    // 422 , validation errors with row-level detail so the user knows what to fix
    return ResponseEntity.unprocessableContent().body(result);
  }
}
