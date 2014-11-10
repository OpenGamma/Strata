/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricerfn.swap;

import java.time.LocalDate;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.swap.AccrualPeriod;
import com.opengamma.platform.finance.swap.RateAccrualPeriod;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.swap.AccrualPeriodPricerFn;

/**
 * Multiple dispatch for {@code AccrualPeriodPricerFn}.
 * <p>
 * Dispatches the pricer request to the correct implementation.
 */
public class StandardAccrualPeriodPricerFn
    implements AccrualPeriodPricerFn<AccrualPeriod> {

  /**
   * Default implementation.
   */
  public static final StandardAccrualPeriodPricerFn DEFAULT = new StandardAccrualPeriodPricerFn(
      StandardRateAccrualPeriodPricerFn.DEFAULT);

  //-------------------------------------------------------------------------
  /**
   * Handle {@link RateAccrualPeriod}.
   */
  private AccrualPeriodPricerFn<RateAccrualPeriod> rateAccrualPeriodFn;

  /**
   * Creates an instance.
   * 
   * @param rateAccrualPeriodFn  the rate provider for {@link RateAccrualPeriod}
   */
  public StandardAccrualPeriodPricerFn(
      AccrualPeriodPricerFn<RateAccrualPeriod> rateAccrualPeriodFn) {
    super();
    this.rateAccrualPeriodFn = ArgChecker.notNull(rateAccrualPeriodFn, "rateAccrualPeriodFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double presentValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      AccrualPeriod period,
      LocalDate paymentDate) {
    // dispatch by runtime type
    if (period instanceof RateAccrualPeriod) {
      return rateAccrualPeriodFn.presentValue(env, valuationDate, (RateAccrualPeriod) period, paymentDate);
    } else {
      throw new IllegalArgumentException("Unknown AccrualPeriod type: " + period.getClass().getSimpleName());
    }
  }

  @Override
  public double futureValue(
      PricingEnvironment env,
      LocalDate valuationDate,
      AccrualPeriod period) {
    // dispatch by runtime type
    if (period instanceof RateAccrualPeriod) {
      return rateAccrualPeriodFn.futureValue(env, valuationDate, (RateAccrualPeriod) period);
    } else {
      throw new IllegalArgumentException("Unknown AccrualPeriod type: " + period.getClass().getSimpleName());
    }
  }

}
