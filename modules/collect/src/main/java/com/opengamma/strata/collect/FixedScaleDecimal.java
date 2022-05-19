/*
 * Copyright (C) 2022 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.function.UnaryOperator;

import org.joda.beans.gen.PropertyDefinition;
import org.joda.convert.FromString;
import org.joda.convert.ToString;

/**
 * A decimal number based on {@code Decimal} with a fixed scale.
 * <p>
 * This is a lightweight wrapper for {@code Decimal} that ensures the scale is fixed, between 0 and 18.
 * It is most useful for monetary values.
 */
public final class FixedScaleDecimal implements Serializable, Comparable<FixedScaleDecimal> {

  /** The serialization version id. */
  private static final long serialVersionUID = 1L;

  /** The unscaled value. */
  @PropertyDefinition
  private final Decimal decimal;
  /** The fixed scale. */
  @PropertyDefinition
  private final int fixedScale;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from a decimal and scale.
   * 
   * @param decimal  the underlying decimal
   * @param fixedScale  the fixed scale, equal or greater than the scale of the decimal
   * @return the equivalent decimal
   */
  public static FixedScaleDecimal of(Decimal decimal, int fixedScale) {
    return new FixedScaleDecimal(decimal, fixedScale);
  }

  /**
   * Parses an instance from a {@code String}.
   * 
   * @param str  the string
   * @return the equivalent decimal
   * @throws NumberFormatException if the string cannot be parsed
   * @throws IllegalArgumentException if the value is too large
   */
  @FromString
  public static FixedScaleDecimal parse(String str) {
    Decimal decimal = Decimal.parse(str);
    int decimalPointPos = str.lastIndexOf('.');
    return new FixedScaleDecimal(decimal, decimalPointPos < 0 ? 0 : str.length() - decimalPointPos - 1);
  }

  // create an instance
  private FixedScaleDecimal(Decimal decimal, int fixedScale) {
    ArgChecker.notNull(decimal, "decimal");
    if (fixedScale < decimal.scale()) {
      throw new IllegalArgumentException("Scale must be equal or greater than the scale of the decimal: " + fixedScale);
    }
    if (fixedScale > Decimal.MAX_SCALE) {
      throw new IllegalArgumentException("Scale must be 18 or less: " + fixedScale);
    }
    this.decimal = decimal;
    this.fixedScale = fixedScale;
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying decimal.
   * <p>
   * The decimal may have a smaller scale, but it will not have a larger scale.
   * 
   * @return the decimal
   */
  public Decimal decimal() {
    return decimal;
  }

  /**
   * Gets the fixed scale.
   * 
   * @return the fixed scale, from 0 to 18
   */
  public int fixedScale() {
    return fixedScale;
  }

  //-------------------------------------------------------------------------
  /**
   * Maps this value using the maths operations of {@code Decimal}.
   * <p>
   * The result must have a scale equal or less than the fixed scale.
   * 
   * @param fn  the function to apply
   * @return the result of the function
   * @throws IllegalArgumentException if the result is too large
   */
  public FixedScaleDecimal map(UnaryOperator<Decimal> fn) {
    return of(fn.apply(decimal), fixedScale);
  }

  /**
   * Gets the value as a {@code BigDecimal} with the fixed scale.
   *
   * @return the decimal, with a scale equal to the fixed scale
   */
  public BigDecimal toBigDecimal() {
    return decimal.toBigDecimal().setScale(fixedScale);
  }

  //-----------------------------------------------------------------------
  @Override
  public int compareTo(FixedScaleDecimal other) {
    return decimal.compareTo(other.decimal);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof FixedScaleDecimal) {
      FixedScaleDecimal other = (FixedScaleDecimal) obj;
      return this.decimal.equals(other.decimal) && this.fixedScale == other.fixedScale;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return decimal.hashCode() ^ fixedScale;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formal string representation of the fixed scale decimal.
   * 
   * @return the plain string
   */
  @Override
  @ToString
  public String toString() {
    return decimal.formatAtLeast(fixedScale);
  }

}
