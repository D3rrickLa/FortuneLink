package com.laderrco.fortunelink.portfolio.infrastructure.exchange.boc;

import java.util.List;

public class BocCurrencyPairResolver {
  private static final String CAD = "CAD";

  private BocCurrencyPairResolver() {
  }

  /**
   * Resolves BOC FX series needed to satisfy a currency request.
   * <p>
   * Examples: USD/CAD -> [FXUSDCAD] CAD/USD -> [FXCADUSD] EUR/USD -> [FXEURCAD, FXCADUSD]
   */
  public static List<String> resolveSeries(String base, String target) {
    base = base.toUpperCase();
    target = target.toUpperCase();

    if (base.equals(target)) {
      throw new IllegalArgumentException("Base and target currencies cannot be the same");
    }

    // Direct CAD pair
    if (CAD.equals(base) || CAD.equals(target)) {
      return List.of("FX" + base + target);
    }

    // Cross-currency via CAD
    return List.of("FX" + base + CAD, "FX" + CAD + target);
  }
}