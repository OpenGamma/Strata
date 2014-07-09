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
import com.opengamma.financial.analytics.isda.credit.YieldCurveData;
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
  
  /**
   * Builds a new instance.
   * @param yieldCurveProvider a provider which, given a currency, returns a {@link YieldCurveData} instance.
   */
  public DefaultIsdaCompliantYieldCurveFn(YieldCurveDataProviderFn yieldCurveProvider) {
    _yieldCurveProvider = ArgumentChecker.notNull(yieldCurveProvider, "yieldCurveProvider");
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
            yieldCurveData.getCalendar()
    );
    
    return builder.build(rates);
  }

}
