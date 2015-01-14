/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.collect.ArgChecker;

/**
 * Flag indicating whether a trade is "buy" or "sell".
 * <p>
 * Species whether the financial instrument is buy-side or sell-side.
 * For example, in a Forward Rate Agreement the buyer receives the floating rate
 * of interest in exchange for a fixed rate, whereas the seller pays the floating rate.
 */
public enum BuySell {

  /**
   * Buy.
   */
  BUY,
  /**
   * Sell.
   */
  SELL;

  //-------------------------------------------------------------------------
  /**
   * Obtains the type from a unique name.
   * 
   * @param uniqueName  the unique name
   * @return the type
   * @throws IllegalArgumentException if the name is not known
   */
  @FromString
  public static BuySell of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    return valueOf(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, uniqueName));
  }

  /**
   * Converts a boolean "is buy" flag to the enum value.
   * 
   * @param isBuy  the buy flag, true for buy, false for sell
   * @return the equivalent enum
   */
  public static BuySell ofBuy(boolean isBuy) {
    return isBuy ? BUY : SELL;
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the type is 'Buy'.
   * 
   * @return true if buy, false if sell
   */
  public boolean isBuy() {
    return this == BUY;
  }

  /**
   * Checks if the type is 'Sell'.
   * 
   * @return true if sell, false if buy
   */
  public boolean isSell() {
    return this == SELL;
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
