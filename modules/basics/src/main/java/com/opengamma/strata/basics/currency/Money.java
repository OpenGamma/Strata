package com.opengamma.strata.basics.currency;

import java.io.Serializable;
import java.math.BigDecimal;

import org.joda.beans.JodaBeanUtils;
import org.joda.convert.ToString;

import com.google.common.collect.ComparisonChain;
import com.google.common.math.DoubleMath;

public class Money implements FxConvertible<Money>, Comparable<Money>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The currency.
   * For example, in the value 'GBP 12.34' the currency is 'GBP'.
   */
  private final Currency currency;

  /**
   * The amount of the currency.
   * For example, in the value 'GBP 12.34' the amount is 12.34.
   */
  private final BigDecimal amount;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of {@code Money} for the specified currency and amount.
   *
   * @param currencyAmount the instance of {@link CurrencyAmount} wrapping the currency and amount.
   * @return the currency amount
   */
  public static Money of(CurrencyAmount currencyAmount) {
    Currency currency = currencyAmount.getCurrency();
    BigDecimal roundedAmount = new BigDecimal(currencyAmount.getAmount())
        .setScale(currency.getMinorUnitDigits(), BigDecimal.ROUND_HALF_EVEN);
    return new Money(currency, roundedAmount);
  }

  /**
   * Obtains an instance of {@code Money} for the specified currency and amount.
   *
   * @param currency the currency the amount is in
   * @param amount the amount of the currency to represent
   * @return the currency amount
   */
  public static Money of(Currency currency, double amount) {
    BigDecimal roundedAmount = new BigDecimal(amount)
        .setScale(currency.getMinorUnitDigits(), BigDecimal.ROUND_HALF_EVEN);
    return new Money(currency, roundedAmount);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   *
   * @param currency the currency
   * @param amount the amount
   */
  private Money(Currency currency, BigDecimal amount) {
    this.currency = currency;
    this.amount = amount;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the currency.
   * <p>
   * For example, in the value 'GBP 12.34' the currency is 'GBP'.
   *
   * @return the currency
   */
  public Currency getCurrency() {
    return currency;
  }

  /**
   * Gets the amount of the currency as an instance of {@link BigDecimal}.
   * The amount will be rounded to the currency specifications.
   * <p>
   * For example, in the value 'GBP 12.34' the amount is 12.34.
   *
   * @return the amount
   */
  public BigDecimal getAmount() {
    return amount;
  }

  /**
   * Gets the amount of the currency as a double primitive.
   * The amount will be rounded to the currency specifications.
   * <p>
   * For example, in the value 'GBP 12.34' the amount is 12.34.
   *
   * @return the amount
   */
  public double getAmountAsDouble() {
    return amount.doubleValue();
  }


  //-------------------------------------------------------------------------
  /**
   * Converts this amount to an equivalent amount the specified currency.
   * <p>
   * The result will be expressed in terms of the given currency, converting
   * using the specified FX rate.
   * <p>
   * For example, if this represents 'GBP 100' and this method is called with
   * arguments {@code (USD, 1.6)} then the result will be 'USD 160'.
   *
   * @param resultCurrency the currency of the result
   * @param fxRate the FX rate from this currency to the result currency
   * @return the converted instance, which should be expressed in the specified currency
   * @throws IllegalArgumentException if the FX is not 1 when no conversion is required
   */
  public Money convertedTo(Currency resultCurrency, double fxRate) {
    if (currency.equals(resultCurrency)) {
      if (DoubleMath.fuzzyEquals(fxRate, 1d, 1e-8)) {
        return this;
      }
      throw new IllegalArgumentException("FX rate must be 1 when no conversion required");
    }
    return Money.of(resultCurrency, amount.multiply(new BigDecimal(fxRate)).doubleValue());
  }

  /**
   * Converts this amount to an equivalent amount in the specified currency.
   * <p>
   * The result will be expressed in terms of the given currency.
   * If conversion is needed, the provider will be used to supply the FX rate.
   *
   * @param resultCurrency the currency of the result
   * @param rateProvider the provider of FX rates
   * @return the converted instance, in the specified currency
   * @throws RuntimeException if no FX rate could be found
   */
  @Override
  public Money convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    if (currency.equals(resultCurrency)) {
      return this;
    }
    double converted = rateProvider.convert(amount.doubleValue(), currency, resultCurrency);
    return Money.of(resultCurrency, converted);
  }

  /**
   * Compares this instance of {@link Money} to another.
   * <p>
   * This compares currencies alphabetically, then by amount.
   *
   * @param other the other amount
   * @return negative if less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(Money other) {
    return ComparisonChain.start()
        .compare(currency, other.currency)
        .compare(amount, other.amount)
        .result();
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this instance of {@link Money} equals another.
   *
   * @param obj the other amount, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Money other = (Money) obj;
      return currency.equals(other.currency) &&
          JodaBeanUtils.equal(amount, other.amount);
    }
    return false;
  }

  /**
   * Returns a suitable hash code for the currency.
   *
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return currency.hashCode() * 31 + JodaBeanUtils.hashCode(amount);
  }

  /**
   * Gets the amount as a string.
   * <p>
   * The format is the currency code, followed by a space, followed by the
   * amount: '${currency} ${amount}'.
   *
   * @return the currency amount
   */
  @Override
  @ToString
  public String toString() {
    return currency + " " +amount.toString();
  }
}
