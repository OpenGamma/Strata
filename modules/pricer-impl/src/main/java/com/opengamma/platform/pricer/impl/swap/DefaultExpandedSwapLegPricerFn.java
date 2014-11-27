/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.swap;

import java.time.LocalDate;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.ExpandedSwapLeg;
import com.opengamma.platform.finance.swap.PaymentPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.PaymentPeriodPricerFn;
import com.opengamma.platform.pricer.swap.SwapLegPricerFn;

/**
 * Pricer for expanded swap legs.
 */
public class DefaultExpandedSwapLegPricerFn implements SwapLegPricerFn<ExpandedSwapLeg> {

  /**
   * Default implementation.
   */
  public static final DefaultExpandedSwapLegPricerFn DEFAULT = new DefaultExpandedSwapLegPricerFn(
      DefaultPaymentPeriodPricerFn.DEFAULT);

  /**
   * Payment period pricer.
   */
  private final PaymentPeriodPricerFn<PaymentPeriod> paymentPeriodPricerFn;

  /**
   * Creates an instance.
   * 
   * @param paymentPeriodPricerFn  the pricer for {@link PaymentPeriod}
   */
  public DefaultExpandedSwapLegPricerFn(
      PaymentPeriodPricerFn<PaymentPeriod> paymentPeriodPricerFn) {
    this.paymentPeriodPricerFn = ArgChecker.notNull(paymentPeriodPricerFn, "paymentPeriodPricerFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(PricingEnvironment env, LocalDate valuationDate, ExpandedSwapLeg swapLeg) {
    return swapLeg.getPaymentPeriods().stream()
      .mapToDouble(p -> paymentPeriodPricerFn.presentValue(env, valuationDate, p))
      .sum();
  }

  @Override
  public double futureValue(PricingEnvironment env, LocalDate valuationDate, ExpandedSwapLeg swapLeg) {
    return swapLeg.getPaymentPeriods().stream()
        .mapToDouble(p -> paymentPeriodPricerFn.futureValue(env, valuationDate, p))
        .sum();
  }

}
