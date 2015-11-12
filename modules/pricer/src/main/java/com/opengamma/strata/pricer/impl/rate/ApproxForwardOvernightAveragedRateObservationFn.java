/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.rate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;

import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.value.OvernightIndexRates;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.rate.RateObservationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.OvernightAveragedRateObservation;

/**
* Rate observation implementation for a rate based on a single overnight index that is arithmetically averaged.
* <p>
* The rate already fixed are retrieved from the time series of the {@link RatesProvider}.
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

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      OvernightAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndex index = observation.getIndex();
    OvernightIndexRates rates = provider.overnightIndexRates(index);
    LocalDate valuationDate = rates.getValuationDate();
    LocalDate startFixingDate = observation.getStartDate();
    LocalDate startPublicationDate = index.calculatePublicationFromFixing(startFixingDate);
    // No fixing to analyze. Go directly to approximation and cut-off.
    if (valuationDate.isBefore(startPublicationDate)) {
      return rateForward(observation, rates);
    }
    ObservationDetails details = new ObservationDetails(observation, rates);
    return details.calculateRate();
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      OvernightAveragedRateObservation observation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndex index = observation.getIndex();
    OvernightIndexRates rates = provider.overnightIndexRates(index);
    LocalDate valuationDate = rates.getValuationDate();
    LocalDate startFixingDate = observation.getStartDate();
    LocalDate startPublicationDate = index.calculatePublicationFromFixing(startFixingDate);
    // No fixing to analyze. Go directly to approximation and cut-off.
    if (valuationDate.isBefore(startPublicationDate)) {
      return rateForwardSensitivity(observation, rates);
    }
    ObservationDetails details = new ObservationDetails(observation, rates);
    return details.calculateRateSensitivity();
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

  //-------------------------------------------------------------------------
  // Compute the approximated rate in the case where the whole period is forward. 
  // There is no need to compute overnight periods, except for the cut-off period.
  private double rateForward(OvernightAveragedRateObservation observation, OvernightIndexRates rates) {
    OvernightIndex index = observation.getIndex();
    HolidayCalendar calendar = index.getFixingCalendar();
    LocalDate startFixingDate = observation.getStartDate();
    LocalDate endFixingDateP1 = observation.getEndDate();
    LocalDate endFixingDate = calendar.previous(endFixingDateP1);
    LocalDate onRateEndDate = index.calculateMaturityFromEffective(index.calculateEffectiveFromFixing(endFixingDate));
    LocalDate onRateStartDate = index.calculateEffectiveFromFixing(startFixingDate);
    LocalDate onRateNoCutOffEndDate = onRateEndDate;
    int cutoffOffset = observation.getRateCutOffDays() > 1 ? observation.getRateCutOffDays() : 1;
    double accumulatedInterest = 0.0d;
    double accrualFactorTotal = index.getDayCount().yearFraction(onRateStartDate, onRateEndDate);
    if (cutoffOffset > 1) { // Cut-off period
      List<Double> noCutOffAccrualFactorList = new ArrayList<>();
      LocalDate currentFixing = endFixingDateP1;
      LocalDate cutOffEffectiveDate;
      for (int i = 0; i < cutoffOffset; i++) {
        currentFixing = calendar.previous(currentFixing);
        cutOffEffectiveDate = index.calculateEffectiveFromFixing(currentFixing);
        onRateNoCutOffEndDate = index.calculateMaturityFromEffective(cutOffEffectiveDate);
        double accrualFactor = index.getDayCount().yearFraction(cutOffEffectiveDate, onRateNoCutOffEndDate);
        noCutOffAccrualFactorList.add(accrualFactor);
      }
      double forwardRateCutOff = rates.rate(currentFixing);
      for (int i = 0; i < cutoffOffset - 1; i++) {
        accumulatedInterest += noCutOffAccrualFactorList.get(i) * forwardRateCutOff;
      }
    }
    // Approximated part
    accumulatedInterest += approximatedInterest(index, onRateStartDate, onRateNoCutOffEndDate, rates);
    // final rate
    return accumulatedInterest / accrualFactorTotal;
  }

  private PointSensitivityBuilder rateForwardSensitivity(
      OvernightAveragedRateObservation observation,
      OvernightIndexRates rates) {

    OvernightIndex index = observation.getIndex();
    HolidayCalendar calendar = index.getFixingCalendar();
    LocalDate startFixingDate = observation.getStartDate();
    LocalDate endFixingDateP1 = observation.getEndDate();
    LocalDate endFixingDate = calendar.previous(endFixingDateP1);
    LocalDate onRateEndDate = index.calculateMaturityFromEffective(index.calculateEffectiveFromFixing(endFixingDate));
    LocalDate onRateStartDate = index.calculateEffectiveFromFixing(startFixingDate);
    LocalDate onRateNoCutOffEndDate = onRateEndDate;
    int cutoffOffset = observation.getRateCutOffDays() > 1 ? observation.getRateCutOffDays() : 1;
    PointSensitivityBuilder combinedPointSensitivityBuilder = PointSensitivityBuilder.none();
    double accrualFactorTotal = index.getDayCount().yearFraction(onRateStartDate, onRateEndDate);
    if (cutoffOffset > 1) { // Cut-off period
      List<Double> noCutOffAccrualFactorList = new ArrayList<>();
      LocalDate currentFixing = endFixingDateP1;
      LocalDate cutOffEffectiveDate;
      for (int i = 0; i < cutoffOffset; i++) {
        currentFixing = calendar.previous(currentFixing);
        cutOffEffectiveDate = index.calculateEffectiveFromFixing(currentFixing);
        onRateNoCutOffEndDate = index.calculateMaturityFromEffective(cutOffEffectiveDate);
        double accrualFactor = index.getDayCount().yearFraction(cutOffEffectiveDate, onRateNoCutOffEndDate);
        noCutOffAccrualFactorList.add(accrualFactor);
      }
      PointSensitivityBuilder forwardRateCutOffSensitivity = rates.ratePointSensitivity(currentFixing);
      double totalAccrualFactor = 0.0;
      for (int i = 0; i < cutoffOffset - 1; i++) {
        totalAccrualFactor += noCutOffAccrualFactorList.get(i);
      }
      forwardRateCutOffSensitivity = forwardRateCutOffSensitivity.multipliedBy(totalAccrualFactor);
      combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.combinedWith(forwardRateCutOffSensitivity);
    }
    // Approximated part
    PointSensitivityBuilder approximatedInterestAndSensitivity =
        approximatedInterestSensitivity(onRateStartDate, onRateNoCutOffEndDate, rates);
    combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.combinedWith(approximatedInterestAndSensitivity);
    combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.multipliedBy(1.0 / accrualFactorTotal);
    // final rate
    return combinedPointSensitivityBuilder;
  }

  // Compute the accrued interest on a given period by approximation
  private static double approximatedInterest(
      OvernightIndex index,
      LocalDate startDate,
      LocalDate endDate,
      OvernightIndexRates rates) {

    double remainingFixingAccrualFactor = index.getDayCount().yearFraction(startDate, endDate);
    double forwardRate = rates.periodRate(startDate, endDate);
    return Math.log(1.0 + forwardRate * remainingFixingAccrualFactor);
  }

  // Compute the accrued interest sensitivity on a given period by approximation
  private static PointSensitivityBuilder approximatedInterestSensitivity(
      LocalDate startDate,
      LocalDate endDate,
      OvernightIndexRates rates) {

    double remainingFixingAccrualFactor = rates.getIndex().getDayCount().yearFraction(startDate, endDate);
    double forwardRate = rates.periodRate(startDate, endDate);
    PointSensitivityBuilder forwardRateSensitivity = rates.periodRatePointSensitivity(startDate, endDate);
    double rateExp = 1.0 + forwardRate * remainingFixingAccrualFactor;
    forwardRateSensitivity = forwardRateSensitivity.multipliedBy(remainingFixingAccrualFactor / rateExp);
    return forwardRateSensitivity;
  }

  //-------------------------------------------------------------------------
  // Internal class representing all the details related to the observation
  private static class ObservationDetails {
    // The list below are created in the constructor and never modified after.
    private final OvernightIndexRates rates;
    private final List<LocalDate> fixingDates; // Dates on which the fixing take place
    private final List<LocalDate> onRatePeriodEffectiveDates; // Dates on which the fixing take place
    private final List<LocalDate> onRatePeriodMaturityDates; // Dates on which the fixing take place
    private final List<LocalDate> publicationDates; // Dates on which the fixing is published
    private final List<Double> accrualFactors; // AF related to the accrual period
    private int fixedPeriod = 0; // Note this is mutable 
    private final double accrualFactorTotal;
    private final int nbPeriods;
    private final OvernightIndex index;
    private final int cutoffOffset;

    // Construct all the details related to the observation: fixing dates, publication dates, start and end dates, 
    // accrual factors, number of already fixed ON rates.
    private ObservationDetails(OvernightAveragedRateObservation observation, OvernightIndexRates rates) {
      this.index = observation.getIndex();
      this.rates = rates;
      ArrayList<LocalDate> fixingDatesCstr = new ArrayList<>();
      List<LocalDate> onRatePeriodEffectiveDatesCstr = new ArrayList<>();
      List<LocalDate> onRatePeriodMaturityDatesCstr = new ArrayList<>();
      List<LocalDate> publicationDatesCstr = new ArrayList<>();
      List<Double> accrualFactorsCstr = new ArrayList<>();
      LocalDate startFixingDate = observation.getStartDate();
      LocalDate endFixingDateP1 = observation.getEndDate();
      LocalDate currentFixing = startFixingDate;
      int publicationOffset = index.getPublicationDateOffset(); // Publication offset is 0 or 1 day.
      this.cutoffOffset = observation.getRateCutOffDays() > 1 ? observation.getRateCutOffDays() : 1;
      double accrualFactorAccumulated = 0.0d;
      while (currentFixing.isBefore(endFixingDateP1)) {
        // All dates involved in the period are computed. Potentially slow.
        // The fixing periods are added as long as their start date is (strictly) before the fixing period end-date. 
        // When the fixing period end-date is not a good business day in the index calendar, 
        // the last fixing end date will be after the fixing end-date.
        LocalDate currentOnRateEffective = index.calculateEffectiveFromFixing(currentFixing);
        onRatePeriodEffectiveDatesCstr.add(currentOnRateEffective);
        LocalDate currentOnRateMaturity = index.calculateMaturityFromEffective(currentOnRateEffective);
        onRatePeriodMaturityDatesCstr.add(currentOnRateMaturity);
        fixingDatesCstr.add(currentFixing);
        publicationDatesCstr.add(publicationOffset == 0 ? currentFixing : index.getFixingCalendar().next(currentFixing));
        double accrualFactor = index.getDayCount().yearFraction(currentOnRateEffective, currentOnRateMaturity);
        accrualFactorsCstr.add(accrualFactor);
        currentFixing = index.getFixingCalendar().next(currentFixing);
        accrualFactorAccumulated += accrualFactor;
      }
      this.accrualFactorTotal = accrualFactorAccumulated;
      this.nbPeriods = accrualFactorsCstr.size();
      // Dealing with Rate cutoff: replace the duplicated periods.
      for (int i = 0; i < cutoffOffset - 1; i++) {
        fixingDatesCstr.set(nbPeriods - 1 - i, fixingDatesCstr.get(nbPeriods - cutoffOffset));
        publicationDatesCstr.set(nbPeriods - 1 - i, publicationDatesCstr.get(nbPeriods - cutoffOffset));
      }
      this.fixingDates = Collections.unmodifiableList(fixingDatesCstr);
      this.onRatePeriodEffectiveDates = Collections.unmodifiableList(onRatePeriodEffectiveDatesCstr);
      this.onRatePeriodMaturityDates = Collections.unmodifiableList(onRatePeriodMaturityDatesCstr);
      this.publicationDates = Collections.unmodifiableList(publicationDatesCstr);
      this.accrualFactors = Collections.unmodifiableList(accrualFactorsCstr);
    }

    // Accumulated rate - publication strictly before valuation date: try accessing fixing time-series. 
    // fixedPeriod is altered by this method.
    private double pastAccumulation() {
      double accumulatedInterest = 0.0d;
      LocalDateDoubleTimeSeries indexFixingDateSeries = rates.getTimeSeries();
      while ((fixedPeriod < nbPeriods) &&
          rates.getValuationDate().isAfter(publicationDates.get(fixedPeriod))) {
        accumulatedInterest += accrualFactors.get(fixedPeriod) *
            checkedFixing(fixingDates.get(fixedPeriod), indexFixingDateSeries, index);
        fixedPeriod++;
      }
      return accumulatedInterest;
    }

    // Accumulated rate - publication on valuation: Check if a fixing is available on current date. 
    // fixedPeriod is altered by this method.
    private double valuationDateAccumulation() {
      double accumulatedInterest = 0.0d;
      LocalDateDoubleTimeSeries indexFixingDateSeries = rates.getTimeSeries();
      boolean ratePresent = true;
      while (ratePresent && fixedPeriod < nbPeriods &&
          rates.getValuationDate().isEqual(publicationDates.get(fixedPeriod))) {
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
    private double approximatedForwardAccumulation() {
      int nbPeriodNotCutOff = nbPeriods - cutoffOffset + 1;
      if (fixedPeriod < nbPeriodNotCutOff) {
        LocalDate startDateApprox = onRatePeriodEffectiveDates.get(fixedPeriod);
        LocalDate endDateApprox = onRatePeriodMaturityDates.get(nbPeriodNotCutOff - 1);
        return approximatedInterest(index, startDateApprox, endDateApprox, rates);
      }
      return 0.0d;
    }

    //  Accumulated rate sensitivity - approximated forward rates if not all fixed and not part of cutoff
    private PointSensitivityBuilder approximatedForwardAccumulationSensitivity() {
      int nbPeriodNotCutOff = nbPeriods - cutoffOffset + 1;
      if (fixedPeriod < nbPeriodNotCutOff) {
        LocalDate startDateApprox = onRatePeriodEffectiveDates.get(fixedPeriod);
        LocalDate endDateApprox = onRatePeriodMaturityDates.get(nbPeriodNotCutOff - 1);
        return approximatedInterestSensitivity(startDateApprox, endDateApprox, rates);
      }
      return PointSensitivityBuilder.none();
    }

    // Accumulated rate - cutoff part if not fixed
    private double cutOffAccumulation() {
      double accumulatedInterest = 0.0d;
      int nbPeriodNotCutOff = nbPeriods - cutoffOffset + 1;
      for (int i = Math.max(fixedPeriod, nbPeriodNotCutOff); i < nbPeriods; i++) {
        double forwardRate = rates.rate(fixingDates.get(i));
        accumulatedInterest += accrualFactors.get(i) * forwardRate;
      }
      return accumulatedInterest;
    }

    // Accumulated rate sensitivity - cutoff part if not fixed
    private PointSensitivityBuilder cutOffAccumulationSensitivity() {
      PointSensitivityBuilder combinedPointSensitivityBuilder = PointSensitivityBuilder.none();
      int nbPeriodNotCutOff = nbPeriods - cutoffOffset + 1;
      for (int i = Math.max(fixedPeriod, nbPeriodNotCutOff); i < nbPeriods; i++) {
        PointSensitivityBuilder forwardRateSensitivity = rates.ratePointSensitivity(fixingDates.get(i));
        forwardRateSensitivity = forwardRateSensitivity.multipliedBy(accrualFactors.get(i));
        combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.combinedWith(forwardRateSensitivity);
      }
      return combinedPointSensitivityBuilder;
    }

    // Calculate the total rate.
    private double calculateRate() {
      return (pastAccumulation() + valuationDateAccumulation()
          + approximatedForwardAccumulation() + cutOffAccumulation())
          / accrualFactorTotal;
    }

    // Calculate the total rate sensitivity.
    private PointSensitivityBuilder calculateRateSensitivity() {
      // call these methods to ensure mutable fixedPeriod variable is updated
      pastAccumulation();
      valuationDateAccumulation();
      // calculate sensitivity
      PointSensitivityBuilder combinedPointSensitivity = approximatedForwardAccumulationSensitivity();
      PointSensitivityBuilder cutOffAccumulationSensitivity = cutOffAccumulationSensitivity();
      combinedPointSensitivity = combinedPointSensitivity.combinedWith(cutOffAccumulationSensitivity);
      combinedPointSensitivity = combinedPointSensitivity.multipliedBy(1.0d / accrualFactorTotal);
      return combinedPointSensitivity;
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
