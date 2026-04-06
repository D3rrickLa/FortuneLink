package com.laderrco.fortunelink.portfolio.api.web.dto.responses;

/**
 * API representation of a single realized capital gain or loss event.
 *
 * Tax note: occurredAt is stored and returned in UTC. The frontend MUST apply
 * the user's timezone offset when bucketing by tax year. A sale at 23:00 UTC
 * on Dec 31 may fall in the next calendar year in Eastern Time. This is not
 * handled server-side and is documented in RealizedGainsSummaryResponse.
 */
public record RealizedGainItemResponse(
    String symbol,
    MoneyResponse realizedGainLoss,
    MoneyResponse costBasisSold,
    Instant occurredAt,
    boolean isGain,
    boolean isLoss) {

  public static RealizedGainItemResponse from(RealizedGainRecord record) {
    return new RealizedGainItemResponse(
        record.symbol().symbol(),
        MoneyResponse.from(record.realizedGainLoss()),
        MoneyResponse.from(record.costBasisSold()),
        record.occurredAt(),
        record.isGain(),
        record.isLoss());
  }
}
