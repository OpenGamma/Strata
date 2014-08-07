/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.sesame.credit;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.SortedMap;

import org.threeten.bp.Period;

import com.google.common.collect.Iterables;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurve;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDACompliantYieldCurveBuild;
import com.opengamma.analytics.financial.credit.isdastandardmodel.ISDAInstrumentTypes;
import com.opengamma.core.holiday.HolidaySource;
import com.opengamma.core.region.RegionSource;
import com.opengamma.financial.analytics.conversion.CalendarUtils;
import com.opengamma.financial.analytics.isda.credit.YieldCurveData;
import com.opengamma.financial.convention.calendar.Calendar;
import com.opengamma.financial.convention.calendar.MondayToFridayCalendar;
import com.opengamma.id.ExternalId;
import com.opengamma.sesame.Environment;
import com.opengamma.sesame.credit.snapshot.YieldCurveDataProviderFn;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.money.Currency;
import com.opengamma.util.result.Result;
import com.opengamma.util.time.Tenor;

/**
 * Default implementation. Builds an ISDA curve for the passed currency using the {@link YieldCurveDataProviderFn}
 * configured. The returned {@link ISDACompliantYieldCurve} is the yield curve used in ISDA compliant analytic
 * credit computations.
 * 
 * This function expects to be able to query a provider which, given a currency, can provide a {@link YieldCurveData}
 * instance. These are typically sourced from a market data snapshot.
 */
public class DefaultIsdaCompliantYieldCurveFn implements IsdaCompliantYieldCurveFn {

  private final YieldCurveDataProviderFn _yieldCurveProvider;
  
  private final RegionSource _regionSource;
  private final HolidaySource _holidaySource;
  
  /**
   * Builds a new instance.
   * @param yieldCurveProvider a provider which, given a currency, returns a {@link YieldCurveData} instance.
   * @param regionSource used to source holiday calendar data
   * @param holidaySource used to source holiday calendar data
   */
  public DefaultIsdaCompliantYieldCurveFn(YieldCurveDataProviderFn yieldCurveProvider,
                                          RegionSource regionSource,
                                          HolidaySource holidaySource) {
    _yieldCurveProvider = ArgumentChecker.notNull(yieldCurveProvider, "yieldCurveProvider");
    _regionSource = ArgumentChecker.notNull(regionSource, "regionSoruce");
    _holidaySource = ArgumentChecker.notNull(holidaySource, "holidaySource");
  }

  @Override
  public Result<IsdaYieldCurve> buildIsdaCompliantCurve(Environment env, Currency ccy) {
    
    Result<YieldCurveData> yieldCurveData = _yieldCurveProvider.retrieveYieldCurveData(ccy);
    
    if (!yieldCurveData.isSuccess()) {
      return Result.failure(yieldCurveData);
    }
    
    ISDACompliantYieldCurve curve = buildCurve(env, yieldCurveData.getValue());
    
    IsdaYieldCurve yieldCurve = IsdaYieldCurve.builder()
                                                    .calibratedCurve(curve)
                                                    .curveData(yieldCurveData.getValue())
                                                    .build();
    
    return Result.success(yieldCurve);
    
  }

  /**
   * Simply pulls fields off of the {@link YieldCurveData} object and passes into the analytics object.
   */
  private ISDACompliantYieldCurve buildCurve(Environment env, YieldCurveData yieldCurveData) {
    
    SortedMap<Tenor, Double> cashData = yieldCurveData.getCashData();
    SortedMap<Tenor, Double> swapData = yieldCurveData.getSwapData();
    int curveNodesLength = cashData.size() + swapData.size();
    
    Period[] periods = new Period[curveNodesLength];
    double[] rates = new double[curveNodesLength];
    ISDAInstrumentTypes[] instrumentTypes = new ISDAInstrumentTypes[curveNodesLength];
    
    int i = 0;
    
    Arrays.fill(instrumentTypes, 0, cashData.size(), ISDAInstrumentTypes.MoneyMarket);
    Arrays.fill(instrumentTypes, cashData.size(), curveNodesLength, ISDAInstrumentTypes.Swap);
    
    for (Entry<Tenor, Double> entry : Iterables.concat(cashData.entrySet(), swapData.entrySet())) {
      Tenor tenor = entry.getKey();
      Double dataPoint = entry.getValue();
      
      periods[i] = tenor.getPeriod();
      rates[i] = dataPoint;
      
      i++;
    }
    
    ExternalId regionId = yieldCurveData.getRegionId();
    Calendar calendar;
    if (regionId != null) {
      calendar = CalendarUtils.getCalendar(_regionSource, _holidaySource, regionId);
    } else {
      calendar = new MondayToFridayCalendar("weekend_only");
    }
    
    ISDACompliantYieldCurveBuild builder = new ISDACompliantYieldCurveBuild(
            env.getValuationDate(), //can theoretically be after the spot date
            yieldCurveData.getSpotDate(),
            instrumentTypes, 
            periods,
            yieldCurveData.getCashDayCount(),
            yieldCurveData.getSwapDayCount(),
            yieldCurveData.getSwapFixedLegInterval().getPeriod(),
            yieldCurveData.getCurveDayCount(),
            yieldCurveData.getCurveBusinessDayConvention(),
            calendar
    );
    
    return builder.build(rates);
  }

}
