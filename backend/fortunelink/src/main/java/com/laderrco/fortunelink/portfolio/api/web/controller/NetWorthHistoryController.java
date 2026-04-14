package com.laderrco.fortunelink.portfolio.api.web.controller;

import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.financial.NetWorthSnapshot;
import com.laderrco.fortunelink.portfolio.domain.model.valueobjects.identifiers.UserId;
import com.laderrco.fortunelink.portfolio.domain.repositories.NetWorthSnapshotRepository;
import com.laderrco.fortunelink.portfolio.infrastructure.config.authentication.AuthenticatedUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/net-worth/history")
public class NetWorthHistoryController {

  private final NetWorthSnapshotRepository snapshotRepository;

  // GET /api/v1/net-worth/history?days=90
  @GetMapping
  public List<NetWorthSnapshotResponse> getHistory(@AuthenticatedUser UserId userId,
      @RequestParam(defaultValue = "90") int days) {

    if (days < 1 || days > 1825) { // max 5 years
      throw new IllegalArgumentException("days must be between 1 and 1825");
    }

    Instant since = Instant.now().minus(days, ChronoUnit.DAYS);
    return snapshotRepository.findByUserIdSince(userId, since).stream()
        .map(NetWorthSnapshotResponse::from).toList();
  }

  public record NetWorthSnapshotResponse(
      double netWorth,
      double totalAssets,
      double totalLiabilities,
      String currency,
      boolean hasStaleData,
      Instant snapshotDate) {

    public static NetWorthSnapshotResponse from(NetWorthSnapshot s) {
      return new NetWorthSnapshotResponse(s.netWorth().amount().doubleValue(),
          s.totalAssets().amount().doubleValue(), s.totalLiabilities().amount().doubleValue(),
          s.displayCurrency().getCode(), s.hasStaleData(), s.snapshotDate());
    }
  }
}