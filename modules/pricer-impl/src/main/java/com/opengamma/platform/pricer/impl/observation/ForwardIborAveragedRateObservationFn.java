/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import java.time.LocalDate;

import com.opengamma.basics.index.IborIndex;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.observation.IborAveragedFixing;
import com.opengamma.platform.finance.observation.IborAveragedRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;

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
  public Pair<Double, MulticurveSensitivity3LD> rateMulticurveSensitivity3LD(
      PricingEnvironment env,
      IborAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    return null;
  }

}
