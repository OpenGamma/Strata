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

import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.PricingException;
import com.opengamma.strata.pricer.rate.OvernightIndexRates;
import com.opengamma.strata.pricer.rate.RateComputationFn;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.OvernightAveragedRateComputation;

/**
* Rate computation implementation for a rate based on a single overnight index that is arithmetically averaged.
* <p>
* The rate already fixed are retrieved from the time series of the {@link RatesProvider}.
* The rate in the future and not in the cut-off period are computed by approximation.
* The rate in the cut-off period (already fixed or forward) are added.
* <p>
* Reference: Overnight Indexes related products, OpenGamma documentation 29, version 1.1, March 2013.
*/
public class ApproxForwardOvernightAveragedRateComputationFn
    implements RateComputationFn<OvernightAveragedRateComputation> {

  /**
   * Default implementation.
   */
  public static final ApproxForwardOvernightAveragedRateComputationFn DEFAULT =
      new ApproxForwardOvernightAveragedRateComputationFn();

  /**
   * Creates an instance.
   */
  public ApproxForwardOvernightAveragedRateComputationFn() {
  }

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      OvernightAveragedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndexRates rates = provider.overnightIndexRates(computation.getIndex());
    LocalDate valuationDate = rates.getValuationDate();
    LocalDate startFixingDate = computation.getStartDate();
    LocalDate startPublicationDate = computation.calculatePublicationFromFixing(startFixingDate);
    // No fixing to analyze. Go directly to approximation and cut-off.
    if (valuationDate.isBefore(startPublicationDate)) {
      return rateForward(computation, rates);
    }
    ObservationDetails details = new ObservationDetails(computation, rates);
    return details.calculateRate();
  }

  @Override
  public PointSensitivityBuilder rateSensitivity(
      OvernightAveragedRateComputation computation,
      LocalDate startDate,
      LocalDate endDate,
      RatesProvider provider) {

    OvernightIndexRates rates = provider.overnightIndexRates(computation.getIndex());
    LocalDate valuationDate = rates.getValuationDate();
    LocalDate startFixingDate = computation.getStartDate();
    LocalDate startPublicationDate = computation.calculatePublicationFromFixing(startFixingDate);
    // No fixing to analyze. Go directly to approximation and cut-off.
    if (valuationDate.isBefore(startPublicationDate)) {
      return rateForwardSensitivity(computation, rates);
    }
    ObservationDetails details = new ObservationDetails(computation, rates);
    return details.calculateRateSensitivity();
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

  //-------------------------------------------------------------------------
  // Compute the approximated rate in the case where the whole period is forward.
  // There is no need to compute overnight periods, except for the cut-off period.
  private double rateForward(OvernightAveragedRateComputation computation, OvernightIndexRates rates) {
    OvernightIndex index = computation.getIndex();
    HolidayCalendar calendar = computation.getFixingCalendar();
    LocalDate startFixingDate = computation.getStartDate();
    LocalDate endFixingDateP1 = computation.getEndDate();
    LocalDate endFixingDate = calendar.previous(endFixingDateP1);
    LocalDate onRateEndDate = computation.calculateMaturityFromFixing(endFixingDate);
    LocalDate onRateStartDate = computation.calculateEffectiveFromFixing(startFixingDate);
    LocalDate onRateNoCutOffEndDate = onRateEndDate;
    int cutoffOffset = computation.getRateCutOffDays() > 1 ? computation.getRateCutOffDays() : 1;
    double accumulatedInterest = 0.0d;
    double accrualFactorTotal = index.getDayCount().yearFraction(onRateStartDate, onRateEndDate);
    if (cutoffOffset > 1) { // Cut-off period
      LocalDate currentFixingDate = endFixingDate;
      OvernightIndexObservation lastIndexObs = null;
      double cutOffAccrualFactorTotal = 0d;
      for (int i = 1; i < cutoffOffset; i++) {
        currentFixingDate = calendar.previous(currentFixingDate);
        lastIndexObs = computation.observeOn(currentFixingDate);
        onRateNoCutOffEndDate = lastIndexObs.getMaturityDate();
        cutOffAccrualFactorTotal += lastIndexObs.getYearFraction();
      }
      double forwardRateCutOff = rates.rate(lastIndexObs);
      accumulatedInterest += cutOffAccrualFactorTotal * forwardRateCutOff;
    }
    // Approximated part
    accumulatedInterest += approximatedInterest(computation.observeOn(onRateStartDate), onRateNoCutOffEndDate, rates);
    // final rate
    return accumulatedInterest / accrualFactorTotal;
  }

  private PointSensitivityBuilder rateForwardSensitivity(
      OvernightAveragedRateComputation computation,
      OvernightIndexRates rates) {

    OvernightIndex index = computation.getIndex();
    HolidayCalendar calendar = computation.getFixingCalendar();
    LocalDate startFixingDate = computation.getStartDate();
    LocalDate endFixingDateP1 = computation.getEndDate();
    LocalDate endFixingDate = calendar.previous(endFixingDateP1);
    LocalDate onRateEndDate = computation.calculateMaturityFromFixing(endFixingDate);
    LocalDate onRateStartDate = computation.calculateEffectiveFromFixing(startFixingDate);
    LocalDate lastNonCutOffMatDate = onRateEndDate;
    int cutoffOffset = computation.getRateCutOffDays() > 1 ? computation.getRateCutOffDays() : 1;
    PointSensitivityBuilder combinedPointSensitivityBuilder = PointSensitivityBuilder.none();
    double accrualFactorTotal = index.getDayCount().yearFraction(onRateStartDate, onRateEndDate);
    if (cutoffOffset > 1) { // Cut-off period
      List<Double> noCutOffAccrualFactorList = new ArrayList<>();
      LocalDate currentFixingDate = endFixingDateP1;
      LocalDate cutOffEffectiveDate;
      for (int i = 0; i < cutoffOffset; i++) {
        currentFixingDate = calendar.previous(currentFixingDate);
        cutOffEffectiveDate = computation.calculateEffectiveFromFixing(currentFixingDate);
        lastNonCutOffMatDate = computation.calculateMaturityFromEffective(cutOffEffectiveDate);
        double accrualFactor = index.getDayCount().yearFraction(cutOffEffectiveDate, lastNonCutOffMatDate);
        noCutOffAccrualFactorList.add(accrualFactor);
      }
      OvernightIndexObservation lastIndexObs = computation.observeOn(currentFixingDate);
      PointSensitivityBuilder forwardRateCutOffSensitivity = rates.ratePointSensitivity(lastIndexObs);
      double totalAccrualFactor = 0.0;
      for (int i = 0; i < cutoffOffset - 1; i++) {
        totalAccrualFactor += noCutOffAccrualFactorList.get(i);
      }
      forwardRateCutOffSensitivity = forwardRateCutOffSensitivity.multipliedBy(totalAccrualFactor);
      combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.combinedWith(forwardRateCutOffSensitivity);
    }
    // Approximated part
    OvernightIndexObservation indexObs = computation.observeOn(onRateStartDate);
    PointSensitivityBuilder approximatedInterestAndSensitivity =
        approximatedInterestSensitivity(indexObs, lastNonCutOffMatDate, rates);
    combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.combinedWith(approximatedInterestAndSensitivity);
    combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.multipliedBy(1.0 / accrualFactorTotal);
    // final rate
    return combinedPointSensitivityBuilder;
  }

  // Compute the accrued interest on a given period by approximation
  private static double approximatedInterest(
      OvernightIndexObservation observation,
      LocalDate endDate,
      OvernightIndexRates rates) {

    DayCount dayCount = observation.getIndex().getDayCount();
    double remainingFixingAccrualFactor = dayCount.yearFraction(observation.getEffectiveDate(), endDate);
    double forwardRate = rates.periodRate(observation, endDate);
    return Math.log(1.0 + forwardRate * remainingFixingAccrualFactor);
  }

  // Compute the accrued interest sensitivity on a given period by approximation
  private static PointSensitivityBuilder approximatedInterestSensitivity(
      OvernightIndexObservation observation,
      LocalDate endDate,
      OvernightIndexRates rates) {

    DayCount dayCount = observation.getIndex().getDayCount();
    double remainingFixingAccrualFactor = dayCount.yearFraction(observation.getEffectiveDate(), endDate);
    double forwardRate = rates.periodRate(observation, endDate);
    PointSensitivityBuilder forwardRateSensitivity = rates.periodRatePointSensitivity(observation, endDate);
    double rateExp = 1.0 + forwardRate * remainingFixingAccrualFactor;
    forwardRateSensitivity = forwardRateSensitivity.multipliedBy(remainingFixingAccrualFactor / rateExp);
    return forwardRateSensitivity;
  }

  //-------------------------------------------------------------------------
  // Internal class representing all the details related to the computation
  private static final class ObservationDetails {
    // The list below are created in the constructor and never modified after.
    private final OvernightIndexRates rates;
    private final List<OvernightIndexObservation> observations;  // one observation per fixing date
    private int fixedPeriod; // Note this is mutable 
    private final double accrualFactorTotal;
    private final int nbPeriods;
    private final OvernightIndex index;
    private final int cutoffOffset;

    // Construct all the details related to the observation: fixing dates, publication dates, start and end dates, 
    // accrual factors, number of already fixed ON rates.
    private ObservationDetails(OvernightAveragedRateComputation computation, OvernightIndexRates rates) {
      this.index = computation.getIndex();
      this.rates = rates;
      LocalDate startFixingDate = computation.getStartDate();
      LocalDate endFixingDateP1 = computation.getEndDate();
      this.cutoffOffset = computation.getRateCutOffDays() > 1 ? computation.getRateCutOffDays() : 1;
      double accrualFactorAccumulated = 0d;
      // find all observations in the period
      LocalDate currentFixing = startFixingDate;
      List<OvernightIndexObservation> indexObsList = new ArrayList<>();
      while (currentFixing.isBefore(endFixingDateP1)) {
        OvernightIndexObservation indexObs = computation.observeOn(currentFixing);
        indexObsList.add(indexObs);
        currentFixing = computation.getFixingCalendar().next(currentFixing);
        accrualFactorAccumulated += indexObs.getYearFraction();
      }
      this.accrualFactorTotal = accrualFactorAccumulated;
      this.nbPeriods = indexObsList.size();
      // dealing with cut-off by replacing observations with ones where fixing/publication locked
      // within cut-off, the effective/maturity dates of each observation have to stay the same
      for (int i = 0; i < cutoffOffset - 1; i++) {
        OvernightIndexObservation fixingIndexObs = indexObsList.get(nbPeriods - cutoffOffset);
        OvernightIndexObservation cutoffIndexObs = indexObsList.get(nbPeriods - 1 - i);
        OvernightIndexObservation updatedIndexObs = cutoffIndexObs.toBuilder()
            .fixingDate(fixingIndexObs.getFixingDate())
            .publicationDate(fixingIndexObs.getPublicationDate())
            .build();
        indexObsList.set(nbPeriods - 1 - i, updatedIndexObs);
      }
      this.observations = Collections.unmodifiableList(indexObsList);
    }

    // Accumulated rate - publication strictly before valuation date: try accessing fixing time-series.
    // fixedPeriod is altered by this method.
    private double pastAccumulation() {
      double accumulatedInterest = 0.0d;
      LocalDateDoubleTimeSeries indexFixingDateSeries = rates.getFixings();
      while ((fixedPeriod < nbPeriods) &&
          rates.getValuationDate().isAfter(observations.get(fixedPeriod).getPublicationDate())) {
        OvernightIndexObservation obs = observations.get(fixedPeriod);
        accumulatedInterest += obs.getYearFraction() *
            checkedFixing(obs.getFixingDate(), indexFixingDateSeries, index);
        fixedPeriod++;
      }
      return accumulatedInterest;
    }

    // Accumulated rate - publication on valuation: Check if a fixing is available on current date.
    // fixedPeriod is altered by this method.
    private double valuationDateAccumulation() {
      double accumulatedInterest = 0.0d;
      LocalDateDoubleTimeSeries indexFixingDateSeries = rates.getFixings();
      boolean ratePresent = true;
      while (ratePresent && fixedPeriod < nbPeriods &&
          rates.getValuationDate().isEqual(observations.get(fixedPeriod).getPublicationDate())) {
        OvernightIndexObservation obs = observations.get(fixedPeriod);
        OptionalDouble fixedRate = indexFixingDateSeries.get(obs.getFixingDate());
        if (fixedRate.isPresent()) {
          accumulatedInterest += obs.getYearFraction() * fixedRate.getAsDouble();
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
        LocalDate endDateApprox = observations.get(nbPeriodNotCutOff - 1).getMaturityDate();
        return approximatedInterest(observations.get(fixedPeriod), endDateApprox, rates);
      }
      return 0.0d;
    }

    //  Accumulated rate sensitivity - approximated forward rates if not all fixed and not part of cutoff
    private PointSensitivityBuilder approximatedForwardAccumulationSensitivity() {
      int nbPeriodNotCutOff = nbPeriods - cutoffOffset + 1;
      if (fixedPeriod < nbPeriodNotCutOff) {
        LocalDate endDateApprox = observations.get(nbPeriodNotCutOff - 1).getMaturityDate();
        return approximatedInterestSensitivity(observations.get(fixedPeriod), endDateApprox, rates);
      }
      return PointSensitivityBuilder.none();
    }

    // Accumulated rate - cutoff part if not fixed
    private double cutOffAccumulation() {
      double accumulatedInterest = 0.0d;
      int nbPeriodNotCutOff = nbPeriods - cutoffOffset + 1;
      for (int i = Math.max(fixedPeriod, nbPeriodNotCutOff); i < nbPeriods; i++) {
        OvernightIndexObservation obs = observations.get(i);
        double forwardRate = rates.rate(obs);
        accumulatedInterest += obs.getYearFraction() * forwardRate;
      }
      return accumulatedInterest;
    }

    // Accumulated rate sensitivity - cutoff part if not fixed
    private PointSensitivityBuilder cutOffAccumulationSensitivity() {
      PointSensitivityBuilder combinedPointSensitivityBuilder = PointSensitivityBuilder.none();
      int nbPeriodNotCutOff = nbPeriods - cutoffOffset + 1;
      for (int i = Math.max(fixedPeriod, nbPeriodNotCutOff); i < nbPeriods; i++) {
        OvernightIndexObservation obs = observations.get(i);
        PointSensitivityBuilder forwardRateSensitivity = rates.ratePointSensitivity(obs)
            .multipliedBy(obs.getYearFraction());
        combinedPointSensitivityBuilder = combinedPointSensitivityBuilder.combinedWith(forwardRateSensitivity);
      }
      return combinedPointSensitivityBuilder;
    }

    // Calculate the total rate.
    private double calculateRate() {
      return (pastAccumulation() + valuationDateAccumulation() +
          approximatedForwardAccumulation() + cutOffAccumulation()) / accrualFactorTotal;
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
      return fixedRate.orElseThrow(() -> new PricingException(
          "Could not get fixing value of index " + index.getName() + " for date " + currentFixingTs));
    }
  }

}
