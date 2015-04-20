/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.finance.rate.OvernightAveragedRateObservation;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
* Rate observation implementation for a rate based on a single overnight index that is arithmetically averaged.
* <p>
* The rate observation retrieve the rate at each fixing date in the period 
* from the {@link PricingEnvironment} and average them. 
*/
public class ForwardOvernightAveragedRateObservationFn
    implements RateObservationFn<OvernightAveragedRateObservation> {

  /**
   * Default implementation.
   */
  public static final ForwardOvernightAveragedRateObservationFn DEFAULT = new ForwardOvernightAveragedRateObservationFn();

  /**
   * Creates an instance.
   */
  public ForwardOvernightAveragedRateObservationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      OvernightAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    OvernightIndex index = observation.getIndex();
    LocalDate lastNonCutoffFixing = observation.getEndDate();
    int cutoffOffset = observation.getRateCutOffDays() > 1 ? observation.getRateCutOffDays() : 1;
    double accumulatedInterest = 0.0d;
    double accrualFactorTotal = 0.0d;
    // Cut-off period. Starting from the end as the cutoff period is defined as a lag from the end. 
    // When the fixing period end-date is not a good business day in the index calendar, 
    // the last fixing end date will be after the fixing end-date.
    double cutoffAccrualFactor = 0.0;
    for (int i = 0; i < cutoffOffset; i++) {
      lastNonCutoffFixing = index.getFixingCalendar().previous(lastNonCutoffFixing);
      LocalDate cutoffEffectiveDate = index.calculateEffectiveFromFixing(lastNonCutoffFixing);
      LocalDate cutoffMaturityDate = index.calculateMaturityFromEffective(cutoffEffectiveDate);
      double accrualFactor = index.getDayCount().yearFraction(cutoffEffectiveDate, cutoffMaturityDate);
      accrualFactorTotal += accrualFactor;
      cutoffAccrualFactor += accrualFactor;
    }
    double forwardRateCutOff = env.overnightIndexRate(index, lastNonCutoffFixing);
    accumulatedInterest += cutoffAccrualFactor * forwardRateCutOff;
    LocalDate currentFixingNonCutoff = observation.getStartDate();
    while (currentFixingNonCutoff.isBefore(lastNonCutoffFixing)) {
      // All dates involved in the period are computed. Potentially slow.
      // The fixing periods are added as long as their start date is (strictly) before the no cutoff period end-date.
      LocalDate currentOnRateStart = index.calculateEffectiveFromFixing(currentFixingNonCutoff);
      LocalDate currentOnRateEnd = index.calculateMaturityFromEffective(currentOnRateStart);
      double accrualFactor = index.getDayCount().yearFraction(currentOnRateStart, currentOnRateEnd);
      double forwardRate = env.overnightIndexRate(index, currentFixingNonCutoff);
      accrualFactorTotal += accrualFactor;
      accumulatedInterest += accrualFactor * forwardRate;
      currentFixingNonCutoff = index.getFixingCalendar().next(currentFixingNonCutoff);
    }
    // final rate
    return accumulatedInterest / accrualFactorTotal;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      PricingEnvironment env,
      OvernightAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    // TODO
    throw new UnsupportedOperationException("Rate sensitivity for OvernightIndex not currently supported");
  }

}
