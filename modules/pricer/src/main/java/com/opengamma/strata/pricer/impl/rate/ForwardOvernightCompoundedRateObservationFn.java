/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;
import java.util.OptionalDouble;

import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.finance.rate.OvernightCompoundedRateObservation;
import com.opengamma.strata.pricer.PricingEnvironment;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.sensitivity.PointSensitivityBuilder;

/**
* Rate observation implementation for a rate based on a single overnight index that is compounded.
* <p>
* Rates that are already fixed are retrieved from the time series of the {@link PricingEnvironment}.
* Rates that are in the future and not in the cut-off period are computed as unique forward rate in the full future period.
* Rates that are in the cut-off period (already fixed or forward) are compounded.
*/
public class ForwardOvernightCompoundedRateObservationFn
    implements RateObservationFn<OvernightCompoundedRateObservation> {

  /**
   * Default implementation.
   */
  public static final ForwardOvernightCompoundedRateObservationFn DEFAULT =
      new ForwardOvernightCompoundedRateObservationFn();

  /**
   * Creates an instance.
   */
  public ForwardOvernightCompoundedRateObservationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      OvernightCompoundedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    return new ObservationDetails(env, observation).calculateRate();
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      PricingEnvironment env,
      OvernightCompoundedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    // TODO
    throw new UnsupportedOperationException("Rate sensitivity for OvernightIndex not currently supported");
  }

  //-------------------------------------------------------------------------
  // Internal class. Observation details stored in a separate class to clarify the construction.
  private static class ObservationDetails {

    private final PricingEnvironment env;
    private final HolidayCalendar fixingCalendar;
    private final OvernightIndex index;
    private final LocalDateDoubleTimeSeries indexFixingDateSeries;
    private final int cutoffOffset;
    private final LocalDate firstFixing; // The date of the first fixing
    private final LocalDate lastFixingP1; // The date after the last fixing
    private final LocalDate lastFixing; // The date of the last fixing
    private final LocalDate lastFixingNonCutoff; // The last fixing not in the cutoff period.
    private final double accrualFactorTotal; // Total accrual factor
    private final double[] accrualFactorCutoff; // Accrual factors for the sub-periods using the cutoff rate.
    private LocalDate nextFixing; // Running variable through the different methods: next fixing date to be analyzed

    private ObservationDetails(PricingEnvironment env, OvernightCompoundedRateObservation observation) {
      this.env = env;
      index = observation.getIndex();
      fixingCalendar = index.getFixingCalendar();
      indexFixingDateSeries = env.timeSeries(index);
      // Details of the cutoff period
      firstFixing = observation.getStartDate();
      lastFixingP1 = observation.getEndDate();
      lastFixing = fixingCalendar.previous(lastFixingP1);
      cutoffOffset = Math.max(observation.getRateCutOffDays(), 1);
      accrualFactorCutoff = new double[cutoffOffset - 1];
      LocalDate currentFixing = lastFixing;
      for (int i = 0; i < cutoffOffset - 1; i++) {
        currentFixing = fixingCalendar.previous(currentFixing);
        LocalDate effectiveDate = index.calculateEffectiveFromFixing(currentFixing);
        LocalDate maturityDate = index.calculateMaturityFromEffective(effectiveDate);
        accrualFactorCutoff[i] = index.getDayCount().yearFraction(effectiveDate, maturityDate);
      }
      lastFixingNonCutoff = currentFixing;
      LocalDate startUnderlyingPeriod = index.calculateEffectiveFromFixing(firstFixing);
      LocalDate endUnderlyingPeriod =
          index.calculateMaturityFromEffective(index.calculateEffectiveFromFixing(lastFixing));
      accrualFactorTotal = index.getDayCount().yearFraction(startUnderlyingPeriod, endUnderlyingPeriod);
    }

    // Composition - publication strictly before valuation date: try accessing fixing time-series
    private double pastCompositionFactor() {
      double compositionFactor = 1.0d;
      LocalDate currentFixing = firstFixing;
      LocalDate currentPublication = index.calculatePublicationFromFixing(currentFixing);
      while ((currentFixing.isBefore(lastFixingNonCutoff)) && // fixing in the non-cutoff period
          env.getValuationDate().isAfter(currentPublication)) { // publication before valuation
        LocalDate effectiveDate = index.calculateEffectiveFromFixing(currentFixing);
        LocalDate maturityDate = index.calculateMaturityFromEffective(effectiveDate);
        double accrualFactor = index.getDayCount().yearFraction(effectiveDate, maturityDate);
        compositionFactor *= 1.0d + accrualFactor * checkedFixing(currentFixing, indexFixingDateSeries, index);
        currentFixing = fixingCalendar.next(currentFixing);
        currentPublication = index.calculatePublicationFromFixing(currentFixing);
      }
      if (currentFixing.equals(lastFixingNonCutoff) && // fixing is on the last non-cutoff date, cutoff period known
          env.getValuationDate().isAfter(currentPublication)) { // publication before valuation
        double rate = checkedFixing(currentFixing, indexFixingDateSeries, index);
        LocalDate effectiveDate = index.calculateEffectiveFromFixing(currentFixing);
        LocalDate maturityDate = index.calculateMaturityFromEffective(effectiveDate);
        double accrualFactor = index.getDayCount().yearFraction(effectiveDate, maturityDate);
        compositionFactor *= 1.0d + accrualFactor * rate;
        for (int i = 0; i < cutoffOffset - 1; i++) {
          compositionFactor *= 1.0d + accrualFactorCutoff[i] * rate;
        }
        currentFixing = fixingCalendar.next(currentFixing);
      }
      nextFixing = currentFixing;
      return compositionFactor;
    }

    // Composition - publication on valuation date: Check if a fixing is available on current date
    private double valuationCompositionFactor() {
      LocalDate currentFixing = nextFixing;
      LocalDate currentPublication = index.calculatePublicationFromFixing(currentFixing);
      if (env.getValuationDate().equals(currentPublication) &&
          !(currentFixing.isAfter(lastFixingNonCutoff))) { // If currentFixing > lastFixingNonCutoff, everything fixed
        OptionalDouble fixedRate = indexFixingDateSeries.get(currentFixing);
        if (fixedRate.isPresent()) {
          nextFixing = fixingCalendar.next(nextFixing);
          LocalDate effectiveDate = index.calculateEffectiveFromFixing(currentFixing);
          LocalDate maturityDate = index.calculateMaturityFromEffective(effectiveDate);
          double accrualFactor = index.getDayCount().yearFraction(effectiveDate, maturityDate);
          if (currentFixing.isBefore(lastFixingNonCutoff)) {
            return 1.0d + accrualFactor * fixedRate.getAsDouble();
          }
          double compositionFactor = 1.0d + accrualFactor * fixedRate.getAsDouble();
          for (int i = 0; i < cutoffOffset - 1; i++) {
            compositionFactor *= 1.0d + accrualFactorCutoff[i] * fixedRate.getAsDouble();
          }
          return compositionFactor;
        }
      }
      return 1.0d;
    }

    // Composition - forward part in non-cutoff period; past/valuation date case dealt with in previous methods
    private double compositionFactorNonCutoff() {
      if (nextFixing.isBefore(lastFixingNonCutoff)) {
        LocalDate startDate = index.calculateEffectiveFromFixing(nextFixing);
        LocalDate endDate = index.calculateMaturityFromEffective(index.calculateEffectiveFromFixing(lastFixingNonCutoff));
        double accrualFactor = index.getDayCount().yearFraction(startDate, endDate);
        double rate = env.overnightIndexRatePeriod(index, startDate, endDate);
        return 1.0d + accrualFactor * rate;
      }
      return 1.0d;
    }

    // Composition - forward part in the cutoff period; past/valuation date case dealt with in previous methods
    private double compositionFactorCutoff() {
      if (nextFixing.isBefore(lastFixingNonCutoff)) {
        double rate = env.overnightIndexRate(index, lastFixingNonCutoff);
        double compositionFactor = 1.0d;
        for (int i = 0; i < cutoffOffset - 1; i++) {
          compositionFactor *= 1.0d + accrualFactorCutoff[i] * rate;
        }
        return compositionFactor;
      }
      return 1.0d;
    }

    // Calculate the total rate
    private double calculateRate() {
      return (pastCompositionFactor() * valuationCompositionFactor() *
          compositionFactorNonCutoff() * compositionFactorCutoff() - 1.0d) / accrualFactorTotal;
    }

    // Check that the fixing is present. Throws an exception if not and return the rate as double.
    private static double checkedFixing(
        LocalDate currentFixingTs,
        LocalDateDoubleTimeSeries indexFixingDateSeries,
        OvernightIndex index) {

      OptionalDouble fixedRate = indexFixingDateSeries.get(currentFixingTs);
      return fixedRate.orElseThrow(() ->
          new PricingException("Could not get fixing value of index " + index.getName() +
              " for date " + currentFixingTs));
    }
  }

}
