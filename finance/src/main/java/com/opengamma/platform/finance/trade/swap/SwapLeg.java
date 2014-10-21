/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.trade.swap;

/**
 * A single leg of a swap trade.
 * <p>
 * A swap leg can be expressed in a parameterized form or in an expanded form.
 * This interface only contains methods to extract the expanded form.
 */
public interface SwapLeg {

  /**
   * Converts this swap leg to the equivalent expanded swap leg.
   * 
   * @return the equivalent expanded swap leg
   * @throws RuntimeException if the swap leg is invalid
   */
  public ExpandedSwapLeg toExpanded();

}
