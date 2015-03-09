/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import java.time.LocalDate;

import com.opengamma.basics.index.IborIndex;
import com.opengamma.collect.tuple.Pair;
import com.opengamma.platform.finance.observation.IborInterpolatedRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;
import com.opengamma.platform.pricer.sensitivity.multicurve.MulticurveSensitivity3LD;

/**
 * Rate observation implementation for rate based on the weighted average of the fixing
 * on a single date of two IBOR-like indices.
 * <p>
 * The rate observation query the rates from the {@code PricingEnvironment} and average them.
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
      PricingEnvironment env,
      IborInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    LocalDate fixingDate = observation.getFixingDate();
    // computes the dates related to the underlying deposits associated to the indices
    IborIndex index1 = observation.getShortIndex();
    IborIndex index2 = observation.getLongIndex();
    LocalDate fixingStartDate1 = index1.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate1 = index1.calculateMaturityFromEffective(fixingStartDate1);
    LocalDate fixingStartDate2 = index2.calculateEffectiveFromFixing(fixingDate);
    LocalDate fixingEndDate2 = index2.calculateMaturityFromEffective(fixingStartDate2);
    // rate is the weighted average of the two rates related to the underlying indices
    double rate1 = env.iborIndexRate(index1, fixingDate);
    double rate2 = env.iborIndexRate(index2, fixingDate);
    // weights: linear interpolation on the number of days between the fixing date and the maturity dates of the 
    //   actual coupons on one side and the maturity dates of the underlying deposit on the other side.
    long fixingEpochDay = fixingDate.toEpochDay();
    double days1 = fixingEndDate1.toEpochDay() - fixingEpochDay;
    double days2 = fixingEndDate2.toEpochDay() - fixingEpochDay;
    double daysN = endDate.toEpochDay() - fixingEpochDay;
    double weight1 = (days2 - daysN) / (days2 - days1);
    double weight2 = (daysN - days1) / (days2 - days1);
    return ((rate1 * weight1) + (rate2 * weight2)) / (weight1 + weight2);
  }

  @Override
  public Pair<Double, MulticurveSensitivity3LD> rateMulticurveSensitivity3LD(
      PricingEnvironment env,
      IborInterpolatedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    return null;
  }

}
