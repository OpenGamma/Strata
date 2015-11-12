/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;

import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.OvernightIndexRates;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.OvernightAveragedRateObservation;

/**
* Rate observation implementation for a rate based on a single overnight index that is arithmetically averaged.
* <p>
* The rate observation retrieve the rate at each fixing date in the period 
* from the {@link RatesProvider} and average them. 
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
      OvernightAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndex index = observation.getIndex();
    OvernightIndexRates rates = provider.overnightIndexRates(index);
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
    double forwardRateCutOff = rates.rate(lastNonCutoffFixing);
    accumulatedInterest += cutoffAccrualFactor * forwardRateCutOff;
    LocalDate currentFixingNonCutoff = observation.getStartDate();
    while (currentFixingNonCutoff.isBefore(lastNonCutoffFixing)) {
      // All dates involved in the period are computed. Potentially slow.
      // The fixing periods are added as long as their start date is (strictly) before the no cutoff period end-date.
      LocalDate currentOnRateStart = index.calculateEffectiveFromFixing(currentFixingNonCutoff);
      LocalDate currentOnRateEnd = index.calculateMaturityFromEffective(currentOnRateStart);
      double accrualFactor = index.getDayCount().yearFraction(currentOnRateStart, currentOnRateEnd);
      double forwardRate = rates.rate(currentFixingNonCutoff);
      accrualFactorTotal += accrualFactor;
      accumulatedInterest += accrualFactor * forwardRate;
      currentFixingNonCutoff = index.getFixingCalendar().next(currentFixingNonCutoff);
    }
    // final rate
    return accumulatedInterest / accrualFactorTotal;
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      OvernightAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndex index = observation.getIndex();
    OvernightIndexRates rates = provider.overnightIndexRates(index);
    LocalDate lastNonCutoffFixing = observation.getEndDate();
    int cutoffOffset = observation.getRateCutOffDays() > 1 ? observation.getRateCutOffDays() : 1;
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
    PointSensitivityBuilder combinedPointSensitivityBuilder = rates.ratePointSensitivity(lastNonCutoffFixing);
    combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.multipliedBy(cutoffAccrualFactor);

    LocalDate currentFixingNonCutoff = observation.getStartDate();
    while (currentFixingNonCutoff.isBefore(lastNonCutoffFixing)) {
      // All dates involved in the period are computed. Potentially slow.
      // The fixing periods are added as long as their start date is (strictly) before the no cutoff period end-date.
      LocalDate currentOnRateStart = index.calculateEffectiveFromFixing(currentFixingNonCutoff);
      LocalDate currentOnRateEnd = index.calculateMaturityFromEffective(currentOnRateStart);
      double accrualFactor = index.getDayCount().yearFraction(currentOnRateStart, currentOnRateEnd);
      PointSensitivityBuilder forwardRateSensitivity = rates.ratePointSensitivity(currentFixingNonCutoff);
      forwardRateSensitivity = forwardRateSensitivity.multipliedBy(accrualFactor);
      combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.combinedWith(forwardRateSensitivity);
      accrualFactorTotal += accrualFactor;
      currentFixingNonCutoff = index.getFixingCalendar().next(currentFixingNonCutoff);
    }
    combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.multipliedBy(1.0 / accrualFactorTotal);
    return combinedPointSensitivityBuilder;
  }

  @Override
  public double explainRate(
      OvernightAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider,
      ExplainMapBuilder builder) {

    double rate = rate(observation, startDate, endDate, provider);
    builder.put(ExplainKey.COMBINED_RATE, rate);
    return rate;
  }

}
