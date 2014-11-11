/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricerfn.swap;

import java.time.LocalDate;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.PaymentPeriod;
import com.opengamma.platform.finance.swap.RatePaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;

/**
 * Multiple dispatch for {@code AccrualPeriodPricerFn}.
 * <p>
 * Dispatches the pricer request to the correct implementation.
 */
public class StandardPaymentPeriodPricerFn
    implements PaymentPeriodPricerFn<PaymentPeriod> {

  /**
   * Default implementation.
   */
  public static final StandardPaymentPeriodPricerFn DEFAULT = new StandardPaymentPeriodPricerFn(
      StandardRatePaymentPeriodPricerFn.DEFAULT);

  //-------------------------------------------------------------------------
  /**
   * Handle {@link RatePaymentPeriod}.
   */
  private PaymentPeriodPricerFn<RatePaymentPeriod> ratePaymentPeriodFn;

  /**
   * Creates an instance.
   * 
   * @param ratePaymentPeriodFn  the rate provider for {@link RatePaymentPeriod}
   */
  public StandardPaymentPeriodPricerFn(
      PaymentPeriodPricerFn<RatePaymentPeriod> ratePaymentPeriodFn) {
    super();
    this.ratePaymentPeriodFn = ArgChecker.notNull(ratePaymentPeriodFn, "ratePaymentPeriodFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      PaymentPeriod period) {
    // dispatch by runtime type
    if (period instanceof RatePaymentPeriod) {
      return ratePaymentPeriodFn.presentValue(env, valuationDate, (RatePaymentPeriod) period);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + period.getClass().getSimpleName());
    }
  }

  @Override
  public double futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      PaymentPeriod period) {
    // dispatch by runtime type
    if (period instanceof RatePaymentPeriod) {
      return ratePaymentPeriodFn.futureValue(env, valuationDate, (RatePaymentPeriod) period);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + period.getClass().getSimpleName());
    }
  }

}
