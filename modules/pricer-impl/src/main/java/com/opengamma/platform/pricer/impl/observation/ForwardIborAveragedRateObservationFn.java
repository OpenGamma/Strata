/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import java.time.LocalDate;

import com.opengamma.basics.index.IborIndex;
import com.opengamma.platform.finance.observation.IborAveragedFixing;
import com.opengamma.platform.finance.observation.IborAveragedRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;

/**
 * Rate observation implementation for a rate based on the average of multiple fixings of a
 * single IBOR-like floating rate index.
 * <p>
 * The rate observation query the rates from the PricingEnvironment and weighted-average them.
 * There is no convexity adjustment computed in this implementation.
 */
public class ForwardIborAveragedRateObservationFn 
    implements RateObservationFn<IborAveragedRateObservation> {
  
  /**
   * Default instance.
   */
  public static final ForwardIborAveragedRateObservationFn DEFAULT = new ForwardIborAveragedRateObservationFn();

  @Override
  public double rate(
      PricingEnvironment env, 
      IborAveragedRateObservation observation, 
      LocalDate startDate, 
      LocalDate endDate) {
    double totalWeight = observation.getFixings().stream()
        .mapToDouble(IborAveragedFixing::getWeight)
        .sum();
        double weightedRate = observation.getFixings().stream()
        .mapToDouble(fixing -> weightedRate(env, observation.getIndex(), fixing))
        .sum();
        return weightedRate / totalWeight;
  }
  
  /**
   * Compute the rate adjusted by the weight for one IborAverageFixing.
   * @param env The pricing environment. 
   * @param iborIndex The Ibor index for the fixing.
   * @param fixing The fixing observation used in the average.
   * @return The weighted rate.
   */
  private double weightedRate(
      PricingEnvironment env,
      IborIndex iborIndex,
      IborAveragedFixing fixing) {
    double rate = (fixing.getFixedRate().isPresent() ?
        fixing.getFixedRate().getAsDouble() : env.iborIndexRate(iborIndex, fixing.getFixingDate()));
    return rate * fixing.getWeight();
  }

}
