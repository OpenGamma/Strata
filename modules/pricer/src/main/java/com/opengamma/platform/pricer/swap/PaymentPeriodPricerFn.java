/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.swap;

import java.time.LocalDate;

import com.opengamma.platform.finance.swap.PaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for a single rate payment period.
 * <p>
 * Defines the values that can be calculated on a swap leg payment period.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 * 
 * @param <T>  the type of period
 */
public interface PaymentPeriodPricerFn<T extends PaymentPeriod> {

  /**
   * Calculates the present value of a single payment period.
   * <p>
   * This returns the value of the period with discounting.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param period  the period to price
   * @return the present value of the period
   */
  public abstract double presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      T period);

  /**
   * Calculates the future value of a single payment period.
   * <p>
   * This returns the value of the period without discounting.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param period  the period to price
   * @return the future value of the period
   */
  public abstract double futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      T period);

}
