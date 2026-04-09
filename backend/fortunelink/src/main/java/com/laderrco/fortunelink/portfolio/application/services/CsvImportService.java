package com.laderrco.fortunelink.portfolio.application.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.laderrco.fortunelink.portfolio.application.commands.records.*;
import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.enums.TransactionType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Price;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Quantity;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.AccountId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.PortfolioId;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvImportService {
  private static Logger log = LoggerFactory.getLogger(CsvImportService.class);
  // Enforce a hard cap to prevent abuse on free tier.
  // If you ever add a "premium" tier, gate this behind a feature flag.
  private static final int MAX_ROWS = 5_000;
  private static final int EXPECTED_COLUMNS = 8;
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final TransactionService transactionService;

  /**
   * Two-phase: validate everything, then commit everything.
   * Returns a summary of what was imported or a list of row-level errors.
   */
  @Transactional
  public CsvImportResult importTransactions(
      MultipartFile file,
      PortfolioId portfolioId,
      UserId userId,
      AccountId accountId) {

    List<CsvRowError> errors = new ArrayList<>();
    List<ParsedRow> rows = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

      String headerLine = reader.readLine();
      if (headerLine == null) {
        return CsvImportResult.failure(List.of(new CsvRowError(0, "File is empty")));
      }

      String line;
      int rowNum = 1;
      while ((line = reader.readLine()) != null) {
        if (line.isBlank())
          continue;
        if (rowNum > MAX_ROWS) {
          errors.add(new CsvRowError(rowNum,
              "File exceeds maximum of " + MAX_ROWS + " rows"));
          break;
        }
        parseRow(line, rowNum, errors, rows);
        rowNum++;
      }
    } catch (Exception e) {
      log.error("CSV parse failed for portfolio={}", portfolioId, e);
      return CsvImportResult.failure(
          List.of(new CsvRowError(0, "File could not be read: " + e.getMessage())));
    }

    // Phase 1 validation failed — do not commit anything
    if (!errors.isEmpty()) {
      return CsvImportResult.failure(errors);
    }

    // Phase 2: commit
    int committed = 0;
    for (ParsedRow row : rows) {
      try {
        executeRow(row, portfolioId, userId, accountId);
        committed++;
      } catch (Exception e) {
        // A commit failure after validation passing means a domain invariant
        // was violated (e.g., selling before buying in chronological order).
        // Roll back the whole transaction via unchecked exception.
        log.error("Commit failed at row {} after validation passed: {}",
            row.rowNum(), e.getMessage());
        throw new CsvImportCommitException(
            "Row " + row.rowNum() + " failed on commit: " + e.getMessage(), e);
      }
    }

    return CsvImportResult.success(committed);
  }

  // -------------------------------------------------------------------------
  // Template generation
  // -------------------------------------------------------------------------

  public String generateTemplate() {
    return """
        # FortuneLink CSV Import Template
        # Date format: yyyy-MM-dd (e.g. 2024-01-15)
        # Types: BUY, SELL, DEPOSIT, WITHDRAWAL, DIVIDEND
        # Asset types: STOCK, ETF, CRYPTO, BOND (ignored for DEPOSIT/WITHDRAWAL/DIVIDEND)
        # Remove comment lines (starting with #) before uploading
        date,type,symbol,asset_type,quantity,price,currency,notes
        2024-01-15,BUY,AAPL,STOCK,10,185.50,CAD,Initial purchase
        2024-02-01,DEPOSIT,,CASH,,1000.00,CAD,Monthly contribution
        2024-03-10,SELL,AAPL,STOCK,5,200.00,CAD,Partial exit
        2024-03-10,DIVIDEND,AAPL,STOCK,,0.24,CAD,Q1 dividend
        """;
  }

  // -------------------------------------------------------------------------
  // Private helpers
  // -------------------------------------------------------------------------

  private void parseRow(String line, int rowNum,
      List<CsvRowError> errors, List<ParsedRow> rows) {
    // Strip comment lines
    if (line.startsWith("#"))
      return;

    String[] parts = line.split(",", -1);
    if (parts.length < EXPECTED_COLUMNS) {
      errors.add(new CsvRowError(rowNum,
          "Expected " + EXPECTED_COLUMNS + " columns, found " + parts.length));
      return;
    }

    try {
      Instant date = LocalDate.parse(parts[0].trim(), DATE_FORMATTER)
          .atStartOfDay(ZoneOffset.UTC).toInstant();
      TransactionType type = TransactionType.valueOf(parts[1].trim().toUpperCase());
      String symbol = parts[2].trim().toUpperCase();
      String assetTypeRaw = parts[3].trim().toUpperCase();
      String quantityRaw = parts[4].trim();
      String priceRaw = parts[5].trim();
      String currency = parts[6].trim().toUpperCase();
      String notes = parts[7].trim();

      // Type-specific field requirements
      if ((type == TransactionType.BUY || type == TransactionType.SELL)
          && symbol.isBlank()) {
        errors.add(new CsvRowError(rowNum, "Symbol required for " + type));
        return;
      }
      if (quantityRaw.isBlank() && (type == TransactionType.BUY
          || type == TransactionType.SELL)) {
        errors.add(new CsvRowError(rowNum, "Quantity required for " + type));
        return;
      }
      if (priceRaw.isBlank()) {
        errors.add(new CsvRowError(rowNum, "Price/amount required"));
        return;
      }

      BigDecimal price = new BigDecimal(priceRaw);
      BigDecimal quantity = quantityRaw.isBlank() ? BigDecimal.ZERO
          : new BigDecimal(quantityRaw);

      AssetType assetType = assetTypeRaw.isBlank() || assetTypeRaw.equals("CASH")
          ? AssetType.STOCK
          : AssetType.valueOf(assetTypeRaw);

      rows.add(new ParsedRow(rowNum, date, type, symbol, assetType,
          quantity, price, currency, notes));

    } catch (DateTimeParseException e) {
      errors.add(new CsvRowError(rowNum,
          "Invalid date format '" + parts[0].trim() + "'. Use yyyy-MM-dd"));
    } catch (IllegalArgumentException e) {
      errors.add(new CsvRowError(rowNum, "Invalid value: " + e.getMessage()));
    }
  }

  private void executeRow(ParsedRow row, PortfolioId portfolioId,
      UserId userId, AccountId accountId) {
    UUID idempotencyKey = deterministicKey(portfolioId, accountId, row);

    switch (row.type()) {
      case BUY -> transactionService.recordPurchase(
          RecordPurchaseCommand.builder()
              .idempotencyKey(idempotencyKey)
              .portfolioId(portfolioId)
              .userId(userId)
              .accountId(accountId)
              .symbol(row.symbol())
              .assetType(row.assetType())
              .quantity(new Quantity(row.quantity()))
              .price(Price.of(row.price(), Currency.of(row.currency())))
              .fees(List.of())
              .transactionDate(row.date())
              .notes(row.notes())
              .skipCashCheck(true) // historical import, no cash balance check
              .build());

      case SELL -> transactionService.recordSale(
          new RecordSaleCommand(idempotencyKey, portfolioId, userId, accountId,
              row.symbol(), new Quantity(row.quantity()),
              Price.of(row.price(), Currency.of(row.currency())),
              List.of(), row.date(), row.notes()));

      case DEPOSIT -> transactionService.recordDeposit(
          new RecordDepositCommand(idempotencyKey, portfolioId, userId, accountId,
              Money.of(row.price(), row.currency()), row.date(), row.notes()));

      case WITHDRAWAL -> transactionService.recordWithdrawal(
          new RecordWithdrawalCommand(idempotencyKey, portfolioId, userId, accountId,
              Money.of(row.price(), row.currency()), row.date(), row.notes()));

      case DIVIDEND -> transactionService.recordDividend(
          new RecordDividendCommand(idempotencyKey, portfolioId, userId, accountId,
              row.symbol(), Money.of(row.price(), row.currency()),
              row.date(), row.notes()));

      default -> throw new IllegalArgumentException(
          "Unsupported transaction type in CSV: " + row.type());
    }
  }

  /**
   * Deterministic idempotency key from content — safe to re-upload the same file.
   * Uses UUID v5 (name-based) semantics via a simple string hash.
   * Not cryptographically secure — this is not a security boundary.
   */
  private UUID deterministicKey(PortfolioId portfolioId, AccountId accountId, ParsedRow row) {
    String seed = portfolioId + ":" + accountId + ":" + row.rowNum() + ":"
        + row.date() + ":" + row.type() + ":" + row.symbol()
        + ":" + row.quantity() + ":" + row.price();
    return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
  }

  // -------------------------------------------------------------------------
  // Value objects
  // -------------------------------------------------------------------------

  public record ParsedRow(
      int rowNum,
      Instant date,
      TransactionType type,
      String symbol,
      AssetType assetType,
      BigDecimal quantity,
      BigDecimal price,
      String currency,
      String notes) {
  }

  public record CsvRowError(int rowNum, String message) {
  }

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

  public static class CsvImportCommitException extends RuntimeException {
    public CsvImportCommitException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}