package com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.laderrco.fortunelink.portfolio.domain.model.enums.AssetType;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.TransactionMetadata.ExclusionRecord;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;

@DisplayName("TransactionMetadata Value Object Unit Tests")
class TransactionMetadataTest {
  @Test
  @DisplayName("Null Safety: handles null additional data and source by providing defaults")
  void nullSafetyAndDefaults() {
    var meta = new TransactionMetadata(AssetType.STOCK, null, null, null);

    assertEquals("UNKNOWN", meta.source());
    assertNotNull(meta.additionalData());
    assertTrue(meta.isEmpty());
  }

  @Test
  @DisplayName("Null Safety: maintains defaults even when an ExclusionRecord is present")
  void nullSafetyWithExclusionRecord() {
    var exclusionRecord = new ExclusionRecord(Instant.now(), UserId.random(), "testing");
    var meta = new TransactionMetadata(AssetType.STOCK, null, exclusionRecord, null);

    assertEquals("UNKNOWN", meta.source());
    assertNotNull(meta.additionalData());
    assertTrue(meta.isEmpty());
  }

  @Test
  @DisplayName("Immutability: additionalData should not reflect changes made to the original input map")
  void immutabilityOfAdditionalData() {
    Map<String, String> originalData = new HashMap<>();
    originalData.put("key", "value");

    var meta = new TransactionMetadata(AssetType.STOCK, "SOURCE", null, originalData);

    // Modify original map post-construction
    originalData.put("key", "changed");

    assertEquals("value", meta.get("key"), "Metadata should retain the original value");
    assertNull(meta.get("key2"));
    assertTrue(meta.containsKey("key"));
    assertFalse(meta.containsKey("keys"));
  }

  @Test
  @DisplayName("Fluent API: 'with' methods should return a new immutable instance")
  void withMethodsCreateNewInstances() {
    var base = TransactionMetadata.manual(AssetType.CRYPTO);
    var updated = base.with("broker_id", "123");

    assertNotSame(base, updated);
    assertTrue(base.isEmpty(), "Original instance should remain empty");
    assertEquals("123", updated.get("broker_id"));
    assertEquals("MANUAL", updated.source());

    Map<String, String> additionalData = Map.of("key", "value");
    TransactionMetadata newData = base.withAll(additionalData);
    assertTrue(newData.containsKey("key"));
  }

  @Test
  @DisplayName("Flat Map: basic metadata correctly flattens to map format")
  void asFlatMapBasic() {
    var meta = new TransactionMetadata(AssetType.STOCK, null, null, null);
    Map<String, String> map = meta.asFlatMap();

    assertEquals("STOCK", map.get("assetType"));
    assertEquals("UNKNOWN", map.get("source"));
    assertEquals("false", map.get("excluded"));
  }

  @Test
  @DisplayName("Flat Map: inclusion of exclusion details when transaction is excluded")
  void asFlatMapExclusionDetails() {
    Instant now = Instant.now();
    UserId userId = UserId.random();
    var exclusionRecord = new ExclusionRecord(now, userId, null);
    var meta = new TransactionMetadata(AssetType.STOCK, null, exclusionRecord, null);

    Map<String, String> map = meta.asFlatMap();

    assertEquals("true", map.get("excluded"));
    assertEquals(now.toString(), map.get("excludedAt"));
    assertEquals(userId.id().toString(), map.get("excludedBy"));
  }

  @Test
  @DisplayName("Flat Map: includes optional exclusion reason if provided")
  void asFlatMapExclusionWithReason() {
    Instant now = Instant.now();
    UserId userId = UserId.random();
    var record = new ExclusionRecord(now, userId, "for testing");
    var meta = new TransactionMetadata(AssetType.STOCK, null, record, null);

    Map<String, String> map = meta.asFlatMap();

    assertEquals("true", map.get("excluded"));
    assertEquals("for testing", map.get("excludedReason"));
  }

  @Test
  @DisplayName("Static Factories: csvImport initializes with correct source and filename")
  void csvImportFactory() {
    var csv = TransactionMetadata.csvImport(AssetType.STOCK, "trades.csv");

    assertEquals("CSV_IMPORT", csv.source());
    assertEquals("trades.csv", csv.get("filename"));
  }
}