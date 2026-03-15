package com.laderrco.fortunelink.portfolio.domain.utils;


import com.laderrco.fortunelink.portfolio.domain.exceptions.DomainArgumentException;

public final class Guard {

  private Guard() {
  }

  public static <T> T notNull(T value, String name) {
    if (value == null) {
      throw new DomainArgumentException("%s is required and cannot be null".formatted(name));
    }
    return value;
  }
}