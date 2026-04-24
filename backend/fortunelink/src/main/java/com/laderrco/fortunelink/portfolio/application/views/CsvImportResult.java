package com.laderrco.fortunelink.portfolio.application.views;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Response object for bulk CSV transaction imports")
public record CsvImportResult(
    @Schema(description = "Indicates if the entire batch was processed successfully", example = "true") boolean success,

    @Schema(description = "The total number of transactions successfully saved to the database", example = "42") int rowsCommitted,

    @Schema(description = "A list of validation or processing errors, populated only if success is false") List<CsvRowError> errors) {
  public static CsvImportResult success(int count) {
    return new CsvImportResult(true, count, List.of());
  }

  public static CsvImportResult failure(List<CsvRowError> errors) {
    return new CsvImportResult(false, 0, errors);
  }
}