/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate.swap;

import com.opengamma.strata.finance.rate.swap.PaymentPeriod;
import com.opengamma.strata.pricer.PricingEnvironment;

/**
 * Pricer for payment periods.
 * <p>
 * This function provides the ability to price a {@link PaymentPeriod}.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of period
 */
public interface PaymentPeriodPricerFn<T extends PaymentPeriod> {

  /**
   * Calculates the present value of a single payment period.
   * <p>
   * The amount is expressed in the currency of the period.
   * This returns the value of the period with discounting.
   * <p>
   * The payment date of the period should not be in the past.
   * The result of this method for payment dates in the past is undefined.
   * 
   * @param env  the pricing environment
   * @param period  the period to price
   * @return the present value of the period
   */
  public abstract double presentValue(PricingEnvironment env, T period);

  /**
   * Calculates the future value of a single payment period.
   * <p>
   * The amount is expressed in the currency of the period.
   * This returns the value of the period without discounting.
   * <p>
   * The payment date of the period should not be in the past.
   * The result of this method for payment dates in the past is undefined.
   * 
   * @param env  the pricing environment
   * @param period  the period to price
   * @return the future value of the period
   */
  public abstract double futureValue(PricingEnvironment env, T period);

}
