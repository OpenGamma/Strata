/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.swap.RatePaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for a single rate payment period.
 * <p>
 * Defines the values that can be calculated on a swap leg payment period.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public interface RatePaymentPeriodPricerFn {

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
  public abstract CurrencyAmount presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      RatePaymentPeriod period);

}
