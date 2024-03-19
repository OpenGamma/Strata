/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.currency;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.function.DoubleUnaryOperator;

import org.joda.beans.JodaBeanUtils;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.collect.ComparisonChain;
import com.google.common.math.DoubleMath;
import com.opengamma.strata.collect.ArgChecker;

/**
 * An amount of a currency.
 * <p>
 * This class represents a {@code double} amount associated with a currency.
 * It is specifically named "CurrencyAmount" and not "Money" to indicate that
 * it simply holds a currency and an amount. By contrast, naming it "Money"
 * would imply it was a suitable choice for accounting purposes, which it is not.
 * This design approach has been chosen primarily for performance reasons.
 * Using a {@code BigDecimal} is markedly slower.
 * See the {@link Money} class for the alternative that uses {@link BigDecimal}.
 * <p>
 * A {@code double} is a 64 bit floating point value suitable for most calculations.
 * Floating point maths is
 * <a href="http://docs.oracle.com/cd/E19957-01/806-3568/ncg_goldberg.html">inexact</a>
 * due to the conflict between binary and decimal arithmetic.
 * As such, there is the potential for data loss at the margins.
 * For example, adding the {@code double} values {@code 0.1d} and {@code 0.2d}
 * results in {@code 0.30000000000000004} rather than {@code 0.3}.
 * As can be seen, the level of error is small, hence providing this class is
 * used appropriately, the use of {@code double} is acceptable.
 * For example, using this class to provide a meaningful result type after
 * calculations have completed would be an appropriate use.
 * <p>
 * This class is immutable and thread-safe.
 */
public final class CurrencyAmount
    implements FxConvertible<CurrencyAmount>, Comparable<CurrencyAmount>, Serializable {

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
  private final double amount;

  //-------------------------------------------------------------------------
  /**
   * Obtains a zero amount instance of {@code CurrencyAmount} for the specified currency.
   *
   * @param currency  the currency the amount is in
   * @return the zero amount instance
   */
  public static CurrencyAmount zero(Currency currency) {
    return of(currency, 0d);
  }

  /**
   * Obtains an instance of {@code CurrencyAmount} for the specified currency and amount.
   * <p>
   * If the negative form of zero is passed in, it will be converted to positive zero.
   *
   * @param currency  the currency the amount is in
   * @param amount  the amount of the currency to represent
   * @return the currency amount
   * @throws IllegalArgumentException if the amount is {@code NaN}
   */
  public static CurrencyAmount of(Currency currency, double amount) {
    return new CurrencyAmount(currency, amount);
  }

  /**
   * Obtains an instance of {@code CurrencyAmount} for the specified ISO-4217
   * three letter currency code and amount.
   * <p>
   * A currency is uniquely identified by ISO-4217 three letter code.
   * This method creates the currency if it is not known.
   * <p>
   * If the negative form of zero is passed in, it will be converted to positive zero.
   *
   * @param currencyCode  the three letter currency code, ASCII and upper case
   * @param amount  the amount of the currency to represent
   * @return the currency amount
   * @throws IllegalArgumentException if the currency code is invalid, or if the amount is {@code NaN}
   */
  public static CurrencyAmount of(String currencyCode, double amount) {
    return of(Currency.of(currencyCode), amount);
  }

  //-------------------------------------------------------------------------
  /**
   * Parses the string to produce a {@code CurrencyAmount}.
   * <p>
   * This parses the {@code toString} format of '${currency} ${amount}'.
   *
   * @param amountStr  the amount string
   * @return the currency amount
   * @throws IllegalArgumentException if the amount cannot be parsed
   */
  @FromString
  public static CurrencyAmount parse(String amountStr) {
    // this method has had some performance optimizations applied
    if (amountStr == null || amountStr.length() <= 4 || amountStr.charAt(3) != ' ') {
      throw new IllegalArgumentException("Unable to parse amount, invalid format: " + amountStr);
    }

    String currencyCode = amountStr.substring(0, 3);
    String doubleString = amountStr.substring(4);
    if (doubleString.indexOf(' ') != -1) {
      throw new IllegalArgumentException("Unable to parse amount, invalid format: " + amountStr);
    }

    try {
      Currency cur = Currency.parse(currencyCode);
      double amount = Double.parseDouble(doubleString);
      return new CurrencyAmount(cur, amount);
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
  private CurrencyAmount(Currency currency, double amount) {
    this.currency = ArgChecker.notNull(currency, "currency");
    this.amount = ArgChecker.notNaN(amount + 0d, "amount");  // this weird addition removes negative zero
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
  public double getAmount() {
    return amount;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this {@code CurrencyAmount} with the specified amount added.
   * <p>
   * This adds the specified amount to this monetary amount, returning a new object.
   * The addition simply uses standard {@code double} arithmetic.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param amountToAdd  the amount to add, in the same currency
   * @return an amount based on this with the specified amount added
   * @throws IllegalArgumentException if the currencies are not equal
   */
  public CurrencyAmount plus(CurrencyAmount amountToAdd) {
    ArgChecker.notNull(amountToAdd, "amountToAdd");
    ArgChecker.isTrue(amountToAdd.getCurrency().equals(currency), "Unable to add amounts in different currencies");
    return plus(amountToAdd.getAmount());
  }

  /**
   * Returns a copy of this {@code CurrencyAmount} with the specified amount added.
   * <p>
   * This adds the specified amount to this monetary amount, returning a new object.
   * The addition simply uses standard {@code double} arithmetic.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param amountToAdd  the amount to add
   * @return an amount based on this with the specified amount added
   */
  public CurrencyAmount plus(double amountToAdd) {
    return new CurrencyAmount(currency, amount + amountToAdd);
  }

  /**
   * Returns a copy of this {@code CurrencyAmount} with the specified amount subtracted.
   * <p>
   * This subtracts the specified amount to this monetary amount, returning a new object.
   * The addition simply uses standard {@code double} arithmetic.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param amountToSubtract  the amount to subtract, in the same currency
   * @return an amount based on this with the specified amount subtracted
   * @throws IllegalArgumentException if the currencies are not equal
   */
  public CurrencyAmount minus(CurrencyAmount amountToSubtract) {
    ArgChecker.notNull(amountToSubtract, "amountToSubtract");
    ArgChecker.isTrue(amountToSubtract.getCurrency().equals(currency), "Unable to subtract amounts in different currencies");
    return minus(amountToSubtract.getAmount());
  }

  /**
   * Returns a copy of this {@code CurrencyAmount} with the specified amount subtracted.
   * <p>
   * This subtracts the specified amount to this monetary amount, returning a new object.
   * The addition simply uses standard {@code double} arithmetic.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param amountToSubtract  the amount to subtract
   * @return an amount based on this with the specified amount subtracted
   */
  public CurrencyAmount minus(double amountToSubtract) {
    return new CurrencyAmount(currency, amount - amountToSubtract);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this {@code CurrencyAmount} with the amount multiplied.
   * <p>
   * This takes this amount and multiplies it by the specified value.
   * The multiplication simply uses standard {@code double} arithmetic.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @param valueToMultiplyBy  the scalar amount to multiply by
   * @return an amount based on this with the amount multiplied
   */
  public CurrencyAmount multipliedBy(double valueToMultiplyBy) {
    return new CurrencyAmount(currency, amount * valueToMultiplyBy);
  }

  /**
   * Applies an operation to the amount.
   * <p>
   * This is generally used to apply a mathematical operation to the amount.
   * For example, the operator could multiply the amount by a constant, or take the inverse.
   * <pre>
   *   multiplied = base.mapAmount(value -> (value &lt; 0 ? 0 : value * 3));
   * </pre>
   *
   * @param mapper  the operator to be applied to the amount
   * @return a copy of this amount with the mapping applied to the original amount
   */
  public CurrencyAmount mapAmount(DoubleUnaryOperator mapper) {
    ArgChecker.notNull(mapper, "mapper");
    return new CurrencyAmount(currency, mapper.applyAsDouble(amount));
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the amount is zero.
   * 
   * @return true if zero
   */
  public boolean isZero() {
    return amount == 0;
  }

  /**
   * Checks if the amount is positive.
   * <p>
   * Zero and negative amounts return false.
   * 
   * @return true if positive
   */
  public boolean isPositive() {
    return amount > 0;
  }

  /**
   * Checks if the amount is negative.
   * <p>
   * Zero and positive amounts return false.
   * 
   * @return true if negative
   */
  public boolean isNegative() {
    return amount < 0;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a copy of this {@code CurrencyAmount} with the amount negated.
   * <p>
   * This takes this amount and negates it. If the amount is 0.0 or -0.0 the negated amount is 0.0.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @return an amount based on this with the amount negated
   */
  public CurrencyAmount negated() {
    return new CurrencyAmount(currency, -amount);
  }

  /**
   * Returns a copy of this {@code CurrencyAmount} with a positive amount.
   * <p>
   * The result of this method will always be positive, where the amount is equal to {@code Math.abs(amount)}.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @return a currency amount based on this where the amount is positive
   */
  public CurrencyAmount positive() {
    return amount < 0 ? negated() : this;
  }

  /**
   * Returns a copy of this {@code CurrencyAmount} with a negative amount.
   * <p>
   * The result of this method will always be negative, equal to {@code -Math.abs(amount)}.
   * <p>
   * This instance is immutable and unaffected by this method.
   * 
   * @return a currency amount based on this where the amount is negative
   */
  public CurrencyAmount negative() {
    return amount > 0 ? negated() : this;
  }

  //-------------------------------------------------------------------------
  /**
   * Converts this monetary amount to the equivalent {@code Money}.
   * <p>
   * An instance of {@link Money} only stores the amount to the number of decimal places of the currency.
   * As such, this method may result in a loss of precision.
   *
   * @return the equivalent {@code Money}
   */
  public Money toMoney() {
    return Money.of(currency, amount);
  }

  /**
   * Converts this monetary amount to the equivalent {@code BigMoney}.
   *
   * @return the equivalent {@code BigMoney}
   */
  public BigMoney toBigMoney() {
    return BigMoney.of(currency, amount);
  }

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
  public CurrencyAmount convertedTo(Currency resultCurrency, double fxRate) {
    if (currency.equals(resultCurrency)) {
      if (DoubleMath.fuzzyEquals(fxRate, 1d, 1e-8)) {
        return this;
      }
      throw new IllegalArgumentException("FX rate must be 1 when no conversion required");
    }
    return CurrencyAmount.of(resultCurrency, amount * fxRate);
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
  public CurrencyAmount convertedTo(Currency resultCurrency, FxRateProvider rateProvider) {
    if (currency.equals(resultCurrency)) {
      return this;
    }
    double converted = rateProvider.convert(amount, currency, resultCurrency);
    return CurrencyAmount.of(resultCurrency, converted);
  }

  //-------------------------------------------------------------------------
  /**
   * Compares this currency amount to another.
   * <p>
   * This compares currencies alphabetically, then by amount.
   *
   * @param other  the other amount
   * @return negative if less, zero if equal, positive if greater
   */
  @Override
  public int compareTo(CurrencyAmount other) {
    return ComparisonChain.start()
        .compare(currency, other.currency)
        .compare(amount, other.amount)
        .result();
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this currency amount equals another.
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
      CurrencyAmount other = (CurrencyAmount) obj;
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
    return currency + " " +
        (DoubleMath.isMathematicalInteger(amount) ? Long.toString((long) amount) : Double.toString(amount));
  }

}
