/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import com.opengamma.strata.basics.currency.Currency;

/**
 * A index of interest rates, such as an Overnight or Inter-Bank rate.
 * <p>
 * Many financial products require knowledge of interest rate indices, such as Libor.
 * Implementations of this interface define these indices.
 * See {@link IborIndex} and {@link OvernightIndex}.
 * <p>
 * All implementations of this interface must be immutable and thread-safe.
 */
public interface RateIndex
    extends Index {

  /**
   * Gets the currency of the index.
   * 
   * @return the currency of the index
   */
  public abstract Currency getCurrency();

}
