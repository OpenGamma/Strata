/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * A compounded rate type.
 * <p>
 * Compounded rate is continuously compounded rate or periodically compounded rate.
 * The main application of this is z-spread computation under a specific way of compounding.
 * See, for example, {@link DiscountFactors}.
 */
public enum CompoundedRateType implements NamedEnum {

  /**
   * Periodic compounding.
   * <p>
   * The rate is periodically compounded.
   * In this case the number of periods par year should be specified in addition.
   */
  PERIODIC,
  /**
   * Continuous compounding.
   * <p>
   * The rate is continuously compounded.
   */
  CONTINUOUS;

  // helper for name conversions
  private static final EnumNames<CompoundedRateType> NAMES = EnumNames.of(CompoundedRateType.class);

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified name.
   * <p>
   * Parsing handles the mixed case form produced by {@link #toString()} and
   * the upper and lower case variants of the enum constant name.
   * 
   * @param name  the name to parse
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static CompoundedRateType of(String name) {
    return NAMES.parse(name);
  }

  //-------------------------------------------------------------------------
  /**
   * Returns the formatted name of the type.
   * 
   * @return the formatted string representing the type
   */
  @ToString
  @Override
  public String toString() {
    return NAMES.format(this);
  }

}
