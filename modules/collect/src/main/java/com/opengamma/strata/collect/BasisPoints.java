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
 * A percentage amount, with a maximum of 8 decimal places.
 * <p>
 * A number has three standard representations in finance:
 * <ul>
 * <li>decimal form - the form needed for mathematical calculations, as used by most parts of Strata
 * <li>percentage - where 1.2% is the same as the decimal 0.012
 * <li>basis points - where 125bps is the same as the decimal 0.0125
 * </ul>
 * This class allows the use of basis points form to be explicit.
 */
public final class BasisPoints implements Comparable<BasisPoints> {

  /** A basis points of zero. */
  public static final BasisPoints ZERO = new BasisPoints(Decimal.ZERO);

  private static final int MAX_SCALE = 8;

  /**
   * The basis points.
   */
  private final Decimal amount;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a basis points value.
   * 
   * @param basisPoints the basis points value
   * @return the basis points object, rounded (HALF_UP) to 8 decimal places
   */
  public static BasisPoints of(double basisPoints) {
    return of(Decimal.of(basisPoints));
  }

  /**
   * Obtains an instance from a basis points value.
   * 
   * @param basisPoints the basis points value
   * @return the basis points object, rounded (HALF_UP) to 8 decimal places
   */
  public static BasisPoints of(Decimal basisPoints) {
    return new BasisPoints(basisPoints);
  }

  /**
   * Obtains an instance from mathematical decimal form, where 0.007 will create an instance representing 70bps.
   * 
   * @param decimal the mathematical decimal value
   * @return the basis points object, rounded (HALF_UP) to 8 decimal places
   */
  public static BasisPoints fromDecimalForm(double decimal) {
    return fromDecimalForm(Decimal.of(decimal));
  }

  /**
   * Obtains an instance from mathematical decimal form, where 0.007 will create an instance representing 70bps.
   * 
   * @param decimal the mathematical decimal value
   * @return the basis points object, rounded (HALF_UP) to 8 decimal places
   */
  public static BasisPoints fromDecimalForm(Decimal decimal) {
    return of(decimal.movePoint(4));
  }

  /**
   * Obtains an instance from a percentage, where 0.7% will create an instance representing 70bps.
   * 
   * @param percentage the percentage
   * @return the basis points object
   */
  public static BasisPoints fromPercentage(Percentage percentage) {
    return of(percentage.valuePercent().movePoint(2));
  }

  /**
   * Parses a percentage.
   * <p>
   * The percentage may be suffixed by 'bps'.
   * 
   * @param str the basis points string
   * @return the basis points object, rounded to 8 decimal places
   */
  @FromString
  public static BasisPoints parse(String str) {
    ArgChecker.notNull(str, "str");
    if (str.endsWith("bps")) {
      return of(Decimal.of(str.substring(0, str.length() - 3).trim()));
    } else {
      return of(Decimal.of(str.trim()));
    }
  }

  // constructor
  private BasisPoints(Decimal basisPoints) {
    ArgChecker.notNull(basisPoints, "basisPoints");
    this.amount = basisPoints.roundToScale(MAX_SCALE, RoundingMode.HALF_UP);
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the value in basis points form as a {@code Decimal}.
   * <p>
   * A value of 150bps will return 150.
   *
   * @return the value in basis points form, with a maximum of 8 decimal places
   */
  public Decimal valueBasisPoints() {
    return amount;
  }

  /**
   * Converts this basis points to mathematical decimal form.
   * <p>
   * A value of 150bps will return 0.015.
   *
   * @return the amount in mathematical decimal form
   */
  public Decimal toDecimalForm() {
    return amount.movePoint(-4);
  }

  /**
   * Converts this basis points to the equivalent percentage.
   * <p>
   * A value of 150bps will return 1.5%.
   *
   * @return the amount in percentage form
   */
  public Percentage toPercentage() {
    return Percentage.fromBasisPoints(this);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a basis points equal to the this basis points plus the other one.
   * 
   * @param other the other basis points
   * @return the resulting basis points
   */
  public BasisPoints plus(BasisPoints other) {
    return new BasisPoints(amount.plus(other.amount));
  }

  /**
   * Returns a basis points equal to the this basis points minus the other one.
   * 
   * @param other the other basis points
   * @return the resulting basis points
   */
  public BasisPoints minus(BasisPoints other) {
    return new BasisPoints(amount.minus(other.amount));
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
  public BasisPoints map(UnaryOperator<Decimal> mapper) {
    return new BasisPoints(mapper.apply(amount));
  }

  //-----------------------------------------------------------------------
  /**
   * Compares this instance to another.
   * 
   * @param other the other instance
   * @return the comparison result
   */
  @Override
  public int compareTo(BasisPoints other) {
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
    if (obj instanceof BasisPoints) {
      BasisPoints other = (BasisPoints) obj;
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
   * Returns the formal string representation, '{value}bps'.
   * 
   * @return the formal string
   */
  @Override
  @ToString
  public String toString() {
    return amount.toString() + "bps";
  }

}
