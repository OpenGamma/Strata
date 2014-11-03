/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import com.opengamma.basics.currency.Currency;

/**
 * A single leg of a swap trade.
 * <p>
 * A swap leg can be expressed in a parameterized form or in an expanded form.
 * All swaps can be converted to the expanded form, which is structured in terms of
 * a list of payment periods consisting of accrual periods.
 */
public interface SwapLeg {

  /**
   * Gets the currency of the leg.
   * <p>
   * A swap leg has a single currency.
   * 
   * @return the currency of the leg
   */
  public Currency getCurrency();

  /**
   * Converts this swap leg to the equivalent expanded swap leg.
   * 
   * @return the equivalent expanded swap leg
   * @throws RuntimeException if the swap leg is invalid
   */
  public ExpandedSwapLeg toExpanded();

}
