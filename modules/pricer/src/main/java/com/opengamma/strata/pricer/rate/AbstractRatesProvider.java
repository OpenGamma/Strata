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
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.OvernightIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.ObjectDoublePair;
import com.opengamma.strata.market.curve.DiscountFactors;
import com.opengamma.strata.market.curve.DiscountIborIndexRates;
import com.opengamma.strata.market.curve.DiscountOvernightIndexRates;
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
    ListMultimap<CurrencyPair, ObjectDoublePair<LocalDate>> grouped = ArrayListMultimap.create();
    for (PointSensitivity point : sensitivities.getSensitivities()) {
      if (point instanceof ZeroRateSensitivity) {
        ZeroRateSensitivity pt = (ZeroRateSensitivity) point;
        CurrencyPair pair = CurrencyPair.of(pt.getCurveCurrency(), pt.getCurrency());
        grouped.put(pair, ObjectDoublePair.of(pt.getDate(), pt.getSensitivity()));
      }
    }
    // calculate per currency
    for (CurrencyPair key : grouped.keySet()) {
      DiscountFactors factors = discountFactors(key.getBase());
      double[] sensiParam = parameterSensitivityZeroRate(factors, grouped.get(key));
      NameCurrencySensitivityKey keyParam = NameCurrencySensitivityKey.of(factors.getCurveName(), key.getCounter());
      mutableMap.put(keyParam, sensiParam);
    }
  }

  // zero rate sensitivity
  private double[] parameterSensitivityZeroRate(DiscountFactors factors, List<ObjectDoublePair<LocalDate>> grouped) {
    int nbParameters = factors.getParameterCount();
    double[] result = new double[nbParameters];
    for (ObjectDoublePair<LocalDate> dateAndPointSens : grouped) {
      double[] sensi1Point = factors.parameterSensitivity(dateAndPointSens.getFirst());
      for (int i = 0; i < nbParameters; i++) {
        result[i] += dateAndPointSens.getSecond() * sensi1Point[i];
      }
    }
    return result;
  }

  // handle ibor rate sensitivities
  private void parameterSensitivityIbor(PointSensitivities sensitivities, Map<SensitivityKey, double[]> mutableMap) {
    // group by currency
    ListMultimap<IndexCurrencySensitivityKey, IndexSensitivity> grouped = ArrayListMultimap.create();
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
        grouped.put(key, new IndexSensitivity(startDate, startTime, endDate, endTime, accrualFactor, pt.getSensitivity()));
      }
    }
    // calculate per currency
    for (IndexCurrencySensitivityKey key : grouped.keySet()) {
      DiscountIborIndexRates rates = (DiscountIborIndexRates) iborIndexRates((IborIndex) key.getIndex());
      DiscountFactors factors = rates.getDiscountFactors();
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(factors.getCurveName(), key.getCurrency());
      double[] sensiParam = parameterSensitivityIndex(factors, grouped.get(key));
      mutableMap.merge(keyParam, sensiParam, AbstractRatesProvider::combineArrays);
    }
  }

  // handle overnight rate sensitivities
  private void parameterSensitivityOvernight(PointSensitivities sensitivities, Map<SensitivityKey, double[]> mutableMap) {
    // group by currency
    ListMultimap<IndexCurrencySensitivityKey, IndexSensitivity> grouped = ArrayListMultimap.create();
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
        grouped.put(key, new IndexSensitivity(startDate, startTime, endDate, endTime, accrualFactor, pt.getSensitivity()));
      }
    }
    // calculate per currency
    for (IndexCurrencySensitivityKey key : grouped.keySet()) {
      DiscountOvernightIndexRates rates = (DiscountOvernightIndexRates) overnightIndexRates((OvernightIndex) key.getIndex());
      DiscountFactors factors = rates.getDiscountFactors();
      SensitivityKey keyParam = NameCurrencySensitivityKey.of(factors.getCurveName(), key.getCurrency());
      double[] sensiParam = parameterSensitivityIndex(factors, grouped.get(key));
      mutableMap.merge(keyParam, sensiParam, AbstractRatesProvider::combineArrays);
    }
  }

  // sensitivity, copied from MulticurveProviderDiscount
  private double[] parameterSensitivityIndex(DiscountFactors factors, List<IndexSensitivity> grouped) {
    int nbParameters = factors.getParameterCount();
    double[] result = new double[nbParameters];
    for (IndexSensitivity group : grouped) {
      double forwardBar = group.sensitivity;
      // Implementation note: only the sensitivity to the forward is available.
      // The sensitivity to the pseudo-discount factors need to be computed.
      double dfForwardStart = factors.discountFactor(group.startDate);
      double dfForwardEnd = factors.discountFactor(group.endDate);
      double dFwddyStart = group.derivativeToYieldStart(dfForwardStart, dfForwardEnd);
      double dFwddyEnd = group.derivativeToYieldEnd(dfForwardStart, dfForwardEnd);
      double[] sensiPtStart = factors.parameterSensitivity(group.startDate);
      double[] sensiPtEnd = factors.parameterSensitivity(group.endDate);
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

  //-------------------------------------------------------------------------
  // simply compound forward sensitivity
  private static class IndexSensitivity {
    private final LocalDate startDate;
    private final double startTime;
    private final LocalDate endDate;
    private final double endTime;
    private final double accrualFactor;
    private final double sensitivity;

    private IndexSensitivity(
        LocalDate startDate,
        double startTime,
        LocalDate endDate,
        double endTime,
        double accrualFactor,
        double sensitivity) {

      this.startDate = startDate;
      this.startTime = startTime;
      this.endDate = endDate;
      this.endTime = endTime;
      this.accrualFactor = accrualFactor;
      this.sensitivity = sensitivity;
    }

    private double derivativeToYieldStart(double dicountfactorStart, double dicountfactorEnd) {
      return -startTime * dicountfactorStart / (dicountfactorEnd * accrualFactor);
    }

    private double derivativeToYieldEnd(double dicountfactorStart, double dicountfactorEnd) {
      return endTime * dicountfactorStart / (dicountfactorEnd * accrualFactor);
    }
  }

}
