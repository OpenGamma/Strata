/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.math.RoundingMode;
import java.util.function.UnaryOperator;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

/**
 * A percentage amount, with a maximum of 10 decimal places.
 * <p>
 * A number has three standard representations in finance:
 * <ul>
 * <li>decimal form - the form needed for mathematical calculations, as used by most parts of Strata
 * <li>percentage - where 1.2% is the same as the decimal 0.012
 * <li>basis points - where 125bps is the same as the decimal 0.0125
 * </ul>
 * This class allows the use of percentage form to be explicit.
 */
public final class Percentage implements Comparable<Percentage> {

  /** A percentage of zero. */
  public static final Percentage ZERO = new Percentage(Decimal.ZERO);

  private static final int MAX_SCALE = 10;

  /**
   * The percentage.
   */
  private final Decimal amount;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a percentage value.
   * 
   * @param percentage the percentage value
   * @return the percentage object, rounded (HALF_UP) to 10 decimal places
   */
  public static Percentage of(double percentage) {
    return of(Decimal.of(percentage));
  }

  /**
   * Obtains an instance from a percentage value.
   * 
   * @param percentage the percentage value
   * @return the percentage object, rounded (HALF_UP) to 10 decimal places
   */
  public static Percentage of(Decimal percentage) {
    return new Percentage(percentage);
  }

  /**
   * Obtains an instance from mathematical decimal form, where 0.007 will create an instance representing 0.7%.
   * 
   * @param decimal the mathematical decimal value
   * @return the percentage object, rounded (HALF_UP) to 10 decimal places
   */
  public static Percentage fromDecimalForm(double decimal) {
    return fromDecimalForm(Decimal.of(decimal));
  }

  /**
   * Obtains an instance from mathematical decimal form, where 0.007 will create an instance representing 0.7%.
   * 
   * @param decimal the mathematical decimal value
   * @return the percentage object, rounded (HALF_UP) to 10 decimal places
   */
  public static Percentage fromDecimalForm(Decimal decimal) {
    return of(decimal.movePoint(2));
  }

  /**
   * Obtains an instance from a basis points value, where 70bps will create an instance representing 0.7%.
   * 
   * @param basisPoints the basis points value
   * @return the percentage object
   */
  public static Percentage fromBasisPoints(BasisPoints basisPoints) {
    return of(basisPoints.valueBasisPoints().movePoint(-2));
  }

  /**
   * Parses a percentage.
   * <p>
   * The percentage may be suffixed by '%' or 'pct'.
   * 
   * @param str the percentage string
   * @return the percentage object, rounded to 10 decimal places
   */
  @FromString
  public static Percentage parse(String str) {
    ArgChecker.notNull(str, "str");
    if (str.endsWith("%")) {
      return of(Decimal.of(str.substring(0, str.length() - 1).trim()));
    } else if (str.endsWith("pct")) {
      return of(Decimal.of(str.substring(0, str.length() - 3).trim()));
    } else {
      return of(Decimal.of(str.trim()));
    }
  }

  // constructor
  private Percentage(Decimal percentage) {
    ArgChecker.notNull(percentage, "percentage");
    this.amount = percentage.roundToScale(MAX_SCALE, RoundingMode.HALF_UP);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value in percentage form as a {@code Decimal}.
   * <p>
   * A value of 1.5% will return 1.5.
   *
   * @return the value in percentage form, with a maximum of 10 decimal places
   */
  public Decimal valuePercent() {
    return amount;
  }

  /**
   * Converts this percentage to mathematical decimal form.
   * <p>
   * A value of 1.5% will return 0.015.
   *
   * @return the amount in mathematical decimal form
   */
  public Decimal toDecimalForm() {
    return amount.movePoint(-2);
  }

  /**
   * Converts this percentage to the equivalent basis points.
   * <p>
   * A value of 1.5% will return 150bps.
   *
   * @return the amount in basis points form
   */
  public BasisPoints toBasisPoints() {
    return BasisPoints.fromPercentage(this);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a percentage equal to the this percentage plus the other one.
   * 
   * @param other the other percentage
   * @return the resulting percentage
   */
  public Percentage plus(Percentage other) {
    return new Percentage(amount.plus(other.amount));
  }

  /**
   * Returns a percentage equal to the this percentage minus the other one.
   * 
   * @param other the other percentage
   * @return the resulting percentage
   */
  public Percentage minus(Percentage other) {
    return new Percentage(amount.minus(other.amount));
  }

  /**
   * Applies an operation to the value.
   * <p>
   * This is generally used to apply a mathematical operation to the value.
   * For example, the operator could multiply the value by a constant, or take the inverse.
   * <pre>
   *   abs = base.map(value -> value.abs());
   * </pre>
   *
   * @param mapper  the operator to be applied to the amount
   * @return a copy of this value with the mapping applied to the original amount
   */
  public Percentage map(UnaryOperator<Decimal> mapper) {
    return new Percentage(mapper.apply(amount));
  }

  //-----------------------------------------------------------------------
  /**
   * Compares this instance to another.
   * 
   * @param other the other instance
   * @return the comparison result
   */
  @Override
  public int compareTo(Percentage other) {
    return amount.compareTo(other.amount);
  }

  /**
   * Checks if this instance equals another.
   * 
   * @param obj the other instance
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Percentage) {
      Percentage other = (Percentage) obj;
      return amount.equals(other.amount);
    }
    return false;
  }

  /**
   * Returns a suitable hash code.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return amount.hashCode();
  }

  /**
   * Returns the formal string representation, '{value}%'.
   * 
   * @return the formal string
   */
  @Override
  @ToString
  public String toString() {
    return amount.toString() + '%';
  }

}
