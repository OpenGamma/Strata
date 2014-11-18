/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.basics.index.IborIndex;
import com.opengamma.platform.finance.rate.IborAveragedFixing;
import com.opengamma.platform.finance.rate.IborAveragedRate;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.RateProviderFn;

/**
 * Rate provider implementation for a rate based on the average of multiple fixings of a
 * single IBOR-like floating rate index.
 * <p>
 * The rate provider examines the historic time-series of known rates and the
 * forward curve to determine the effective annualized rate.
 */
public class StandardIborAveragedRateProviderFn
    implements RateProviderFn<IborAveragedRate> {

  /**
   * Default implementation.
   */
  public static final StandardIborAveragedRateProviderFn DEFAULT = new StandardIborAveragedRateProviderFn();

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      LocalDate valuationDate,
      IborAveragedRate rate,
      LocalDate startDate,
      LocalDate endDate) {
    
    double totalWeight = rate.getFixings().stream()
        .mapToDouble(IborAveragedFixing::getWeight)
        .sum();
    double weightedRate = rate.getFixings().stream()
        .mapToDouble(fixing -> weightedRate(env, valuationDate, rate.getIndex(), fixing))
        .sum();
    return weightedRate / totalWeight;
  }

  // calculate the rate and multiply it by the weight
  private double weightedRate(
      PricingEnvironment env,
      LocalDate valuationDate,
      IborIndex iborIndex,
      IborAveragedFixing fixing) {
    
    double rate = (fixing.getFixedRate() != null ?
        fixing.getFixedRate() : env.indexRate(iborIndex, valuationDate, fixing.getFixingDate()));
    return rate * fixing.getWeight();
  }

}
