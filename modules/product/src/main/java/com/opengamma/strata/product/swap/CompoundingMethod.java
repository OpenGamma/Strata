/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * A convention defining how to compound interest.
 * <p>
 * When calculating interest, it may be necessary to apply compounding.
 * Compound interest occurs where the basic interest is collected over one period but paid over a longer period.
 * For example, interest may be collected every three months but only paid every year.
 * <p>
 * For more information see this <a href="http://www.isda.org/c_and_a/pdf/ISDA-Compounding-memo.pdf">ISDA note</a>.
 */
public enum CompoundingMethod {

  /**
   * No compounding applies.
   * <p>
   * This is typically used when the payment periods align with the accrual periods
   * thus no compounding is necessary. It may also be used when there are multiple
   * accrual periods, but they are summed rather than compounded.
   */
  NONE,
  /**
   * Straight compounding applies, which is inclusive of the spread.
   * <p>
   * Compounding is based on the total of the observed rate and the spread.
   * <p>
   * Defined as "Compounding" in the ISDA 2006 definitions.
   */
  STRAIGHT,
  /**
   * Flat compounding applies.
   * <p>
   * For interest on the notional, known as the <i>Basic Compounding Period Amount</i>,
   * compounding is based on the total of the observed rate and the spread.
   * For interest on previously accrued interest, known as the <i>Additional Compounding Period Amount</i>,
   * compounding is based only on the observed rate, excluding the spread.
   * <p>
   * Defined as "Flat Compounding" in the ISDA 2006 definitions.
   */
  FLAT,
  /**
   * Spread exclusive compounding applies.
   * <p>
   * Compounding is based only on the observed rate, with the spread treated as simple interest.
   * <p>
   * Defined as "Compounding treating Spread as simple interest" in the ISDA definitions.
   */
  SPREAD_EXCLUSIVE;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CompoundingMethod of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted unique name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name());
  }

}
