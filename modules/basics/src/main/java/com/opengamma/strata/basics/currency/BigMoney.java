/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.UnaryOperator;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.Splitter;
import com.google.common.collect.ComparisonChain;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A monetary amount, held to a maximum of 12 decimal places.
 * <p>
 * This class is similar to {@link Money}, but permits any number of decimal places.
 * The amount will always have at least the number of decimal places of the currency.
 * Trailing zeroes are stripped.
 * <p>
 * This class is immutable and thread-safe.
 */
public class BigMoney
    implements FxConvertible<BigMoney>, Comparable<BigMoney>, Serializable {

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
   * Obtains a zero amount instance of {@code BigMoney} for the specified currency.
   *
   * @param currency  the currency the amount is in
   * @return the zero amount instance
   */
  public static BigMoney zero(Currency currency) {
    return of(currency, BigDecimal.ZERO);
  }

  /**
   * Obtains an instance of {@code BigMoney} for the specified {@link CurrencyAmount}.
   *
   * @param currencyAmount  the instance of {@link CurrencyAmount} wrapping the currency and amount.
   * @return the currency amount
   */
  public static BigMoney of(CurrencyAmount currencyAmount) {
    return new BigMoney(currencyAmount.getCurrency(), BigDecimal.valueOf(currencyAmount.getAmount()));
  }

  /**
   * Obtains an instance of {@code BigMoney} for the specified {@link Money}.
   *
   * @param money  the instance of {@link Money} wrapping the currency and amount.
   * @return the currency amount
   */
  public static BigMoney of(Money money) {
    return new BigMoney(money.getCurrency(), money.getAmount());
  }

  /**
   * Obtains an instance of {@code BigMoney} for the specified currency and amount.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent
   * @return the currency amount
   */
  public static BigMoney of(Currency currency, double amount) {
    return new BigMoney(currency, BigDecimal.valueOf(amount));
  }

  /**
   * Obtains an instance of {@code BigMoney} for the specified currency and amount.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent, as an instance of {@link BigDecimal}
   * @return the currency amount
   */
  public static BigMoney of(Currency currency, BigDecimal amount) {
    return new BigMoney(currency, amount);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the string to produce a {@link BigMoney}.
   * <p>
   * This parses the {@code toString} format of '${currency} ${amount}'.
   *
   * @param amountStr  the amount string
   * @return the currency amount
   * @throws IllegalArgumentException if the amount cannot be parsed
   */
  @FromString
  public static BigMoney parse(String amountStr) {
    ArgChecker.notNull(amountStr, "amountStr");
    List<String> split = Splitter.on(' ').splitToList(amountStr);
    if (split.size() != 2) {
      throw new IllegalArgumentException("Unable to parse amount, invalid format: " + amountStr);
    }
    try {
      Currency cur = Currency.parse(split.get(0));
      return new BigMoney(cur, new BigDecimal(split.get(1)));
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
  private BigMoney(Currency currency, BigDecimal amount) {
    ArgChecker.notNull(currency, "currency");
    ArgChecker.notNull(amount, "amount");
    this.currency = currency;
    BigDecimal fixedAmount = amount.stripTrailingZeros();
    if (fixedAmount.scale() < currency.getMinorUnitDigits()) {
      fixedAmount = fixedAmount.setScale(currency.getMinorUnitDigits());
    }
    if (fixedAmount.scale() > 12) {
      fixedAmount = fixedAmount.setScale(12, RoundingMode.HALF_UP);
    }
    this.amount = fixedAmount;
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
   * Gets the amount of the currency.
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
   * Returns a copy of this {@code BigMoney} with the specified amount added.
   * <p>
   * This adds the specified amount to this monetary amount, returning a new object.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param amountToAdd  the amount to add, in the same currency
   * @return an amount based on this with the specified amount added
   * @throws IllegalArgumentException if the currencies are not equal
   */
  public BigMoney plus(BigMoney amountToAdd) {
    ArgChecker.notNull(amountToAdd, "amountToAdd");
    ArgChecker.isTrue(amountToAdd.getCurrency().equals(currency), "Unable to add amounts in different currencies");
    return new BigMoney(currency, amount.add(amountToAdd.amount));
  }

  /**
   * Returns a copy of this {@code BigMoney} with the specified amount subtracted.
   * <p>
   * This subtracts the specified amount to this monetary amount, returning a new object.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param amountToSubtract  the amount to subtract, in the same currency
   * @return an amount based on this with the specified amount subtracted
   * @throws IllegalArgumentException if the currencies are not equal
   */
  public BigMoney minus(BigMoney amountToSubtract) {
    ArgChecker.notNull(amountToSubtract, "amountToSubtract");
    ArgChecker.isTrue(amountToSubtract.getCurrency().equals(currency), "Unable to subtract amounts in different currencies");
    return new BigMoney(currency, amount.subtract(amountToSubtract.amount));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this {@code BigMoney} with the amount multiplied.
   * <p>
   * This takes this amount and multiplies it by the specified value.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param valueToMultiplyBy  the scalar amount to multiply by
   * @return an amount based on this with the amount multiplied
   */
  public BigMoney multipliedBy(long valueToMultiplyBy) {
    return new BigMoney(currency, amount.multiply(BigDecimal.valueOf(valueToMultiplyBy)));
  }

  /**
   * Applies an operation to the amount.
   * <p>
   * This is generally used to apply a mathematical operation to the amount.
   * For example, the operator could multiply the amount by a constant, or take the inverse.
   * <pre>
   *   abs = base.mapAmount(value -> value.abs());
   * </pre>
   *
   * @param mapper  the operator to be applied to the amount
   * @return a copy of this amount with the mapping applied to the original amount
   */
  public BigMoney mapAmount(UnaryOperator<BigDecimal> mapper) {
    ArgChecker.notNull(mapper, "mapper");
    return new BigMoney(currency, mapper.apply(amount));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the amount is zero.
   * 
   * @return true if zero
   */
  public boolean isZero() {
    return amount.signum() == 0;
  }

  /**
   * Checks if the amount is positive.
   * <p>
   * Zero and negative amounts return false.
   * 
   * @return true if positive
   */
  public boolean isPositive() {
    return amount.signum() > 0;
  }

  /**
   * Checks if the amount is negative.
   * <p>
   * Zero and positive amounts return false.
   * 
   * @return true if negative
   */
  public boolean isNegative() {
    return amount.signum() < 0;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this {@code BigMoney} with the amount negated.
   * <p>
   * This takes this amount and negates it. If the amount is 0.0 or -0.0 the negated amount is 0.0.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @return an amount based on this with the amount negated
   */
  public BigMoney negated() {
    if (isZero()) {
      return this;
    }
    return new BigMoney(currency, amount.negate());
  }

  /**
   * Returns a copy of this {@code BigMoney} with a positive amount.
   * <p>
   * The result of this method will always be positive, where the amount is equal to {@code abs(amount)}.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @return an amount based on this where the amount is positive
   */
  public BigMoney positive() {
    return isNegative() ? negated() : this;
  }

  /**
   * Returns a copy of this {@code BigMoney} with a negative amount.
   * <p>
   * The result of this method will always be negative, equal to {@code -.abs(amount)}.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @return an amount based on this where the amount is negative
   */
  public BigMoney negative() {
    return isPositive() ? negated() : this;
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this monetary amount to the equivalent {@code CurrencyAmount}.
   *
   * @return the equivalent {@code CurrencyAmount}
   */
  public CurrencyAmount toCurrencyAmount() {
    return CurrencyAmount.of(currency, amount.doubleValue());
  }

  /**
   * Converts this monetary amount to the equivalent {@code Money}.
   *
   * @return the equivalent {@code Money}
   */
  public Money toMoney() {
    return Money.of(this);
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
  public BigMoney convertedTo(Currency resultCurrency, BigDecimal fxRate) {
    if (currency.equals(resultCurrency)) {
      if (DoubleMath.fuzzyEquals(fxRate.doubleValue(), 1d, 1e-8)) {
        return this;
      }
      throw new IllegalArgumentException("FX rate must be 1 when no conversion required");
    }
    return BigMoney.of(resultCurrency, amount.multiply(fxRate));
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
  public BigMoney convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    if (currency.equals(resultCurrency)) {
      return this;
    }
    double converted = rateProvider.convert(amount.doubleValue(), currency, resultCurrency);
    return BigMoney.of(resultCurrency, converted);
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
  public int compareTo(BigMoney other) {
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
      BigMoney other = (BigMoney) obj;
      return currency.equals(other.currency) && amount.equals(other.amount);
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
    return currency.hashCode() * 31 + amount.hashCode();
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
    return currency + " " + amount.toPlainString();
  }

}
