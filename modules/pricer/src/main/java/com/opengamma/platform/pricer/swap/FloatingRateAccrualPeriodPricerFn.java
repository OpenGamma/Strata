/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.swap;

import java.time.LocalDate;

import com.opengamma.platform.finance.swap.FloatingRateAccrualPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for floating rate accrual periods.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public interface FloatingRateAccrualPeriodPricerFn {

  /**
   * Calculates the present value of a single accrual period.
   * <p>
   * This returns the value of the period with discounting.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param period  the period to price
   * @param paymentDate  the date that the accrual will be paid on
   * @return the present value of the period
   */
  public abstract double presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      FloatingRateAccrualPeriod period,
      LocalDate paymentDate);

  /**
   * Calculates the future value of a single accrual period.
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
      FloatingRateAccrualPeriod period);

}
