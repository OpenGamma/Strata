/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.view.IborIndexRates;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.IborInterpolatedRateObservation;
import com.opengamma.strata.product.rate.IborRateObservation;

/**
 * Rate observation implementation for rate based on the weighted average of the fixing
 * on a single date of two Ibor indices.
 * <p>
 * The rate observation query the rates from the {@code RatesProvider} and average them.
 * There is no convexity adjustment computed in this implementation.
 */
public class ForwardIborInterpolatedRateObservationFn
    implements RateObservationFn<IborInterpolatedRateObservation> {

  /**
   * Default instance.
   */
  public static final ForwardIborInterpolatedRateObservationFn DEFAULT = new ForwardIborInterpolatedRateObservationFn();

  /**
   * Creates an instance.
   */
  public ForwardIborInterpolatedRateObservationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      IborInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    IborRateObservation obs1 = observation.getShortObservation();
    IborRateObservation obs2 = observation.getLongObservation();
    IborIndexRates rates1 = provider.iborIndexRates(obs1.getIndex());
    IborIndexRates rates2 = provider.iborIndexRates(obs2.getIndex());

    double rate1 = rates1.rate(obs1);
    double rate2 = rates2.rate(obs2);
    DoublesPair weights = weights(obs1, obs2, endDate);
    return ((rate1 * weights.getFirst()) + (rate2 * weights.getSecond())) / (weights.getFirst() + weights.getSecond());
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      IborInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    // computes the dates related to the underlying deposits associated to the indices
    IborRateObservation obs1 = observation.getShortObservation();
    IborRateObservation obs2 = observation.getLongObservation();
    DoublesPair weights = weights(obs1, obs2, endDate);
    double totalWeight = weights.getFirst() + weights.getSecond();

    IborIndexRates ratesIndex1 = provider.iborIndexRates(obs1.getIndex());
    PointSensitivityBuilder sens1 = ratesIndex1.ratePointSensitivity(obs1)
        .multipliedBy(weights.getFirst() / totalWeight);
    IborIndexRates ratesIndex2 = provider.iborIndexRates(obs2.getIndex());
    PointSensitivityBuilder sens2 = ratesIndex2.ratePointSensitivity(obs2)
        .multipliedBy(weights.getSecond() / totalWeight);
    return sens1.combinedWith(sens2);
  }

  @Override
  public double explainRate(
      IborInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    IborRateObservation obs1 = observation.getShortObservation();
    IborRateObservation obs2 = observation.getLongObservation();
    double rate1 = provider.iborIndexRates(obs1.getIndex()).rate(obs1);
    double rate2 = provider.iborIndexRates(obs2.getIndex()).rate(obs2);
    DoublesPair weights = weights(obs1, obs2, endDate);
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "IborIndexObservation")
        .put(ExplainKey.FIXING_DATE, obs1.getFixingDate())
        .put(ExplainKey.INDEX, obs1.getIndex())
        .put(ExplainKey.INDEX_VALUE, rate1)
        .put(ExplainKey.WEIGHT, weights.getFirst()));
    builder.addListEntry(ExplainKey.OBSERVATIONS, child -> child
        .put(ExplainKey.ENTRY_TYPE, "IborIndexObservation")
        .put(ExplainKey.FIXING_DATE, obs2.getFixingDate())
        .put(ExplainKey.INDEX, obs2.getIndex())
        .put(ExplainKey.INDEX_VALUE, rate2)
        .put(ExplainKey.WEIGHT, weights.getSecond()));
    double rate = rate(observation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

  // computes the weights related to the two indices
  private DoublesPair weights(IborRateObservation obs1, IborRateObservation obs2, LocalDate endDate) {
    // weights: linear interpolation on the number of days between the fixing date and the maturity dates of the 
    //   actual coupons on one side and the maturity dates of the underlying deposit on the other side.
    long fixingEpochDay = obs1.getFixingDate().toEpochDay();
    double days1 = obs1.getMaturityDate().toEpochDay() - fixingEpochDay;
    double days2 = obs2.getMaturityDate().toEpochDay() - fixingEpochDay;
    double daysN = endDate.toEpochDay() - fixingEpochDay;
    double weight1 = (days2 - daysN) / (days2 - days1);
    double weight2 = (daysN - days1) / (days2 - days1);
    return DoublesPair.of(weight1, weight2);
  }

}
