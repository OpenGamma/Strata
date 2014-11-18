/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.pricer.impl.rate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

import com.opengamma.OpenGammaRuntimeException;
import com.opengamma.analytics.financial.instrument.index.IndexONMaster;
import com.opengamma.basics.index.OvernightIndex;
import com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.platform.finance.rate.OvernightCompoundedRate;
import com.opengamma.platform.pricer.PricingEnvironment;
import com.opengamma.platform.pricer.rate.RateProviderFn;

/**
 * Rate provider implementation for a rate based on a single overnight index that is compounded.
 * <p>
 * The rate provider examines the historic time-series of known rates and the
 * forward curve to determine the effective annualized rate.
 */
public class StandardOvernightCompoundedRateProviderFn
    implements RateProviderFn<OvernightCompoundedRate> {

  /**
   * Default implementation.
   */
  public static final StandardOvernightCompoundedRateProviderFn DEFAULT = new StandardOvernightCompoundedRateProviderFn();

  //-------------------------------------------------------------------------
  @Override
  public double rate(
      PricingEnvironment env,
      LocalDate valuationDate,
      OvernightCompoundedRate rate,
      LocalDate startDate,
      LocalDate endDate) {
    // the time-series contains the value on the fixing date, not the publication date
    // TODO: is publication date properly handled?
    if (rate.getRateCutoffDaysOffset() < 0) {
      // TODO: rate cutoff
      throw new IllegalArgumentException("Rate cutoff not supported");
    }
    // if no fixings apply, then only use forward rate
    OvernightIndex index = rate.getIndex();
    final LocalDate firstPublicationDate = index.calculatePublicationFromFixing(startDate);
    if (valuationDate.isBefore(firstPublicationDate)) {
      return rateFromForwardCurve(env, valuationDate, index, startDate, endDate);
    }
    // fixing periods, based on business days of the index
    final List<LocalDate> fixingDateList = new ArrayList<>();
    final List<Double> fixingAccrualFactorList = new ArrayList<>();
    LocalDate currentStart = startDate;
    fixingDateList.add(currentStart);
    while (currentStart.isBefore(endDate)) {
      LocalDate currentEnd = index.getFixingCalendar().next(currentStart);
      fixingDateList.add(currentEnd);
      fixingAccrualFactorList.add(index.getDayCount().yearFraction(currentStart, currentEnd));
      currentStart = currentEnd;
    }
    // try accessing fixing time-series
    LocalDateDoubleTimeSeries indexFixingDateSeries = env.getTimeSeries(index);
    int fixedPeriod = 0;
    int publicationLag = index.getPublicationDateOffset();
    double accruedUnitNotional = 1d;
    // accrue notional for fixings before today, up to and including yesterday
    while ((fixedPeriod < fixingDateList.size() - 1) &&
        ((fixedPeriod + publicationLag) < fixingDateList.size()) &&
        valuationDate.isAfter(fixingDateList.get(fixedPeriod + publicationLag))) {
      LocalDate currentDate1 = fixingDateList.get(fixedPeriod);
      OptionalDouble fixedRate = indexFixingDateSeries.get(currentDate1);
      if (!fixedRate.isPresent()) {
        LocalDate latestDate = indexFixingDateSeries.getLatestDate();
        if (currentDate1.isAfter(latestDate)) {
          throw new OpenGammaRuntimeException("Could not get fixing value of index " + index.getName() +
              " for date " + currentDate1 + ". The last data is available on " + latestDate);
        }
        if (!fixedRate.isPresent()) {
          throw new OpenGammaRuntimeException("Could not get fixing value of index " + index.getName() +
              " for date " + currentDate1);
        }
      }
      accruedUnitNotional *= 1 + fixingAccrualFactorList.get(fixedPeriod) * fixedRate.getAsDouble();
      fixedPeriod++;
    }
    // accrue notional for fixings for today
    if (fixedPeriod < fixingDateList.size() - 1) {
      // Check to see if a fixing is available on current date
      OptionalDouble fixedRate = indexFixingDateSeries.get(fixingDateList.get(fixedPeriod));
      if (fixedRate.isPresent()) {
        accruedUnitNotional *= 1 + fixingAccrualFactorList.get(fixedPeriod) * fixedRate.getAsDouble();
        fixedPeriod++;
      }
    }
    // use forward curve for remainder
    double fixingAccrualfactor = index.getDayCount().yearFraction(startDate, endDate);
    if (fixedPeriod < fixingDateList.size() - 1) {
      // fixing period is the remaining time of the period
      final double fixingStart = env.relativeTime(valuationDate, fixingDateList.get(fixedPeriod));
      final double fixingEnd = env.relativeTime(valuationDate, fixingDateList.get(fixingDateList.size() - 1));
      double fixingAccrualFactorLeft = 0.0;
      for (int loopperiod = fixedPeriod; loopperiod < fixingAccrualFactorList.size(); loopperiod++) {
        fixingAccrualFactorLeft += fixingAccrualFactorList.get(loopperiod);
      }
      double observedRate = env.getMulticurve().getSimplyCompoundForwardRate(
          IndexONMaster.getInstance().getIndex("FED FUND"), fixingStart, fixingEnd, fixingAccrualFactorLeft);
      double ratio = 1d + fixingAccrualFactorLeft * observedRate;
      return (accruedUnitNotional * ratio - 1d) / fixingAccrualfactor;
    }
    // all fixed
    return (accruedUnitNotional - 1d) / fixingAccrualfactor;
  }

  // dates entirely in the future
  private double rateFromForwardCurve(
      PricingEnvironment env,
      LocalDate valuationDate,
      OvernightIndex index,
      LocalDate startDate,
      LocalDate endDate) {
    
    double fixingStart = env.relativeTime(valuationDate, startDate);
    double fixingEnd = env.relativeTime(valuationDate, endDate);
    double fixingAccrualfactor = index.getDayCount().yearFraction(startDate, endDate);
    double observedRate = env.getMulticurve().getSimplyCompoundForwardRate(
        IndexONMaster.getInstance().getIndex("FED FUND"), fixingStart, fixingEnd, fixingAccrualfactor);
    return observedRate;
  }

}
