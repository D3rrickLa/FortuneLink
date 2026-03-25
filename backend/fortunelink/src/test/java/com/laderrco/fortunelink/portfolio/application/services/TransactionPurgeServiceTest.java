package com.laderrco.fortunelink.portfolio.application.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.laderrco.fortunelink.portfolio.domain.repositories.TransactionRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Transaction Purge Service Unit Tests")
class TransactionPurgeServiceTest {

  @Mock
  private TransactionRepository transactionRepository;

  private TransactionPurgeService purgeService;

  @Nested
  @DisplayName("Purge Logic and Date Calculation")
  class PurgeLogicTests {
    @Test
    @DisplayName("purgeExpiredTransactions: calculates correct cutoff for 30 day retention")
    void purgeCalculatesCorrectCutoff() {
      int retentionDays = 30;
      purgeService = new TransactionPurgeService(transactionRepository, retentionDays);

      purgeService.purgeExpiredTransactions();

      ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
      verify(transactionRepository).deleteAllExpiredTransactions(cutoffCaptor.capture());

      Instant capturedCutoff = cutoffCaptor.getValue();
      Instant expectedApprox = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

      // Check that the calculation is correct (within a 1-sec margin for execution time)
      long diffSeconds = Math.abs(capturedCutoff.getEpochSecond() - expectedApprox.getEpochSecond());
      assert (diffSeconds < 2);
    }

    @Test
    @DisplayName("purgeExpiredTransactions: calls repository even when retention is 0")
    void purgeWorksWithZeroRetention() {
      purgeService = new TransactionPurgeService(transactionRepository, 0);

      purgeService.purgeExpiredTransactions();

      verify(transactionRepository).deleteAllExpiredTransactions(any(Instant.class));
    }
  }

  @Nested
  @DisplayName("Execution Results and Logging")
  class ExecutionResultsTests {
    @Test
    @DisplayName("purgeExpiredTransactions: handles zero deletions without error")
    void purgeHandlesZeroDeletions() {
      purgeService = new TransactionPurgeService(transactionRepository, 365);
      // Repository returns 0 deleted rows
      when(transactionRepository.deleteAllExpiredTransactions(any(Instant.class)))
          .thenReturn(0);

      assertThatCode(() -> purgeService.purgeExpiredTransactions())
          .doesNotThrowAnyException();

      verify(transactionRepository).deleteAllExpiredTransactions(any(Instant.class));
    }

    @Test
    @DisplayName("purgeExpiredTransactions: handles positive deletions successfully")
    void purgeHandlesPositiveDeletions() {
      purgeService = new TransactionPurgeService(transactionRepository, 365);
      // Repository returns 50 deleted rows
      when(transactionRepository.deleteAllExpiredTransactions(any(Instant.class)))
          .thenReturn(50);
      purgeService.purgeExpiredTransactions();

      verify(transactionRepository).deleteAllExpiredTransactions(any(Instant.class));
    }
  }

  @Nested
  @DisplayName("Error Handling and Resilience")
  class ErrorHandlingTests {
    @Test
    @DisplayName("purgeExpiredTransactions: suppresses exceptions to keep scheduler alive")
    void purgeHandlesRepositoryExceptionGracefully() {
      purgeService = new TransactionPurgeService(transactionRepository, 365);

      // Simulate a database failure
      when(transactionRepository.deleteAllExpiredTransactions(any()))
          .thenThrow(new RuntimeException("Database Connection Timeout"));

      // The method should NOT throw an exception up to the caller
      assertThatCode(() -> purgeService.purgeExpiredTransactions())
          .doesNotThrowAnyException();

      verify(transactionRepository).deleteAllExpiredTransactions(any());
    }

    @Test
    @DisplayName("purgeExpiredTransactions: skip info logging when nothing is deleted")
    void purgeSkipsLoggingWhenNoDeletions() {
      purgeService = new TransactionPurgeService(transactionRepository, 365);
      when(transactionRepository.deleteAllExpiredTransactions(any())).thenReturn(0);

      purgeService.purgeExpiredTransactions();

      // We can't easily verify the logger itself without complex setups,
      // but we verify the logic flow completes.
      verify(transactionRepository).deleteAllExpiredTransactions(any());
    }
  }
}