/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.finance.rate.IborAveragedFixing;
import com.opengamma.strata.finance.rate.IborAveragedRateObservation;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
 * Rate observation implementation for a rate based on the average of multiple fixings of a
 * single IBOR-like floating rate index.
 * <p>
 * The rate observation query the rates from the {@code PricingEnvironment} and weighted-average them.
 * There is no convexity adjustment computed in this implementation.
 */
public class ForwardIborAveragedRateObservationFn
    implements RateObservationFn<IborAveragedRateObservation> {

  /**
   * Default instance.
   */
  public static final ForwardIborAveragedRateObservationFn DEFAULT = new ForwardIborAveragedRateObservationFn();

  /**
   * Creates an instance.
   */
  public ForwardIborAveragedRateObservationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      IborAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    
    // take (rate * weight) for each fixing and divide by total weight
    double weightedRate = observation.getFixings().stream()
        .mapToDouble(fixing -> weightedRate(env, observation.getIndex(), fixing))
        .sum();
    return weightedRate / observation.getTotalWeight();
  }

  // Compute the rate adjusted by the weight for one IborAverageFixing.
  private double weightedRate(PricingEnvironment env, IborIndex iborIndex, IborAveragedFixing fixing) {
    double rate = fixing.getFixedRate().orElse(env.iborIndexRate(iborIndex, fixing.getFixingDate()));
    return rate * fixing.getWeight();
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      PricingEnvironment env,
      IborAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {

    // combine the weighted sensitivity to each fixing
    // omit fixed rates as they have no sensitivity to a curve
    return observation.getFixings().stream()
        .filter(fixing -> !fixing.getFixedRate().isPresent())
        .map(fixing -> weightedSensitivity(env, observation, fixing))
        .reduce(PointSensitivityBuilder.none(), PointSensitivityBuilder::combinedWith);
  }

  // Compute the weighted sensitivity for one IborAverageFixing.
  private PointSensitivityBuilder weightedSensitivity(
      PricingEnvironment env,
      IborAveragedRateObservation observation,
      IborAveragedFixing fixing) {

    return env.iborIndexRateSensitivity(observation.getIndex(), fixing.getFixingDate())
        .multipliedBy(fixing.getWeight() / observation.getTotalWeight());
  }

}
