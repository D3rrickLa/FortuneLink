package com.laderrco.fortunelink.portfolio.domain.model.enums;

public enum PositionStrategy {
  ACB("Average Cost Basis", "Required for Canadian tax reporting"),
  FIFO("First In First Out", "Default for US tax reporting"),
  LIFO("Last In First Out", "Allowed in US, not in Canada"),
  SPECIFIC_ID("Specific Identification", "Advanced: choose which lots to sell");

  private final String displayName;
  private final String description;

  PositionStrategy(String displayName, String description) {
    this.displayName = displayName;
    this.description = description;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDescription() {
    return description;
  }
}