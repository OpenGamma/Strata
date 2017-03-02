/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.google.common.base.CaseFormat;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Flag indicating whether a trade is "buy" or "sell".
 * <p>
 * Specifies whether the financial instrument is buy-side or sell-side.
 * For example, in a Forward Rate Agreement the buyer receives the floating rate
 * of interest in exchange for a fixed rate, whereas the seller pays the floating rate.
 * This flag is stored on the instrument to indicate whether it was bought or sold.
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
   * Obtains an instance from the specified unique name.
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
   * Normalizes the specified notional amount using this buy/sell rule.
   * <p>
   * This returns a positive signed amount if this is 'buy', and a negative signed amount 
   * if this is 'sell'. This effectively normalizes the input notional
   * to the buy/sell sign conventions of this library.
   * 
   * @param amount  the amount to adjust
   * @return the adjusted amount
   */
  public double normalize(double amount) {
    double normalized = Math.abs(amount);
    return isBuy() ? normalized : -normalized;
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
