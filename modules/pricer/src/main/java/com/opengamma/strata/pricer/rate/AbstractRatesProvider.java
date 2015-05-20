/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.ForwardSensitivity;
import com.opengamma.analytics.financial.provider.sensitivity.multicurve.SimplyCompoundedForwardSensitivity;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.DoublesPair;
import com.opengamma.strata.market.curve.DiscountIborIndexRates;
import com.opengamma.strata.market.curve.DiscountOvernightIndexRates;
import com.opengamma.strata.market.curve.ZeroRateDiscountFactors;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.IndexCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.SensitivityKey;
import com.opengamma.strata.market.sensitivity.ZeroRateSensitivity;

/**
 * An abstract rates provider implementation.
 * <p>
 * This class exists to provide common functionality between rate provider implementations.
 */
public abstract class AbstractRatesProvider
    implements RatesProvider {

  //-------------------------------------------------------------------------
  @Override
  public CurveParameterSensitivity parameterSensitivity(PointSensitivities sensitivities) {
    Map<SensitivityKey, double[]> map = new HashMap<>();
    paramSensitivityZeroRate(sensitivities, map);
    parameterSensitivityIbor(sensitivities, map);
    parameterSensitivityOvernight(sensitivities, map);
    return CurveParameterSensitivity.of(map);
  }

  // handle zero rate sensitivities
  private void paramSensitivityZeroRate(PointSensitivities sensitivities, Map<SensitivityKey, double[]> mutableMap) {
    // group by currency
    ListMultimap<CurrencyPair, DoublesPair> grouped = ArrayListMultimap.create();
    for (PointSensitivity point : sensitivities.getSensitivities()) {
      if (point instanceof ZeroRateSensitivity) {
        ZeroRateSensitivity pt = (ZeroRateSensitivity) point;
        CurrencyPair pair = CurrencyPair.of(pt.getCurveCurrency(), pt.getCurrency());
        grouped.put(pair, DoublesPair.of(relativeTime(pt.getDate()), pt.getSensitivity()));
      }
    }
    // calculate per currency
    for (CurrencyPair key : grouped.keySet()) {
      YieldAndDiscountCurve curve = ((ZeroRateDiscountFactors) discountFactors(key.getBase())).getCurve();
      double[] sensiParam = parameterSensitivityZeroRate(curve, grouped.get(key));
      NameCurrencySensitivityKey keyParam = NameCurrencySensitivityKey.of(curve.getName(), key.getCounter());
      mutableMap.put(keyParam, sensiParam);
    }
  }

  // sensitivity, copied from MulticurveProviderDiscount
  private double[] parameterSensitivityZeroRate(YieldAndDiscountCurve curve, List<DoublesPair> pointSensitivity) {
    int nbParameters = curve.getNumberOfParameters();
    double[] result = new double[nbParameters];
    for (DoublesPair timeAndS : pointSensitivity) {
      double[] sensi1Point = curve.getInterestRateParameterSensitivity(timeAndS.getFirst());
      for (int i = 0; i < nbParameters; i++) {
        result[i] += timeAndS.getSecond() * sensi1Point[i];
      }
    }
    return result;
  }

  // handle ibor rate sensitivities
  private void parameterSensitivityIbor(PointSensitivities sensitivities, Map<SensitivityKey, double[]> mutableMap) {
    // group by currency
    ListMultimap<IndexCurrencySensitivityKey, ForwardSensitivity> grouped = ArrayListMultimap.create();
    for (PointSensitivity point : sensitivities.getSensitivities()) {
      if (point instanceof IborRateSensitivity) {
        IborRateSensitivity pt = (IborRateSensitivity) point;
        IborIndex index = pt.getIndex();
        LocalDate startDate = index.calculateEffectiveFromFixing(pt.getFixingDate());
        LocalDate endDate = index.calculateMaturityFromEffective(startDate);
        double startTime = relativeTime(startDate);
        double endTime = relativeTime(endDate);
        double accrualFactor = index.getDayCount().yearFraction(startDate, endDate);
        IndexCurrencySensitivityKey key = IndexCurrencySensitivityKey.of(index, pt.getCurrency());
        grouped.put(key, new SimplyCompoundedForwardSensitivity(startTime, endTime, accrualFactor, pt.getSensitivity()));
      }
    }
    // calculate per currency
    for (IndexCurrencySensitivityKey key : grouped.keySet()) {
      DiscountIborIndexRates iborCurve = (DiscountIborIndexRates) iborIndexRates((IborIndex) key.getIndex());
      YieldAndDiscountCurve curve = ((ZeroRateDiscountFactors) iborCurve.getDiscountFactors()).getCurve();
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(curve.getName(), key.getCurrency());
      double[] sensiParam = parameterSensitivityIndex(curve, grouped.get(key));
      mutableMap.merge(keyParam, sensiParam, AbstractRatesProvider::combineArrays);
    }
  }

  // handle overnight rate sensitivities
  private void parameterSensitivityOvernight(PointSensitivities sensitivities, Map<SensitivityKey, double[]> mutableMap) {
    // group by currency
    ListMultimap<IndexCurrencySensitivityKey, ForwardSensitivity> grouped = ArrayListMultimap.create();
    for (PointSensitivity point : sensitivities.getSensitivities()) {
      if (point instanceof OvernightRateSensitivity) {
        OvernightRateSensitivity pt = (OvernightRateSensitivity) point;
        OvernightIndex index = pt.getIndex();
        LocalDate fixingDate = pt.getFixingDate();
        LocalDate endDate = pt.getEndDate();
        LocalDate startDate = index.calculateEffectiveFromFixing(fixingDate);
        double startTime = relativeTime(startDate);
        double endTime = relativeTime(endDate);
        double accrualFactor = index.getDayCount().yearFraction(startDate, endDate);
        IndexCurrencySensitivityKey key = IndexCurrencySensitivityKey.of(index, pt.getCurrency());
        grouped.put(key, new SimplyCompoundedForwardSensitivity(startTime, endTime, accrualFactor, pt.getSensitivity()));
      }
    }
    // calculate per currency
    for (IndexCurrencySensitivityKey key : grouped.keySet()) {
      DiscountOvernightIndexRates iborCurve =
          (DiscountOvernightIndexRates) overnightIndexRates((OvernightIndex) key.getIndex());
      YieldAndDiscountCurve curve = ((ZeroRateDiscountFactors) iborCurve.getDiscountFactors()).getCurve();
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(curve.getName(), key.getCurrency());
      double[] sensiParam = parameterSensitivityIndex(curve, grouped.get(key));
      mutableMap.merge(keyParam, sensiParam, AbstractRatesProvider::combineArrays);
    }
  }

  // sensitivity, copied from MulticurveProviderDiscount
  private double[] parameterSensitivityIndex(YieldAndDiscountCurve curve, List<ForwardSensitivity> pointSensitivity) {
    int nbParameters = curve.getNumberOfParameters();
    double[] result = new double[nbParameters];
    for (ForwardSensitivity timeAndS : pointSensitivity) {
      double startTime = timeAndS.getStartTime();
      double endTime = timeAndS.getEndTime();
      double forwardBar = timeAndS.getValue();
      // Implementation note: only the sensitivity to the forward is available.
      // The sensitivity to the pseudo-discount factors need to be computed.
      double dfForwardStart = curve.getDiscountFactor(startTime);
      double dfForwardEnd = curve.getDiscountFactor(endTime);
      double dFwddyStart = timeAndS.derivativeToYieldStart(dfForwardStart, dfForwardEnd);
      double dFwddyEnd = timeAndS.derivativeToYieldEnd(dfForwardStart, dfForwardEnd);
      double[] sensiPtStart = curve.getInterestRateParameterSensitivity(startTime);
      double[] sensiPtEnd = curve.getInterestRateParameterSensitivity(endTime);
      for (int i = 0; i < nbParameters; i++) {
        result[i] += dFwddyStart * sensiPtStart[i] * forwardBar;
        result[i] += dFwddyEnd * sensiPtEnd[i] * forwardBar;
      }
    }
    return result;
  }

  // add two arrays
  private static double[] combineArrays(double[] a, double[] b) {
    ArgChecker.isTrue(a.length == b.length, "Sensitivity arrays must have same length");
    double[] result = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      result[i] = a[i] + b[i];
    }
    return result;
  }

}
