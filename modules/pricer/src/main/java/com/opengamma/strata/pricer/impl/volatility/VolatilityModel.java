/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.volatility;

/**
 * An interface of volatility surface description under a specific volatility model.  
 * <p>
 * The volatility value is computed by {@code #getVolatility(Object)}, where the type of the argument depends on 
 * respective volatility models.  
 * <p>
 * An example of the argument is a set of time-to-expiry, strike value and forward value. 
 * In this case the argument is an array of {@code double}. 
 *  
 * @param <T>  the type of argument for volatility and volatility sensitivity
 */
public interface VolatilityModel<T> {

  /**
   * Obtains volatility value for the given argument.
   * 
   * @param t  the argument. 
   * @return the volatility
   */
  public abstract double getVolatility(T t);
}
