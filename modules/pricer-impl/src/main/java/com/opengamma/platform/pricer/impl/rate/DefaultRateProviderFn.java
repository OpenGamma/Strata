/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.collect.ArgChecker;
import com.opengamma.platform.finance.rate.FixedRate;
import com.opengamma.platform.finance.rate.IborAveragedRate;
import com.opengamma.platform.finance.rate.IborInterpolatedRate;
import com.opengamma.platform.finance.rate.IborRate;
import com.opengamma.platform.finance.rate.OvernightAveragedRate;
import com.opengamma.platform.finance.rate.OvernightCompoundedRate;
import com.opengamma.platform.finance.rate.Rate;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.RateProviderFn;

/**
 * Multiple dispatch for {@code RateProviderFn}.
 * <p>
 * Dispatches the rate request to the correct implementation.
 */
public class DefaultRateProviderFn
    implements RateProviderFn<Rate> {

  /**
   * Default implementation.
   */
  public static final DefaultRateProviderFn DEFAULT = new DefaultRateProviderFn(
      DefaultIborRateProviderFn.DEFAULT,
      DefaultIborInterpolatedRateProviderFn.DEFAULT,
      DefaultIborAveragedRateProviderFn.DEFAULT,
      DefaultOvernightCompoundedRateProviderFn.DEFAULT,
      DefaultOvernightAveragedRateProviderFn.DEFAULT);

  //-------------------------------------------------------------------------
  /**
   * Handle {@link IborRate}.
   */
  private RateProviderFn<IborRate> iborRateFn;
  /**
   * Handle {@link IborInterpolatedRate}.
   */
  private RateProviderFn<IborInterpolatedRate> iborInterpolatedRateFn;
  /**
   * Handle {@link IborAveragedRate}.
   */
  private RateProviderFn<IborAveragedRate> iborAveragedRateFn;
  /**
   * Handle {@link OvernightCompoundedRate}.
   */
  private RateProviderFn<OvernightCompoundedRate> overnightCompoundedRateFn;
  /**
   * Handle {@link OvernightAveragedRate}.
   */
  private RateProviderFn<OvernightAveragedRate> overnightAveragedRateFn;

  /**
   * Creates an instance.
   * 
   * @param iborRateFn  the rate provider for {@link IborRate}
   * @param iborInterpolatedRateFn  the rate provider for {@link IborInterpolatedRate}
   * @param iborAveragedRateFn  the rate provider for {@link IborAveragedRate}
   * @param overnightCompoundedRateFn  the rate provider for {@link OvernightCompoundedRate}
   * @param overnightAveragedRateFn  the rate provider for {@link OvernightAveragedRate}
   */
  public DefaultRateProviderFn(
      RateProviderFn<IborRate> iborRateFn,
      RateProviderFn<IborInterpolatedRate> iborInterpolatedRateFn,
      RateProviderFn<IborAveragedRate> iborAveragedRateFn,
      RateProviderFn<OvernightCompoundedRate> overnightCompoundedRateFn,
      RateProviderFn<OvernightAveragedRate> overnightAveragedRateFn) {
    super();
    this.iborRateFn = ArgChecker.notNull(iborRateFn, "iborRateFn");
    this.iborInterpolatedRateFn = ArgChecker.notNull(iborInterpolatedRateFn, "iborInterpolatedRateFn");
    this.iborAveragedRateFn = ArgChecker.notNull(iborAveragedRateFn, "iborAveragedRateFn");
    this.overnightCompoundedRateFn = ArgChecker.notNull(overnightCompoundedRateFn, "overnightCompoundedRateFn");
    this.overnightAveragedRateFn = ArgChecker.notNull(overnightAveragedRateFn, "overnightAveragedRateFn");
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      LocalDate valuationDate,
      Rate rate,
      LocalDate startDate,
      LocalDate endDate) {
    // dispatch by runtime type
    if (rate instanceof FixedRate) {
      return ((FixedRate) rate).getRate();
    } else if (rate instanceof IborRate) {
      return iborRateFn.rate(env, valuationDate, (IborRate) rate, startDate, endDate);
    } else if (rate instanceof IborInterpolatedRate) {
      return iborInterpolatedRateFn.rate(env, valuationDate, (IborInterpolatedRate) rate, startDate, endDate);
    } else if (rate instanceof IborAveragedRate) {
      return iborAveragedRateFn.rate(env, valuationDate, (IborAveragedRate) rate, startDate, endDate);
    } else if (rate instanceof OvernightCompoundedRate) {
      return overnightCompoundedRateFn.rate(env, valuationDate, (OvernightCompoundedRate) rate, startDate, endDate);
    } else if (rate instanceof OvernightAveragedRate) {
      return overnightAveragedRateFn.rate(env, valuationDate, (OvernightAveragedRate) rate, startDate, endDate);
    } else {
      throw new IllegalArgumentException("Unknown Rate type: " + rate.getClass().getSimpleName());
    }
  }

}
