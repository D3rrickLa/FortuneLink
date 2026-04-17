package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.application.services.CsvImportService;
import com.laderrco.fortunelink.portfolio.application.views.CsvImportResult;
import com.laderrco.fortunelink.portfolio.application.views.CsvRowError;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @apiNote This was not tested properly via the api/Postman
 *          CSV import endpoints.
 *          <p>
 *          GET /template , download the CSV template POST / , upload a CSV file
 *          for import
 *          <p>
 *          The import is intentionally synchronous for MVP. At scale, you'd
 *          want to push the file to S3 and
 *          process it async via SQS/queue. For the user base you're targeting
 *          (hundreds, not millions),
 *          synchronous is fine and far simpler to debug and reason about.
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/portfolios/{portfolioId}/accounts/{accountId}/transactions/import")
@Tag(name = "Transaction Import", description = "Bulk CSV operations for transaction history")
public class CsvImportController {
  private final CsvImportService csvImportService;

  @GetMapping("/template")
  @Operation(summary = "Download CSV template", description = "Returns a blank CSV template with the required headers for bulk transaction importing.")
  @ApiResponse(responseCode = "200", description = "CSV file stream", content = @Content(mediaType = "text/csv", schema = @Schema(type = "string", format = "binary")))
  public ResponseEntity<byte @NotNull []> getTemplate() {
    byte[] content = csvImportService.generateTemplate().getBytes(StandardCharsets.UTF_8);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("text/csv"));
    headers.setContentDisposition(
        ContentDisposition.attachment().filename("fortunelink_import_template.csv").build());

    return ResponseEntity.ok().headers(headers).body(content);
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Operation(summary = "Upload transaction CSV", description = "Processes a multipart CSV file. Validates all rows before committing. Returns row-level errors on failure.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Import successful", content = @Content(schema = @Schema(implementation = CsvImportResult.class))),
      @ApiResponse(responseCode = "422", description = "Validation failed for one or more rows", content = @Content(schema = @Schema(implementation = CsvImportResult.class))),
      @ApiResponse(responseCode = "413", description = "File size exceeds 5MB limit"),
      @ApiResponse(responseCode = "415", description = "Unsupported file type (must be CSV)")
  })
  public ResponseEntity<CsvImportResult> importCsv(
      @PathVariable @Schema(example = "p-123") String portfolioId,
      @AuthenticatedUser UserId userId,
      @PathVariable @Schema(example = "acc-456") String accountId,
      @Parameter(description = "The CSV file to upload", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)) @RequestParam("file") MultipartFile file) {

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
