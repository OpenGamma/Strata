/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.IborIndexRates;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.IborInterpolatedRateComputation;

/**
 * Rate computation implementation for rate based on the weighted average of the fixing
 * on a single date of two Ibor indices.
 * <p>
 * The rate computation queries the rates from the {@code RatesProvider} and interpolates them.
 * There is no convexity adjustment computed in this implementation.
 */
public class ForwardIborInterpolatedRateComputationFn
    implements RateComputationFn<IborInterpolatedRateComputation> {

  /**
   * Default instance.
   */
  public static final ForwardIborInterpolatedRateComputationFn DEFAULT = new ForwardIborInterpolatedRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardIborInterpolatedRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      IborInterpolatedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    IborIndexObservation obs1 = computation.getShortObservation();
    IborIndexObservation obs2 = computation.getLongObservation();
    IborIndexRates rates1 = provider.iborIndexRates(obs1.getIndex());
    IborIndexRates rates2 = provider.iborIndexRates(obs2.getIndex());

    double rate1 = rates1.rate(obs1);
    double rate2 = rates2.rate(obs2);
    DoublesPair weights = weights(obs1, obs2, endDate);
    return ((rate1 * weights.getFirst()) + (rate2 * weights.getSecond())) / (weights.getFirst() + weights.getSecond());
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      IborInterpolatedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    // computes the dates related to the underlying deposits associated to the indices
    IborIndexObservation obs1 = computation.getShortObservation();
    IborIndexObservation obs2 = computation.getLongObservation();
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
      IborInterpolatedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    IborIndexObservation obs1 = computation.getShortObservation();
    IborIndexObservation obs2 = computation.getLongObservation();
    DoublesPair weights = weights(obs1, obs2, endDate);
    IborIndexRates rates1 = provider.iborIndexRates(obs1.getIndex());
    IborIndexRates rates2 = provider.iborIndexRates(obs2.getIndex());
    rates1.explainRate(obs1, builder, child -> child.put(ExplainKey.WEIGHT, weights.getFirst()));
    rates2.explainRate(obs2, builder, child -> child.put(ExplainKey.WEIGHT, weights.getSecond()));
    double rate = rate(computation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

  // computes the weights related to the two indices
  private DoublesPair weights(IborIndexObservation obs1, IborIndexObservation obs2, LocalDate endDate) {
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
