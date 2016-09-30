/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.IborAveragedFixing;
import com.opengamma.strata.product.rate.IborAveragedRateComputation;

/**
 * Rate computation implementation for a rate based on the average of multiple fixings of a
 * single Ibor floating rate index.
 * <p>
 * The rate computation queries the rates from the {@code RatesProvider} and weighted-average them.
 * There is no convexity adjustment computed in this implementation.
 */
public class ForwardIborAveragedRateComputationFn
    implements RateComputationFn<IborAveragedRateComputation> {

  /**
   * Default instance.
   */
  public static final ForwardIborAveragedRateComputationFn DEFAULT = new ForwardIborAveragedRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardIborAveragedRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      IborAveragedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    IborIndexRates rates = provider.iborIndexRates(computation.getIndex());

    // take (rate * weight) for each fixing and divide by total weight
    double weightedRate = computation.getFixings().stream()
        .mapToDouble(fixing -> weightedRate(fixing, rates))
        .sum();
    return weightedRate / computation.getTotalWeight();
  }

  // Compute the rate adjusted by the weight for one IborAverageFixing.
  private double weightedRate(IborAveragedFixing fixing, IborIndexRates rates) {
    double rate = fixing.getFixedRate().orElse(rates.rate(fixing.getObservation()));
    return rate * fixing.getWeight();
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      IborAveragedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    IborIndexRates rates = provider.iborIndexRates(computation.getIndex());

    // combine the weighted sensitivity to each fixing
    // omit fixed rates as they have no sensitivity to a curve
    return computation.getFixings().stream()
        .filter(fixing -> !fixing.getFixedRate().isPresent())
        .map(fixing -> weightedSensitivity(fixing, computation.getTotalWeight(), rates))
        .reduce(PointSensitivityBuilder.none(), PointSensitivityBuilder::combinedWith);
  }

  // Compute the weighted sensitivity for one IborAverageFixing.
  private PointSensitivityBuilder weightedSensitivity(
      IborAveragedFixing fixing,
      double totalWeight,
      IborIndexRates rates) {

    return rates.ratePointSensitivity(fixing.getObservation())
        .multipliedBy(fixing.getWeight() / totalWeight);
  }

  @Override
  public double explainRate(
      IborAveragedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    IborIndexRates rates = provider.iborIndexRates(computation.getIndex());
    for (IborAveragedFixing fixing : computation.getFixings()) {
      rates.explainRate(fixing.getObservation(), builder, child -> child.put(ExplainKey.WEIGHT, fixing.getWeight()));
    }
    double rate = rate(computation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
