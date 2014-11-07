/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.swap;

import java.time.LocalDate;

import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.swap.SwapPaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;

/**
 * Pricer for a single swap payment period.
 * <p>
 * Implementations must be immutable and thread-safe functions.
 */
public interface SwapPaymentPeriodPricerFn {

  /**
   * Calculates the present value of the payment period.
   * 
   * @param env  the pricing environment
   * @param valuationDate  the valuation date
   * @param paymentPeriod  the payment period to price
   * @return the present value of the swap
   */
  public abstract CurrencyAmount presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      SwapPaymentPeriod paymentPeriod);

}
