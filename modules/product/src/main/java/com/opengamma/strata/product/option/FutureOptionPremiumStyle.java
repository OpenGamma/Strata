/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The style of premium for an option on a futures contract.
 * <p>
 * There are two styles of future options, one with daily margining, and one
 * with an up-front premium. This class specifies the types.
 */
public enum FutureOptionPremiumStyle {

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

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static FutureOptionPremiumStyle of(String uniqueName) {
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
