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
import com.opengamma.strata.market.value.IborIndexRates;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.IborAveragedFixing;
import com.opengamma.strata.product.rate.IborAveragedRateObservation;

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

    IborIndexRates rates = provider.iborIndexRates(observation.getIndex());

    // take (rate * weight) for each fixing and divide by total weight
    double weightedRate = observation.getFixings().stream()
        .mapToDouble(fixing -> weightedRate(fixing, rates))
        .sum();
    return weightedRate / observation.getTotalWeight();
  }

  // Compute the rate adjusted by the weight for one IborAverageFixing.
  private double weightedRate(IborAveragedFixing fixing, IborIndexRates rates) {
    double rate = fixing.getFixedRate().orElse(rates.rate(fixing.getFixingDate()));
    return rate * fixing.getWeight();
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      IborAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    IborIndexRates rates = provider.iborIndexRates(observation.getIndex());

    // combine the weighted sensitivity to each fixing
    // omit fixed rates as they have no sensitivity to a curve
    return observation.getFixings().stream()
        .filter(fixing -> !fixing.getFixedRate().isPresent())
        .map(fixing -> weightedSensitivity(fixing, observation.getTotalWeight(), rates))
        .reduce(PointSensitivityBuilder.none(), PointSensitivityBuilder::combinedWith);
  }

  // Compute the weighted sensitivity for one IborAverageFixing.
  private PointSensitivityBuilder weightedSensitivity(
      IborAveragedFixing fixing,
      double totalWeight,
      IborIndexRates rates) {

    return rates.ratePointSensitivity(fixing.getFixingDate())
        .multipliedBy(fixing.getWeight() / totalWeight);
  }

  @Override
  public double explainRate(
      IborAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    IborIndexRates rates = provider.iborIndexRates(observation.getIndex());
    for (IborAveragedFixing fixing : observation.getFixings()) {
      builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
          .put(ExplainKey.ENTRY_TYPE, "IborIndexObservation")
          .put(ExplainKey.FIXING_DATE, fixing.getFixingDate())
          .put(ExplainKey.INDEX, observation.getIndex())
          .put(ExplainKey.INDEX_VALUE, fixing.getFixedRate().orElse(rates.rate(fixing.getFixingDate())))
          .put(ExplainKey.WEIGHT, fixing.getWeight()));
    }
    double rate = rate(observation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
