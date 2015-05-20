/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.finance.rate.IborInterpolatedRateObservation;
import com.opengamma.strata.market.curve.IborIndexRates;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;

/**
 * Rate observation implementation for rate based on the weighted average of the fixing
 * on a single date of two IBOR-like indices.
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

    LocalDate fixingDate = observation.getFixingDate();
    IborIndex index1 = observation.getShortIndex();
    IborIndex index2 = observation.getLongIndex();
    double rate1 = provider.iborIndexRate(index1, fixingDate);
    double rate2 = provider.iborIndexRate(index2, fixingDate);
    DoublesPair weights = weights(index1, index2, fixingDate, endDate);
    return ((rate1 * weights.getFirst()) + (rate2 * weights.getSecond())) / (weights.getFirst() + weights.getSecond());
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      IborInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    LocalDate fixingDate = observation.getFixingDate();
    // computes the dates related to the underlying deposits associated to the indices
    IborIndex index1 = observation.getShortIndex();
    IborIndex index2 = observation.getLongIndex();
    DoublesPair weights = weights(index1, index2, fixingDate, endDate);
    double totalWeight = weights.getFirst() + weights.getSecond();

    IborIndexRates ratesIndex1 = provider.iborIndexRates(index1);
    PointSensitivityBuilder sens1 = ratesIndex1.pointSensitivity(fixingDate)
        .multipliedBy(weights.getFirst() / totalWeight);
    IborIndexRates ratesIndex2 = provider.iborIndexRates(index2);
    PointSensitivityBuilder sens2 = ratesIndex2.pointSensitivity(fixingDate)
        .multipliedBy(weights.getSecond() / totalWeight);
    return sens1.combinedWith(sens2);
  }

  // computes the weights related to the two indices
  private DoublesPair weights(IborIndex index1, IborIndex index2, LocalDate fixingDate, LocalDate endDate) {
    LocalDate fixingStartDate1 = index1.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate1 = index1.calculateMaturityFromEffective(fixingStartDate1);
    LocalDate fixingStartDate2 = index2.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate2 = index2.calculateMaturityFromEffective(fixingStartDate2);
    // weights: linear interpolation on the number of days between the fixing date and the maturity dates of the 
    //   actual coupons on one side and the maturity dates of the underlying deposit on the other side.
    long fixingEpochDay = fixingDate.toEpochDay();
    double days1 = fixingEndDate1.toEpochDay() - fixingEpochDay;
    double days2 = fixingEndDate2.toEpochDay() - fixingEpochDay;
    double daysN = endDate.toEpochDay() - fixingEpochDay;
    double weight1 = (days2 - daysN) / (days2 - days1);
    double weight2 = (daysN - days1) / (days2 - days1);
    return DoublesPair.of(weight1, weight2);
  }

}
