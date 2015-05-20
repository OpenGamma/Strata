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
import com.opengamma.strata.market.curve.IborIndexRates;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Rate observation implementation for a rate based on the average of multiple fixings of a
 * single IBOR-like floating rate index.
 * <p>
 * The rate observation query the rates from the {@code RatesProvider} and weighted-average them.
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
      IborAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    // take (rate * weight) for each fixing and divide by total weight
    double weightedRate = observation.getFixings().stream()
        .mapToDouble(fixing -> weightedRate(observation.getIndex(), fixing, provider))
        .sum();
    return weightedRate / observation.getTotalWeight();
  }

  // Compute the rate adjusted by the weight for one IborAverageFixing.
  private double weightedRate(IborIndex iborIndex, IborAveragedFixing fixing, RatesProvider provider) {
    double rate = fixing.getFixedRate().orElse(provider.iborIndexRate(iborIndex, fixing.getFixingDate()));
    return rate * fixing.getWeight();
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      IborAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    // combine the weighted sensitivity to each fixing
    // omit fixed rates as they have no sensitivity to a curve
    return observation.getFixings().stream()
        .filter(fixing -> !fixing.getFixedRate().isPresent())
        .map(fixing -> weightedSensitivity(observation, fixing, provider))
        .reduce(PointSensitivityBuilder.none(), PointSensitivityBuilder::combinedWith);
  }

  // Compute the weighted sensitivity for one IborAverageFixing.
  private PointSensitivityBuilder weightedSensitivity(
      IborAveragedRateObservation observation,
      IborAveragedFixing fixing,
      RatesProvider provider) {

    IborIndexRates rates = provider.iborIndexRates(observation.getIndex());
    return rates.pointSensitivity(fixing.getFixingDate())
        .multipliedBy(fixing.getWeight() / observation.getTotalWeight());
  }

}
