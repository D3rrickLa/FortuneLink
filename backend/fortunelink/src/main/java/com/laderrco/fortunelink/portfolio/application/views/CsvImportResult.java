package com.laderrco.fortunelink.portfolio.application.views;

import java.util.List;

public record CsvImportResult(
    boolean success,
    int rowsCommitted,
    List<CsvRowError> errors) {

  public static CsvImportResult success(int count) {
    return new CsvImportResult(true, count, List.of());
  }

  public static CsvImportResult failure(List<CsvRowError> errors) {
    return new CsvImportResult(false, 0, errors);
  }
}