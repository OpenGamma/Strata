/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.rate.swap.PaymentPeriod;
import com.opengamma.platform.finance.rate.swap.RatePaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;

/**
 * Pricer implementation for payment periods using multiple dispatch.
 * <p>
 * Dispatches the request to the correct implementation.
 */
public class DispatchingPaymentPeriodPricerFn
    implements PaymentPeriodPricerFn<PaymentPeriod> {

  /**
   * Default implementation.
   */
  public static final DispatchingPaymentPeriodPricerFn DEFAULT = new DispatchingPaymentPeriodPricerFn(
      DiscountingRatePaymentPeriodPricerFn.DEFAULT);

  /**
   * Pricer for {@link RatePaymentPeriod}.
   */
  private final PaymentPeriodPricerFn<RatePaymentPeriod> ratePaymentPeriodPricerFn;

  /**
   * Creates an instance.
   * 
   * @param ratePaymentPeriodPricerFn  the pricer for {@link RatePaymentPeriod}
   */
  public DispatchingPaymentPeriodPricerFn(
      PaymentPeriodPricerFn<RatePaymentPeriod> ratePaymentPeriodPricerFn) {
    this.ratePaymentPeriodPricerFn = ArgChecker.notNull(ratePaymentPeriodPricerFn, "ratePaymentPeriodPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, PaymentPeriod paymentPeriod) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricerFn.presentValue(env, (RatePaymentPeriod) paymentPeriod);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

  @Override
  public double futureValue(PricingEnvironment env, PaymentPeriod paymentPeriod) {
    // dispatch by runtime type
    if (paymentPeriod instanceof RatePaymentPeriod) {
      return ratePaymentPeriodPricerFn.futureValue(env, (RatePaymentPeriod) paymentPeriod);
    } else {
      throw new IllegalArgumentException("Unknown PaymentPeriod type: " + paymentPeriod.getClass().getSimpleName());
    }
  }

}
