/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.platform.finance.rate.IborRate;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.RateProviderFn;

/**
 * Rate provider implementation for an IBOR-like index.
 * <p>
 * The rate provider examines the historic time-series of known rates and the
 * forward curve to determine the effective annualized rate.
 */
public class StandardIborRateProviderFn
    implements RateProviderFn<IborRate> {

  /**
   * Default implementation.
   */
  public static final StandardIborRateProviderFn DEFAULT = new StandardIborRateProviderFn();

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      LocalDate valuationDate,
      IborRate rate,
      LocalDate startDate,
      LocalDate endDate) {

    return env.indexRate(rate.getIndex(), valuationDate, rate.getFixingDate());
  }

}
