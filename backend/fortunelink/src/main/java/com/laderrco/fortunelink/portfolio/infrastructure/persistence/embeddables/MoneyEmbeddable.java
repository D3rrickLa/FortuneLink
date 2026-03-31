package com.laderrco.fortunelink.portfolio.infrastructure.persistence.embeddables;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * JPA-side representation of {@code Money}.
 * <p>
 * Domain {@code Money} is a pure record with a custom {@code Currency} wrapper
 * 
 * it cannot be annotated with {@code @Embeddable}. This class lives entirely
 * in the infrastructure layer and is converted to/from {@code Money} by the
 * domain mappers.
 * <p>
 * Column names are intentionally generic ({@code _amount} / {@code _currency})
 * so that {@code @AttributeOverride} at the entity level provides the real
 * names.
 *
 * <pre>
 * // Usage example in an entity:
 * {@literal @}Embedded
 * {@literal @}AttributeOverrides({
 *     {@literal @}AttributeOverride(name = "amount",       column = {@literal @}Column(name = "cash_balance_amount")),
 *     {@literal @}AttributeOverride(name = "currencyCode", column = {@literal @}Column(name = "cash_balance_currency"))
 * })
 * private MoneyEmbeddable cashBalance;
 * </pre>
 */
@Embeddable
public class MoneyEmbeddable {

  @Column(nullable = false, precision = 20, scale = 10)
  private BigDecimal amount;

  @Column(length = 3, nullable = false)
  private String currencyCode;

  // JPA
  protected MoneyEmbeddable() {
  }

  public MoneyEmbeddable(BigDecimal amount, String currencyCode) {
    this.amount = amount;
    this.currencyCode = currencyCode;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrencyCode() {
    return currencyCode;
  }
}