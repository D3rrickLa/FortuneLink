package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.application.services.AuthenticationUserService;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Currency;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.Money;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.NetWorthSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.NetWorthSnapshotRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.config.limiting.RateLimitInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = NetWorthHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class NetWorthHistoryControllerTest {

  private static final UUID USER_UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final String BASE_URL = "/api/v1/net-worth/history";

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  NetWorthSnapshotRepository snapshotRepository;
  @MockitoBean
  AuthenticationUserService authenticationUserService;
  @MockitoBean
  RateLimitInterceptor rateLimitInterceptor;

  @BeforeEach
  void setUp() throws Exception {
    when(authenticationUserService.getCurrentUser()).thenReturn(USER_UUID);
    when(rateLimitInterceptor.preHandle(
        any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
        .thenReturn(true);
  }

  @Nested
  @DisplayName("GET / — getHistory")
  class GetHistory {

    @Test
    @DisplayName("200 with list of snapshots using default days=90")
    void returns200WithDefaultDays() throws Exception {
      when(snapshotRepository.findByUserIdSince(any(UserId.class), any(Instant.class)))
          .thenReturn(List.of(buildSnapshot()));

      mockMvc.perform(get(BASE_URL))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(1))
          .andExpect(jsonPath("$[0].netWorth").value(50000.0))
          .andExpect(jsonPath("$[0].currency").value("CAD"))
          .andExpect(jsonPath("$[0].hasStaleData").value(false));
    }

    @Test
    @DisplayName("200 with snapshots filtered by custom days parameter")
    void returns200WithCustomDays() throws Exception {
      when(snapshotRepository.findByUserIdSince(any(UserId.class), any(Instant.class)))
          .thenReturn(List.of());

      mockMvc.perform(get(BASE_URL).param("days", "365"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("200 with empty list when user has no historical snapshots")
    void returns200WithEmptyList() throws Exception {
      when(snapshotRepository.findByUserIdSince(any(UserId.class), any(Instant.class)))
          .thenReturn(List.of());

      mockMvc.perform(get(BASE_URL))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("400 when days=0 (below minimum of 1)")
    void returns400ForZeroDays() throws Exception {
      mockMvc.perform(get(BASE_URL).param("days", "0"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(snapshotRepository);
    }

    @Test
    @DisplayName("400 when days=1826 (above maximum of 1825)")
    void returns400ForExcessiveDays() throws Exception {
      mockMvc.perform(get(BASE_URL).param("days", "1826"))
          .andExpect(status().isBadRequest());

      verifyNoInteractions(snapshotRepository);
    }

    @Test
    @DisplayName("200 at boundary: days=1825 is valid")
    void returns200AtMaxDaysBoundary() throws Exception {
      when(snapshotRepository.findByUserIdSince(any(UserId.class), any(Instant.class)))
          .thenReturn(List.of());

      mockMvc.perform(get(BASE_URL).param("days", "1825"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("200 at boundary: days=1 is valid")
    void returns200AtMinDaysBoundary() throws Exception {
      when(snapshotRepository.findByUserIdSince(any(UserId.class), any(Instant.class)))
          .thenReturn(List.of());

      mockMvc.perform(get(BASE_URL).param("days", "1"))
          .andExpect(status().isOk());
    }
  }

  
  
  

  private NetWorthSnapshot buildSnapshot() {
    Currency cad = Currency.of("CAD");
    Money fifty = new Money(new java.math.BigDecimal("50000"), cad);
    Money zero = Money.zero(cad);
    return new NetWorthSnapshot(
        UUID.randomUUID(),
        UserId.fromString(USER_UUID.toString()),
        fifty, 
        zero, 
        fifty, 
        cad,
        false,
        Instant.now());
  }
}