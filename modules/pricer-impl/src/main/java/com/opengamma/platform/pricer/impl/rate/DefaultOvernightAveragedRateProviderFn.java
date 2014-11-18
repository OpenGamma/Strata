/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.platform.finance.rate.OvernightAveragedRate;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.RateProviderFn;

/**
 * Rate provider implementation for a rate based on a single overnight index that is averaged.
 * <p>
 * The rate provider examines the historic time-series of known rates and the
 * forward curve to determine the effective annualized rate.
 */
public class DefaultOvernightAveragedRateProviderFn
    implements RateProviderFn<OvernightAveragedRate> {

  /**
   * Default implementation.
   */
  public static final DefaultOvernightAveragedRateProviderFn DEFAULT = new DefaultOvernightAveragedRateProviderFn();

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      LocalDate valuationDate,
      OvernightAveragedRate rate,
      LocalDate startDate,
      LocalDate endDate) {

    // TODO
    return 1d;
  }

}
