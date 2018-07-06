/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.rate.OvernightIndexRates;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.OvernightAveragedDailyRateComputation;

/**
* Rate computation implementation for an averaged daily rate for a single Overnight index.
* <p>
* The rate computation retrieves the rate at each fixing date in the period 
* from the {@link RatesProvider} and average them.
*/
public class ForwardOvernightAveragedDailyRateComputationFn
    implements RateComputationFn<OvernightAveragedDailyRateComputation> {

  /**
   * Default implementation.
   */
  public static final ForwardOvernightAveragedDailyRateComputationFn DEFAULT =
      new ForwardOvernightAveragedDailyRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardOvernightAveragedDailyRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      OvernightAveragedDailyRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndex index = computation.getIndex();
    OvernightIndexRates rates = provider.overnightIndexRates(index);
    LocalDate lastFixingDate = computation.getEndDate();
    double interestSum = 0d;
    int numberOfDays = 0;
    LocalDate currentFixingDate = computation.getStartDate();
    while (!currentFixingDate.isAfter(lastFixingDate)) {
      LocalDate referenceFixingDate = computation.getFixingCalendar().previousOrSame(currentFixingDate);
      OvernightIndexObservation indexObs = computation.observeOn(referenceFixingDate);
      double forwardRate = rates.rate(indexObs);
      interestSum += forwardRate;
      numberOfDays++;
      currentFixingDate = currentFixingDate.plusDays(1);
    }

    return interestSum / numberOfDays;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      OvernightAveragedDailyRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndex index = computation.getIndex();
    OvernightIndexRates rates = provider.overnightIndexRates(index);
    LocalDate lastFixingDate = computation.getEndDate();
    PointSensitivityBuilder pointSensitivityBuilder = PointSensitivityBuilder.none();
    int numberOfDays = 0;
    LocalDate currentFixingDate = computation.getStartDate();
    while (!currentFixingDate.isAfter(lastFixingDate)) {
      LocalDate referenceFixingDate = computation.getFixingCalendar().previousOrSame(currentFixingDate);
      OvernightIndexObservation indexObs = computation.observeOn(referenceFixingDate);
      PointSensitivityBuilder forwardRateSensitivity = rates.ratePointSensitivity(indexObs);
      pointSensitivityBuilder = pointSensitivityBuilder.combinedWith(forwardRateSensitivity);
      numberOfDays++;
      currentFixingDate = currentFixingDate.plusDays(1);
    }

    return pointSensitivityBuilder.multipliedBy(1d / numberOfDays);
  }

  @Override
  public double explainRate(
      OvernightAveragedDailyRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    double rate = rate(computation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
