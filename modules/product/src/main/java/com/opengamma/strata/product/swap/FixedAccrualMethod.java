/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The method of accruing interest based on a fixed rate.
 * <p>
 * Two methods of accrual are supported - compounded and blah.
 */
public enum FixedAccrualMethod implements NamedEnum {

  /**
   * The compounded method.
   * <p>
   * Interest is accrued by simple compounding of the fixed rate during the accrual period.
   */
  COMPOUNDED {
    @Override
    public double simpleRate(double fixedRate, double accrual) {
      return fixedRate;
    }
  },

  /**
   * The blah method.
   * <p>
   * blah blah.
   */
  BLAH {
    @Override
    public double simpleRate(double fixedRate, double accrual) {
      return (Math.pow((1.0d + fixedRate), accrual) - 1.0d) / accrual;
    }
  };

  /**
   * Returns the simple rate associated with the accrual method.
   *
   *
   * @return the simple rate
   */
  public abstract double simpleRate(double fixedRate, double accrual);

  // helper for name conversions
  private static final EnumNames<FixedAccrualMethod> NAMES = EnumNames.of(FixedAccrualMethod.class);

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
  public static FixedAccrualMethod of(String name) {
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
