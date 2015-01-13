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
import com.opengamma.basics.date.BusinessDayConvention;
import com.opengamma.basics.date.BusinessDayConventions;
import com.opengamma.basics.date.HolidayCalendar;
import com.opengamma.basics.index.OvernightIndex;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.platform.finance.observation.OvernightAveragedRateObservation;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.observation.RateObservationFn;

public class ApproximatedForwardOvernightAveragedRateObservationFn 
    implements RateObservationFn<OvernightAveragedRateObservation> {

  /**
   * Default implementation.
   */
  public static final ApproximatedForwardOvernightAveragedRateObservationFn DEFAULT = 
      new ApproximatedForwardOvernightAveragedRateObservationFn();

  /**
   * Creates an instance.
   */
  public ApproximatedForwardOvernightAveragedRateObservationFn() {
  }
  
public static final BusinessDayConvention FOLLOWING = BusinessDayConventions.FOLLOWING;

  @Override
  public double rate(
      PricingEnvironment env, 
      OvernightAveragedRateObservation observation, 
      LocalDate startDate, 
      LocalDate endDate) {
    OvernightIndex index = observation.getIndex();
    HolidayCalendar calendar = index.getFixingCalendar();
    int publicationOffset = index.getPublicationDateOffset(); // Publication offset is 0 or 1 day.
    int cutoffOffset = observation.getRateCutOffDays() > 1 ? observation.getRateCutOffDays() : 1; 
    LocalDate startFixingDate = observation.getStartDate();
    LocalDate startPublicationDate = index.calculatePublicationFromFixing(startFixingDate);
    LocalDate endFixingDateP1 = observation.getEndDate();
    LocalDate endFixingDate = calendar.previous(endFixingDateP1);
    LocalDate valuationDate = env.getValuationDate();
    LocalDate onRateStartDate = index.calculateEffectiveFromFixing(startFixingDate);
    LocalDate onRateEndDate = index.calculateMaturityFromEffective(index.calculateEffectiveFromFixing(endFixingDate));
    LocalDate onRateNoCutOffEndDate = onRateEndDate;
    double accruedUnitNotional = 0.0d;
    if (valuationDate.isBefore(startPublicationDate)) {
      // No fixing to be analyzed. Go directly to approximation and cut-off.
      // Cut-off part.
      double accrualFactorTotal = index.getDayCount().yearFraction(onRateStartDate, onRateEndDate);
      if (cutoffOffset > 1) { // Cut-off period
        final List<Double> noCutOffAccrualFactorList = new ArrayList<>();
        LocalDate currentFixing = endFixingDateP1;
        LocalDate cutOffEffectiveDate;
        LocalDate cutOffMaturityDate = onRateNoCutOffEndDate;
        for (int i = 0; i < cutoffOffset; i++) {
          currentFixing = calendar.previous(currentFixing);
          cutOffEffectiveDate = index.calculateEffectiveFromFixing(currentFixing);
          cutOffMaturityDate = index.calculateMaturityFromEffective(cutOffEffectiveDate);
          double accrualFactor = index.getDayCount().yearFraction(cutOffEffectiveDate, cutOffMaturityDate);
          noCutOffAccrualFactorList.add(accrualFactor);
        }
        onRateNoCutOffEndDate = cutOffMaturityDate;
        double forwardRateCutOff = env.overnightIndexRate(index, currentFixing);
        for (int i = 0; i < cutoffOffset - 1; i++) {
          accruedUnitNotional += noCutOffAccrualFactorList.get(i) * forwardRateCutOff;
        }
      }
      // Approximated part
      double remainingFixingAccrualFactor = index.getDayCount().yearFraction(onRateStartDate, onRateNoCutOffEndDate);
      double forwardRate = env.overnightIndexRate(index, onRateStartDate, onRateNoCutOffEndDate);
      accruedUnitNotional += Math.log(1.0 + forwardRate * remainingFixingAccrualFactor);
      // final rate
      return accruedUnitNotional / accrualFactorTotal;
    }
    List<LocalDate> fixingDates = new ArrayList<>(); // Dates on which the fixing take place
    List<LocalDate> onRatePeriodEffectiveDates = new ArrayList<>(); // Dates on which the fixing take place
    List<LocalDate> onRatePeriodMaturityDates = new ArrayList<>(); // Dates on which the fixing take place
    List<LocalDate> publicationDates = new ArrayList<>(); // Dates on which the fixing is published
    List<Double> accrualFactors = new ArrayList<>(); // AF related to the accrual period
    LocalDate currentFixing = startFixingDate;
    double accrualFactorTotal = 0.0d;
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
    int nbPeriods = accrualFactors.size();
    // Dealing with Rate cutoff: replace the duplicated periods.
    if (cutoffOffset > 1) { // Cut-off period
      for (int i = 0; i < cutoffOffset - 1; i++) {
        fixingDates.set(nbPeriods - 1 - i, fixingDates.get(nbPeriods - cutoffOffset));
        publicationDates.set(nbPeriods - 1 - i, publicationDates.get(nbPeriods - cutoffOffset));
      }

    }
    // Publication strictly before valuation date;  try accessing fixing time-series
    LocalDateDoubleTimeSeries indexFixingDateSeries = env.timeSeries(index);
    int fixedPeriod = 0;
    while ((fixedPeriod < nbPeriods) &&
        valuationDate.isAfter(publicationDates.get(fixedPeriod))) {
      LocalDate currentFixingTs = fixingDates.get(fixedPeriod);
      OptionalDouble fixedRate = indexFixingDateSeries.get(currentFixingTs);
      if (!fixedRate.isPresent()) {
        throw new OpenGammaRuntimeException("Could not get fixing value of index " + index.getName() +
            " for date " + currentFixingTs);
      }
      accruedUnitNotional += accrualFactors.get(fixedPeriod) * fixedRate.getAsDouble();
      fixedPeriod++;
    } 
    // accrue notional for publication on valuation
    boolean ratePresent = true;
    while (ratePresent && fixedPeriod < nbPeriods && valuationDate.isEqual(publicationDates.get(fixedPeriod))) {
      // Check to see if a fixing is available on current date
      OptionalDouble fixedRate = indexFixingDateSeries.get(fixingDates.get(fixedPeriod));
      if (fixedRate.isPresent()) {
        accruedUnitNotional += accrualFactors.get(fixedPeriod) * fixedRate.getAsDouble();
        fixedPeriod++;
      } else {
        ratePresent = false;
      }
    } 
    // forward rates if not all fixed and not part of cut-off
    int nbPeriodNotCutOff = nbPeriods - cutoffOffset + 1;
    if (fixedPeriod < nbPeriodNotCutOff) {
      double remainingFixingAccrualFactor = 0.0d;
      for (int i = fixedPeriod; i < nbPeriodNotCutOff; i++) {
        remainingFixingAccrualFactor += accrualFactors.get(i);
      }
      double forwardRate = env.overnightIndexRate(index, onRatePeriodEffectiveDates.get(fixedPeriod), 
          onRatePeriodMaturityDates.get(nbPeriodNotCutOff - 1));
      accruedUnitNotional += Math.log(1.0 + forwardRate * remainingFixingAccrualFactor);
    }
    // Cut-off part if not fixed
    for (int i = Math.max(fixedPeriod, nbPeriodNotCutOff); i < nbPeriods; i++) {
      double forwardRate = env.overnightIndexRate(index, fixingDates.get(i));
      accruedUnitNotional += accrualFactors.get(i) * forwardRate;
    }
    // final rate
    return accruedUnitNotional / accrualFactorTotal;
  }

}
