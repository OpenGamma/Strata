/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.observation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.basics.date.HolidayCalendar;
import com.opengamma.basics.index.OvernightIndex;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.platform.finance.observation.OvernightAveragedRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;

/**
* Rate observation implementation for a rate based on a single overnight index that is arithmetically averaged.
* <p>
* The rate already fixed are retrieved from the time series of the {@link PricingEnvironment}.
* The rate in the future and not in the cut-off period are computed by approximation.
* The rate in the cut-off period (already fixed or forward) are added.
* <p>
* Reference: Overnight Indexes related products, OpenGamma documentation 29, version 1.1, March 2013.
*/
public class ApproxForwardOvernightAveragedRateObservationFn
    implements RateObservationFn<OvernightAveragedRateObservation> {

  /**
   * Default implementation.
   */
  public static final ApproxForwardOvernightAveragedRateObservationFn DEFAULT =
      new ApproxForwardOvernightAveragedRateObservationFn();

  /**
   * Creates an instance.
   */
  public ApproxForwardOvernightAveragedRateObservationFn() {
  }

  @Override
  public double rate(
      PricingEnvironment env,
      OvernightAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate) {
    OvernightIndex index = observation.getIndex();
    HolidayCalendar calendar = index.getFixingCalendar();
    LocalDate valuationDate = env.getValuationDate();
    LocalDate startFixingDate = observation.getStartDate();
    LocalDate endFixingDateP1 = observation.getEndDate();
    LocalDate startPublicationDate = index.calculatePublicationFromFixing(startFixingDate);
    LocalDate endFixingDate = calendar.previous(endFixingDateP1);
    LocalDate onRateEndDate = index.calculateMaturityFromEffective(index.calculateEffectiveFromFixing(endFixingDate));
    if (valuationDate.isBefore(startPublicationDate)) {// No fixing to analyze. Go directly to approximation and cut-off.
      LocalDate onRateStartDate = index.calculateEffectiveFromFixing(startFixingDate);
      return rateForward(onRateStartDate, onRateEndDate, env, observation);
    }
    double accumulatedInterest = 0.0d;
    ObservationDetails details = new ObservationDetails(observation);
    accumulatedInterest += details.pastAccumulation(env);
    accumulatedInterest += details.valuationDateAccumulation(env);
    accumulatedInterest += details.approximatedForwardAccumulation(env);
    accumulatedInterest += details.cutOffAccumulation(env);
    // final rate
    return accumulatedInterest / details.getAccrualFactorTotal();
  }

  // Check that the fixing is present. Throws an exception if not and return the rate as double.
  private double checkedFixing(LocalDate currentFixingTs, LocalDateDoubleTimeSeries indexFixingDateSeries,
      OvernightIndex index) {
    OptionalDouble fixedRate = indexFixingDateSeries.get(currentFixingTs);
    if (!fixedRate.isPresent()) {
      throw new OpenGammaRuntimeException("Could not get fixing value of index " + index.getName() +
          " for date " + currentFixingTs);
    }
    return fixedRate.getAsDouble();
  }

  // Compute the approximated rate in the case where the whole period is forward. 
  // There is no need to compute overnight periods, except for the cut-off period.
  private double rateForward(
      LocalDate onRateStartDate,
      LocalDate onRateEndDate,
      PricingEnvironment env,
      OvernightAveragedRateObservation observation) {
    OvernightIndex index = observation.getIndex();
    HolidayCalendar calendar = index.getFixingCalendar();
    LocalDate onRateNoCutOffEndDate = onRateEndDate;
    LocalDate endFixingDateP1 = observation.getEndDate();
    int cutoffOffset = observation.getRateCutOffDays() > 1 ? observation.getRateCutOffDays() : 1;
    double accumulatedInterest = 0.0d;
    double accrualFactorTotal = index.getDayCount().yearFraction(onRateStartDate, onRateEndDate);
    if (cutoffOffset > 1) { // Cut-off period
      final List<Double> noCutOffAccrualFactorList = new ArrayList<>();
      LocalDate currentFixing = endFixingDateP1;
      LocalDate cutOffEffectiveDate;
      for (int i = 0; i < cutoffOffset; i++) {
        currentFixing = calendar.previous(currentFixing);
        cutOffEffectiveDate = index.calculateEffectiveFromFixing(currentFixing);
        onRateNoCutOffEndDate = index.calculateMaturityFromEffective(cutOffEffectiveDate);
        double accrualFactor = index.getDayCount().yearFraction(cutOffEffectiveDate, onRateNoCutOffEndDate);
        noCutOffAccrualFactorList.add(accrualFactor);
      }
      double forwardRateCutOff = env.overnightIndexRate(index, currentFixing);
      for (int i = 0; i < cutoffOffset - 1; i++) {
        accumulatedInterest += noCutOffAccrualFactorList.get(i) * forwardRateCutOff;
      }
    }
    // Approximated part
    accumulatedInterest += approximatedInterest(env, index, onRateStartDate, onRateNoCutOffEndDate);
    // final rate
    return accumulatedInterest / accrualFactorTotal;
  }

  // Compute the accrued interest on a given period by approximation.
  private double approximatedInterest(PricingEnvironment env, OvernightIndex index, LocalDate startDate, LocalDate endDate) {
    double remainingFixingAccrualFactor = index.getDayCount().yearFraction(startDate, endDate);
    double forwardRate = env.overnightIndexRatePeriod(index, startDate, endDate);
    return Math.log(1.0 + forwardRate * remainingFixingAccrualFactor);
  }

  // Internal class representing all the details related to the observation
  class ObservationDetails {
    public final List<LocalDate> fixingDates = new ArrayList<>(); // Dates on which the fixing take place
    public final List<LocalDate> onRatePeriodEffectiveDates = new ArrayList<>(); // Dates on which the fixing take place
    public final List<LocalDate> onRatePeriodMaturityDates = new ArrayList<>(); // Dates on which the fixing take place
    public final List<LocalDate> publicationDates = new ArrayList<>(); // Dates on which the fixing is published
    public final List<Double> accrualFactors = new ArrayList<>(); // AF related to the accrual period
    public int fixedPeriod = 0;
    public double accrualFactorTotal = 0.0d;
    public final int nbPeriods;
    public final OvernightIndex index;
    public int cutoffOffset;

    // Construct all the details related to the observation: fixing dates, publication dates, start and end dates, 
    // accrual factors, number of already fixed ON rates.
    public ObservationDetails(OvernightAveragedRateObservation observation) {
      index = observation.getIndex();
      LocalDate startFixingDate = observation.getStartDate();
      LocalDate endFixingDateP1 = observation.getEndDate();
      LocalDate currentFixing = startFixingDate;
      int publicationOffset = index.getPublicationDateOffset(); // Publication offset is 0 or 1 day.
      cutoffOffset = observation.getRateCutOffDays() > 1 ? observation.getRateCutOffDays() : 1;
      while (currentFixing.isBefore(endFixingDateP1)) { // All dates involved in the period are computed. Potentially slow.
        // The fixing periods are added as long as their start date is (strictly) before the fixing period end-date. 
        // When the fixing period end-date is not a good business day in the index calendar, 
        // the last fixing end date will be after the fixing end-date.
        LocalDate currentOnRateEffective = index.calculateEffectiveFromFixing(currentFixing);
        onRatePeriodEffectiveDates.add(currentOnRateEffective);
        LocalDate currentOnRateMaturity = index.calculateMaturityFromEffective(currentOnRateEffective);
        onRatePeriodMaturityDates.add(currentOnRateMaturity);
        fixingDates.add(currentFixing);
        publicationDates.add(publicationOffset == 0 ? currentFixing : index.getFixingCalendar().next(currentFixing));
        double accrualFactor = index.getDayCount().yearFraction(currentOnRateEffective, currentOnRateMaturity);
        accrualFactors.add(accrualFactor);
        currentFixing = index.getFixingCalendar().next(currentFixing);
        accrualFactorTotal += accrualFactor;
      }
      nbPeriods = accrualFactors.size();
      // Dealing with Rate cutoff: replace the duplicated periods.
      if (cutoffOffset > 1) { // Cut-off period
        for (int i = 0; i < cutoffOffset - 1; i++) {
          fixingDates.set(nbPeriods - 1 - i, fixingDates.get(nbPeriods - cutoffOffset));
          publicationDates.set(nbPeriods - 1 - i, publicationDates.get(nbPeriods - cutoffOffset));
        }
      }
    }

    // Accumulated rate - publication strictly before valuation date: try accessing fixing time-series
    public double pastAccumulation(PricingEnvironment env) {
      double accumulatedInterest = 0.0d;
      LocalDateDoubleTimeSeries indexFixingDateSeries = env.timeSeries(index);
      while ((fixedPeriod < nbPeriods) &&
          env.getValuationDate().isAfter(publicationDates.get(fixedPeriod))) {
        accumulatedInterest += accrualFactors.get(fixedPeriod) *
            checkedFixing(fixingDates.get(fixedPeriod), indexFixingDateSeries, index);
        fixedPeriod++;
      }
      return accumulatedInterest;
    }

    // Accumulated rate - publication on valuation: Check if a fixing is available on current date
    public double valuationDateAccumulation(PricingEnvironment env) {
      double accumulatedInterest = 0.0d;
      LocalDateDoubleTimeSeries indexFixingDateSeries = env.timeSeries(index);
      boolean ratePresent = true;
      while (ratePresent && fixedPeriod < nbPeriods &&
          env.getValuationDate().isEqual(publicationDates.get(fixedPeriod))) {
        OptionalDouble fixedRate = indexFixingDateSeries.get(fixingDates.get(fixedPeriod));
        if (fixedRate.isPresent()) {
          accumulatedInterest += accrualFactors.get(fixedPeriod) * fixedRate.getAsDouble();
          fixedPeriod++;
        } else {
          ratePresent = false;
        }
      }
      return accumulatedInterest;
    }

    //  Accumulated rate - approximated forward rates if not all fixed and not part of cutoff
    public double approximatedForwardAccumulation(PricingEnvironment env) {
      double accumulatedInterest = 0.0d;
      int nbPeriodNotCutOff = nbPeriods - cutoffOffset + 1;
      if (fixedPeriod < nbPeriodNotCutOff) {
        LocalDate startDateApprox = onRatePeriodEffectiveDates.get(fixedPeriod);
        LocalDate endDateApprox = onRatePeriodMaturityDates.get(nbPeriodNotCutOff - 1);
        accumulatedInterest = approximatedInterest(env, index, startDateApprox, endDateApprox);
      }
      return accumulatedInterest;
    }

    // Accumulated rate - cutoff part if not fixed
    public double cutOffAccumulation(PricingEnvironment env) {
      double accumulatedInterest = 0.0d;
      int nbPeriodNotCutOff = nbPeriods - cutoffOffset + 1;
      for (int i = Math.max(fixedPeriod, nbPeriodNotCutOff); i < nbPeriods; i++) {
        double forwardRate = env.overnightIndexRate(index, fixingDates.get(i));
        accumulatedInterest += accrualFactors.get(i) * forwardRate;
      }
      return accumulatedInterest;
    }

    // Returns the total accrual factor for the observation.
    public double getAccrualFactorTotal() {
      return accrualFactorTotal;
    }

  }

}
