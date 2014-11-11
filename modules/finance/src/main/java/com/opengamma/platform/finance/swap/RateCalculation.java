/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import org.joda.beans.ImmutableBean;

/**
 * Part of a swap leg. TODO
 * <p>
 * Implementations must be immutable and thread-safe beans.
 */
public interface RateCalculation
    extends ImmutableBean {

  /**
   * Gets the notional of the leg.
   * 
   * @return the notional
   */
  public abstract NotionalAmount getNotional();

  /**
   * Gets the negative rate method.
   * 
   * @return the method
   */
  public default NegativeRateMethod getNegativeRateMethod() {
    return NegativeRateMethod.ALLOW_NEGATIVE;
  }

}
