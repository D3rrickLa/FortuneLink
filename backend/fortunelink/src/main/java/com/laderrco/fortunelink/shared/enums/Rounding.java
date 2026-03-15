package com.laderrco.fortunelink.shared.enums;

import java.math.RoundingMode;

public enum Rounding {
  DIVISION(RoundingMode.HALF_UP),
  FOREX(RoundingMode.HALF_EVEN),
  MONEY(RoundingMode.HALF_EVEN),
  PERCENTAGE(RoundingMode.HALF_UP),
  QUANTITY(RoundingMode.HALF_EVEN);

  private final RoundingMode mode;

  Rounding(RoundingMode mode) {
    this.mode = mode;
  }

  public RoundingMode getMode() {
    return mode;
  }
}