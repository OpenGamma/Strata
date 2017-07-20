/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.strata.collect.named.EnumNames;
import com.opengamma.strata.collect.named.NamedEnum;

/**
 * The style of premium for an option on a futures contract.
 * <p>
 * There are two styles of future options, one with daily margining, and one
 * with an up-front premium. This class specifies the types.
 */
public enum FutureOptionPremiumStyle implements NamedEnum {

  /**
   * The "DailyMargin" style, used where the option has daily margining.
   * This is also known as <i>future-style margining</i>.
   */
  DAILY_MARGIN,
  /**
   * The "UpfrontPremium" style, used where the option has an upfront premium.
   * This is also known as <i>equity-style margining</i>.
   */
  UPFRONT_PREMIUM;

  // helper for name conversions
  private static final EnumNames<FutureOptionPremiumStyle> NAMES = EnumNames.of(FutureOptionPremiumStyle.class);

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
  public static FutureOptionPremiumStyle of(String name) {
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
