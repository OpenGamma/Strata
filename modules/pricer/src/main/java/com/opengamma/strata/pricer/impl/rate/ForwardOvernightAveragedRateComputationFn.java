/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import com.opengamma.strata.product.rate.OvernightAveragedRateComputation;

/**
* Rate computation implementation for a rate based on a single overnight index that is arithmetically averaged.
* <p>
* The rate computation retrieves the rate at each fixing date in the period 
* from the {@link RatesProvider} and average them.
*/
public class ForwardOvernightAveragedRateComputationFn
    implements RateComputationFn<OvernightAveragedRateComputation> {

  /**
   * Default implementation.
   */
  public static final ForwardOvernightAveragedRateComputationFn DEFAULT = new ForwardOvernightAveragedRateComputationFn();

  /**
   * Creates an instance.
   */
  public ForwardOvernightAveragedRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      OvernightAveragedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndex index = computation.getIndex();
    OvernightIndexRates rates = provider.overnightIndexRates(index);
    LocalDate lastNonCutoffFixing = computation.getEndDate();
    int cutoffOffset = computation.getRateCutOffDays() > 1 ? computation.getRateCutOffDays() : 1;
    double accumulatedInterest = 0.0d;
    double accrualFactorTotal = 0.0d;
    // Cut-off period. Starting from the end as the cutoff period is defined as a lag from the end.
    // When the fixing period end-date is not a good business day in the index calendar, 
    // the last fixing end date will be after the fixing end-date.
    double cutoffAccrualFactor = 0.0;
    OvernightIndexObservation lastIndexObs = null;
    // cutoffOffset >= 1, so loop always runs at least once
    for (int i = 0; i < cutoffOffset; i++) {
      lastNonCutoffFixing = computation.getFixingCalendar().previous(lastNonCutoffFixing);
      lastIndexObs = computation.observeOn(lastNonCutoffFixing);
      accrualFactorTotal += lastIndexObs.getYearFraction();
      cutoffAccrualFactor += lastIndexObs.getYearFraction();
    }
    double forwardRateCutOff = rates.rate(lastIndexObs);
    accumulatedInterest += cutoffAccrualFactor * forwardRateCutOff;
    LocalDate currentFixingNonCutoff = computation.getStartDate();
    while (currentFixingNonCutoff.isBefore(lastNonCutoffFixing)) {
      // All dates involved in the period are computed. Potentially slow.
      // The fixing periods are added as long as their start date is (strictly) before the no cutoff period end-date.
      OvernightIndexObservation indexObs = computation.observeOn(currentFixingNonCutoff);
      double forwardRate = rates.rate(indexObs);
      accrualFactorTotal += indexObs.getYearFraction();
      accumulatedInterest += indexObs.getYearFraction() * forwardRate;
      currentFixingNonCutoff = computation.getFixingCalendar().next(currentFixingNonCutoff);
    }
    // final rate
    return accumulatedInterest / accrualFactorTotal;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      OvernightAveragedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndex index = computation.getIndex();
    OvernightIndexRates rates = provider.overnightIndexRates(index);
    LocalDate lastNonCutoffFixing = computation.getEndDate();
    int cutoffOffset = computation.getRateCutOffDays() > 1 ? computation.getRateCutOffDays() : 1;
    double accrualFactorTotal = 0.0d;
    // Cut-off period. Starting from the end as the cutoff period is defined as a lag from the end.
    // When the fixing period end-date is not a good business day in the index calendar, 
    // the last fixing end date will be after the fixing end-date.
    double cutoffAccrualFactor = 0.0;
    OvernightIndexObservation lastIndexObs = null;
    // cutoffOffset >= 1, so loop always runs at least once
    for (int i = 0; i < cutoffOffset; i++) {
      lastNonCutoffFixing = computation.getFixingCalendar().previous(lastNonCutoffFixing);
      lastIndexObs = computation.observeOn(lastNonCutoffFixing);
      accrualFactorTotal += lastIndexObs.getYearFraction();
      cutoffAccrualFactor += lastIndexObs.getYearFraction();
    }
    PointSensitivityBuilder combinedPointSensitivityBuilder = rates.ratePointSensitivity(lastIndexObs)
        .multipliedBy(cutoffAccrualFactor);

    LocalDate currentFixingNonCutoff = computation.getStartDate();
    while (currentFixingNonCutoff.isBefore(lastNonCutoffFixing)) {
      // All dates involved in the period are computed. Potentially slow.
      // The fixing periods are added as long as their start date is (strictly) before the no cutoff period end-date.
      OvernightIndexObservation indexObs = computation.observeOn(currentFixingNonCutoff);
      PointSensitivityBuilder forwardRateSensitivity = rates.ratePointSensitivity(indexObs)
          .multipliedBy(indexObs.getYearFraction());
      combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.combinedWith(forwardRateSensitivity);
      accrualFactorTotal += indexObs.getYearFraction();
      currentFixingNonCutoff = computation.getFixingCalendar().next(currentFixingNonCutoff);
    }
    return combinedPointSensitivityBuilder.multipliedBy(1.0 / accrualFactorTotal);
  }

  @Override
  public double explainRate(
      OvernightAveragedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    double rate = rate(computation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
