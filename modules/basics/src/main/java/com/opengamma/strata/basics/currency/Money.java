/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import org.joda.beans.JodaBeanUtils;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An amount of a currency, rounded to match the currency specifications.
 * <p>
 * This class is similar to {@link CurrencyAmount}, but only exposes the rounded amounts.
 * The rounding is done using {@link BigDecimal}, as BigDecimal.ROUND_HALF_UP. Given this operation,
 * it should be assumed that the numbers are an approximation, and not an exact figure.
 * <p>
 * This class is immutable and thread-safe.
 */
public class Money
    implements FxConvertible<Money>, Comparable<Money>, Serializable {

  /** Serialization version. */
  private static final long serialVersionUID = 1L;

  /**
   * The currency.
   * <p>
   * For example, in the value 'GBP 12.34' the currency is 'GBP'.
   */
  private final Currency currency;
  /**
   * The amount of the currency.
   * <p>
   * For example, in the value 'GBP 12.34' the amount is 12.34.
   */
  private final BigDecimal amount;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of {@code Money} for the specified {@link CurrencyAmount}.
   *
   * @param currencyAmount  the instance of {@link CurrencyAmount} wrapping the currency and amount.
   * @return the currency amount
   */
  public static Money of(CurrencyAmount currencyAmount) {
    Currency currency = currencyAmount.getCurrency();
    BigDecimal roundedAmount = BigDecimal.valueOf(currencyAmount.getAmount())
        .setScale(currency.getMinorUnitDigits(), BigDecimal.ROUND_HALF_UP);
    return new Money(currency, roundedAmount);
  }

  /**
   * Obtains an instance of {@code Money} for the specified currency and amount.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent
   * @return the currency amount
   */
  public static Money of(Currency currency, double amount) {
    BigDecimal roundedAmount = BigDecimal.valueOf(amount)
        .setScale(currency.getMinorUnitDigits(), BigDecimal.ROUND_HALF_UP);
    return new Money(currency, roundedAmount);
  }

  /**
   * Obtains an instance of {@code Money} for the specified currency and amount.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent, as an instance of {@link BigDecimal}
   * @return the currency amount
   */
  public static Money of(Currency currency, BigDecimal amount) {
    BigDecimal roundedAmount = amount.setScale(currency.getMinorUnitDigits(), BigDecimal.ROUND_HALF_UP);
    return new Money(currency, roundedAmount);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the string to produce a {@link Money}.
   * <p>
   * This parses the {@code toString} format of '${currency} ${amount}'.
   *
   * @param amountStr  the amount string
   * @return the currency amount
   * @throws IllegalArgumentException if the amount cannot be parsed
   */
  @FromString
  public static Money parse(String amountStr) {
    ArgChecker.notNull(amountStr, "amountStr");
    List<String> split = Splitter.on(' ').splitToList(amountStr);
    if (split.size() != 2) {
      throw new IllegalArgumentException("Unable to parse amount, invalid format: " + amountStr);
    }
    try {
      Currency cur = Currency.parse(split.get(0));
      return new Money(cur, new BigDecimal(split.get(1)));
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException("Unable to parse amount: " + amountStr, ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   *
   * @param currency  the currency
   * @param amount  the amount
   */
  private Money(Currency currency, BigDecimal amount) {
    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(amount, "amount");
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
   * <p>
   * The amount will be rounded to the currency specifications.
   * <p>
   * For example, in the value 'GBP 12.34' the amount is 12.34.
   *
   * @return the amount
   */
  public BigDecimal getAmount() {
    return amount;
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
   * @param resultCurrency  the currency of the result
   * @param fxRate  the FX rate from this currency to the result currency
   * @return the converted instance, which should be expressed in the specified currency
   * @throws IllegalArgumentException if the FX is not 1 when no conversion is required
   */
  public Money convertedTo(Currency resultCurrency, BigDecimal fxRate) {
    if (currency.equals(resultCurrency)) {
      if (DoubleMath.fuzzyEquals(fxRate.doubleValue(), 1d, 1e-8)) {
        return this;
      }
      throw new IllegalArgumentException("FX rate must be 1 when no conversion required");
    }
    return Money.of(resultCurrency, amount.multiply(fxRate));
  }

  /**
   * Converts this amount to an equivalent amount in the specified currency.
   * <p>
   * The result will be expressed in terms of the given currency.
   * If conversion is needed, the provider will be used to supply the FX rate.
   *
   * @param resultCurrency  the currency of the result
   * @param rateProvider  the provider of FX rates
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

  //-------------------------------------------------------------------------
  /**
   * Compares this money to another.
   * <p>
   * This compares currencies alphabetically, then by amount.
   *
   * @param other  the other amount
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
   * Checks if this money equals another.
   *
   * @param obj  the other amount, null returns false
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

  //-------------------------------------------------------------------------
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
    return currency + " " + amount.toString();
  }

}
