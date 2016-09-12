/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.param.CrossGammaParameterSensitivities;
import com.opengamma.strata.market.param.CrossGammaParameterSensitivity;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.pricer.datasets.RatesProviderDataSets;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConventions;

/**
 * Test {@link CurveGammaCalculator} cross-gamma.
 */
@Test
public class CurveGammaCalculatorCrossGammaTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double EPS = 1.0e-6;
  private static final double TOL = 1.0e-14;
  private static final CurveGammaCalculator FORWARD =
      CurveGammaCalculator.ofForwardDifference(EPS * 0.1);
  private static final CurveGammaCalculator CENTRAL =
      CurveGammaCalculator.ofCentralDifference(EPS);
  private static final CurveGammaCalculator BACKWARD =
      CurveGammaCalculator.ofBackwardDifference(EPS * 0.1);

  public void sensitivity_single_curve() {
    CrossGammaParameterSensitivities forward =
        FORWARD.calculateCrossGammaIntraCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    CrossGammaParameterSensitivities central =
        CENTRAL.calculateCrossGammaIntraCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    CrossGammaParameterSensitivities backward =
        BACKWARD.calculateCrossGammaIntraCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    DoubleArray times = RatesProviderDataSets.TIMES_1;
    for (CrossGammaParameterSensitivities sensi : new CrossGammaParameterSensitivities[] {forward, central, backward}) {
      CurrencyParameterSensitivities diagonalComputed = sensi.diagonal();
      assertEquals(sensi.size(), 1);
      assertEquals(diagonalComputed.size(), 1);
      DoubleMatrix s = sensi.getSensitivities().get(0).getSensitivity();
      assertEquals(s.columnCount(), times.size());
      for (int i = 0; i < times.size(); i++) {
        for (int j = 0; j < times.size(); j++) {
          double expected = 32d * times.get(i) * times.get(j);
          assertEquals(s.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS);
        }
      }
    }
    // no difference for single curve
    CrossGammaParameterSensitivities forwardCross =
        FORWARD.calculateCrossGammaCrossCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    assertTrue(forward.equalWithTolerance(forwardCross, TOL));
    CrossGammaParameterSensitivities centralCross =
        CENTRAL.calculateCrossGammaCrossCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    assertTrue(central.equalWithTolerance(centralCross, TOL));
    CrossGammaParameterSensitivities backwardCross =
        BACKWARD.calculateCrossGammaCrossCurve(RatesProviderDataSets.SINGLE_USD, this::sensiFn);
    assertTrue(backward.equalWithTolerance(backwardCross, TOL));
  }

  public void sensitivity_intra_multi_curve() {
    CrossGammaParameterSensitivities sensiComputed =
        CENTRAL.calculateCrossGammaIntraCurve(RatesProviderDataSets.MULTI_CPI_USD, this::sensiFn);
    DoubleArray times1 = RatesProviderDataSets.TIMES_1;
    DoubleArray times2 = RatesProviderDataSets.TIMES_2;
    DoubleArray times3 = RatesProviderDataSets.TIMES_3;
    DoubleArray times4 = RatesProviderDataSets.TIMES_4;
    assertEquals(sensiComputed.size(), 4);
    DoubleMatrix s1 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD).getSensitivity();
    assertEquals(s1.columnCount(), times1.size());
    for (int i = 0; i < times1.size(); i++) {
      for (int j = 0; j < times1.size(); j++) {
        double expected = 8d * times1.get(i) * times1.get(j);
        assertEquals(s1.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS * 10d);
      }
    }
    DoubleMatrix s2 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertEquals(s2.columnCount(), times2.size());
    for (int i = 0; i < times2.size(); i++) {
      for (int j = 0; j < times2.size(); j++) {
        double expected = 2d * times2.get(i) * times2.get(j);
        assertEquals(s2.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS);
      }
    }
    DoubleMatrix s3 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    assertEquals(s3.columnCount(), times3.size());
    for (int i = 0; i < times3.size(); i++) {
      for (int j = 0; j < times3.size(); j++) {
        double expected = 2d * times3.get(i) * times3.get(j);
        assertEquals(s3.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS);
      }
    }
    DoubleMatrix s4 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD).getSensitivity();
    assertEquals(s4.columnCount(), times4.size());
    for (int i = 0; i < times4.size(); i++) {
      for (int j = 0; j < times4.size(); j++) {
        double expected = 2d * times4.get(i) * times4.get(j);
        assertEquals(s4.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS);
      }
    }
  }

  public void sensitivity_multi_curve_empty() {
    CrossGammaParameterSensitivities sensiComputed =
        CENTRAL.calculateCrossGammaIntraCurve(RatesProviderDataSets.MULTI_CPI_USD, this::sensiModFn);
    DoubleArray times2 = RatesProviderDataSets.TIMES_2;
    DoubleArray times3 = RatesProviderDataSets.TIMES_3;
    assertEquals(sensiComputed.size(), 2);
    DoubleMatrix s2 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertEquals(s2.columnCount(), times2.size());
    for (int i = 0; i < times2.size(); i++) {
      for (int j = 0; j < times2.size(); j++) {
        double expected = 2d * times2.get(i) * times2.get(j);
        assertEquals(s2.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS);
      }
    }
    DoubleMatrix s3 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    assertEquals(s3.columnCount(), times3.size());
    for (int i = 0; i < times3.size(); i++) {
      for (int j = 0; j < times3.size(); j++) {
        double expected = 2d * times3.get(i) * times3.get(j);
        assertEquals(s3.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS);
      }
    }
    Optional<CrossGammaParameterSensitivity> oisSensi =
        sensiComputed.findSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD);
    assertFalse(oisSensi.isPresent());
    Optional<CrossGammaParameterSensitivity> priceIndexSensi =
        sensiComputed.findSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD);
    assertFalse(priceIndexSensi.isPresent());
  }

  public void sensitivity_cross_multi_curve() {
    CrossGammaParameterSensitivities sensiComputed =
        CENTRAL.calculateCrossGammaCrossCurve(RatesProviderDataSets.MULTI_CPI_USD, this::sensiFn);
    DoubleArray times1 = RatesProviderDataSets.TIMES_1;
    DoubleArray times2 = RatesProviderDataSets.TIMES_2;
    DoubleArray times3 = RatesProviderDataSets.TIMES_3;
    DoubleArray times4 = RatesProviderDataSets.TIMES_4;
    int paramsTotal = times1.size() + times2.size() + times3.size() + times4.size();
    double[] timesTotal = new double[paramsTotal];
    DoubleArray times1Twice = times1.multipliedBy(2d);
    System.arraycopy(times4.toArray(), 0, timesTotal, 0, times4.size());
    System.arraycopy(times1Twice.toArray(), 0, timesTotal, times4.size(), times1.size());
    System.arraycopy(times2.toArray(), 0, timesTotal, times1.size() + times4.size(), times2.size());
    System.arraycopy(times3.toArray(), 0, timesTotal, times1.size() + times2.size() + times4.size(), times3.size());

    assertEquals(sensiComputed.size(), 4);
    DoubleMatrix s1 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD).getSensitivity();
    assertEquals(s1.columnCount(), paramsTotal);
    for (int i = 0; i < times1.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 4d * times1.get(i) * timesTotal[j];
        assertEquals(s1.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS * 10d);
      }
    }
    DoubleMatrix s2 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertEquals(s2.columnCount(), paramsTotal);
    for (int i = 0; i < times2.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times2.get(i) * timesTotal[j];
        assertEquals(s2.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS * 10d);
      }
    }
    DoubleMatrix s3 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    assertEquals(s3.columnCount(), paramsTotal);
    for (int i = 0; i < times3.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times3.get(i) * timesTotal[j];
        assertEquals(s3.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS * 10d);
      }
    }
    DoubleMatrix s4 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD).getSensitivity();
    assertEquals(s4.columnCount(), paramsTotal);
    for (int i = 0; i < times4.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times4.get(i) * timesTotal[j];
        assertEquals(s4.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS * 20d);
      }
    }
  }

  public void sensitivity_cross_multi_curve_empty() {
    CrossGammaParameterSensitivities sensiComputed =
        CENTRAL.calculateCrossGammaCrossCurve(RatesProviderDataSets.MULTI_CPI_USD, this::sensiModFn);
    DoubleArray times2 = RatesProviderDataSets.TIMES_2;
    DoubleArray times3 = RatesProviderDataSets.TIMES_3;
    int paramsTotal = times2.size() + times3.size();
    double[] timesTotal = new double[paramsTotal];
    System.arraycopy(times2.toArray(), 0, timesTotal, 0, times2.size());
    System.arraycopy(times3.toArray(), 0, timesTotal, times2.size(), times3.size());
    assertEquals(sensiComputed.size(), 2);
    DoubleMatrix s2 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L3_NAME, USD).getSensitivity();
    assertEquals(s2.columnCount(), paramsTotal);
    for (int i = 0; i < times2.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times2.get(i) * timesTotal[j];
        assertEquals(s2.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS);
      }
    }
    DoubleMatrix s3 = sensiComputed.getSensitivity(RatesProviderDataSets.USD_L6_NAME, USD).getSensitivity();
    assertEquals(s3.columnCount(), paramsTotal);
    for (int i = 0; i < times3.size(); i++) {
      for (int j = 0; j < paramsTotal; j++) {
        double expected = 2d * times3.get(i) * timesTotal[j];
        assertEquals(s3.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS);
      }
    }
    Optional<CrossGammaParameterSensitivity> oisSensi =
        sensiComputed.findSensitivity(RatesProviderDataSets.USD_DSC_NAME, USD);
    assertFalse(oisSensi.isPresent());
    Optional<CrossGammaParameterSensitivity> priceIndexSensi =
        sensiComputed.findSensitivity(RatesProviderDataSets.USD_CPI_NAME, USD);
    assertFalse(priceIndexSensi.isPresent());
  }

  // test diagonal part against finite difference approximation computed from pv
  public void swap_exampleTest() {
    LocalDate start = LocalDate.of(2014, 3, 10);
    LocalDate end = LocalDate.of(2021, 3, 10);
    double notional = 1.0e6;
    ResolvedSwap swap = FixedIborSwapConventions.USD_FIXED_6M_LIBOR_3M
        .toTrade(RatesProviderDataSets.VAL_DATE_2014_01_22, start, end, BuySell.BUY, notional, 0.005)
        .getProduct()
        .resolve(REF_DATA);
    DiscountingSwapProductPricer pricer = DiscountingSwapProductPricer.DEFAULT;
    Function<ImmutableRatesProvider, CurrencyAmount> pvFunction = p -> pricer.presentValue(swap, USD, p);
    Function<ImmutableRatesProvider, CurrencyParameterSensitivities> sensiFunction = p -> {
      PointSensitivities sensi = pricer.presentValueSensitivity(swap, p).build();
      return p.parameterSensitivity(sensi);
    };
    CurrencyParameterSensitivities expected = sensitivityDiagonal(RatesProviderDataSets.MULTI_CPI_USD, pvFunction);
    CurrencyParameterSensitivities computed =
        CENTRAL.calculateCrossGammaIntraCurve(RatesProviderDataSets.MULTI_CPI_USD, sensiFunction).diagonal();
    assertTrue(computed.equalWithTolerance(expected, Math.sqrt(EPS) * notional));
    CurrencyParameterSensitivities computedFromCross =
        CENTRAL.calculateCrossGammaCrossCurve(RatesProviderDataSets.MULTI_CPI_USD, sensiFunction).diagonal();
    assertTrue(computed.equalWithTolerance(computedFromCross, TOL));
  }

  //-------------------------------------------------------------------------
  private CurrencyParameterSensitivities sensiFn(ImmutableRatesProvider provider) {
    CurrencyParameterSensitivities sensi = CurrencyParameterSensitivities.empty();
    // Currency
    ImmutableMap<Currency, Curve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, Curve> entry : mapCurrency.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      double sumSqrt = sum(provider);
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curveInt.getName(), USD,
          DoubleArray.of(curveInt.getParameterCount(), i -> 2d * sumSqrt * curveInt.getXValues().get(i))));
    }
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      double sumSqrt = sum(provider);
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curveInt.getName(), USD,
          DoubleArray.of(curveInt.getParameterCount(), i -> 2d * sumSqrt * curveInt.getXValues().get(i))));
    }
    return sensi;
  }

  // modified sensitivity function - sensitivities are computed only for ibor index curves
  private CurrencyParameterSensitivities sensiModFn(ImmutableRatesProvider provider) {
    CurrencyParameterSensitivities sensi = CurrencyParameterSensitivities.empty();
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      if (entry.getKey() instanceof IborIndex) {
        InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
        double sumSqrt = sumMod(provider);
        sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curveInt.getName(), USD,
            DoubleArray.of(curveInt.getParameterCount(), i -> 2d * sumSqrt * curveInt.getXValues().get(i))));
      }
    }
    return sensi;
  }

  private double sum(ImmutableRatesProvider provider) {
    double result = 0.0;
    // Currency
    ImmutableMap<Currency, Curve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, Curve> entry : mapCurrency.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      result += sumSingle(curveInt);
    }
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      result += sumSingle(curveInt);
    }
    return result;
  }

  private double sumMod(ImmutableRatesProvider provider) {
    double result = 0.0;
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      if (entry.getKey() instanceof IborIndex) {
        InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
        result += sumSingle(curveInt);
      }
    }
    return result;
  }

  private double sumSingle(NodalCurve curveInt) {
    double result = 0.0;
    DoubleArray x = curveInt.getXValues();
    DoubleArray y = curveInt.getYValues();
    int nbNodePoint = x.size();
    for (int i = 0; i < nbNodePoint; i++) {
      result += x.get(i) * y.get(i);
    }
    return result;
  }

  // check that the curve is InterpolatedNodalCurve
  private InterpolatedNodalCurve checkInterpolated(Curve curve) {
    ArgChecker.isTrue(curve instanceof InterpolatedNodalCurve, "Curve should be a InterpolatedNodalCurve");
    return (InterpolatedNodalCurve) curve;
  }

  //-------------------------------------------------------------------------
  // computes diagonal part
  private CurrencyParameterSensitivities sensitivityDiagonal(
      RatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn) {

    ImmutableRatesProvider immProv = provider.toImmutableRatesProvider();
    CurrencyAmount valueInit = valueFn.apply(immProv);
    CurrencyParameterSensitivities discounting = sensitivity(
        immProv,
        immProv.getDiscountCurves(),
        (base, bumped) -> base.toBuilder().discountCurves(bumped).build(),
        valueFn,
        valueInit);
    CurrencyParameterSensitivities forward = sensitivity(
        immProv,
        immProv.getIndexCurves(),
        (base, bumped) -> base.toBuilder().indexCurves(bumped).build(),
        valueFn,
        valueInit);
    return discounting.combinedWith(forward);
  }

  // computes the sensitivity with respect to the curves
  private <T> CurrencyParameterSensitivities sensitivity(
      ImmutableRatesProvider provider,
      Map<T, Curve> baseCurves,
      BiFunction<ImmutableRatesProvider, Map<T, Curve>, ImmutableRatesProvider> storeBumpedFn,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn,
      CurrencyAmount valueInit) {

    CurrencyParameterSensitivities result = CurrencyParameterSensitivities.empty();
    for (Entry<T, Curve> entry : baseCurves.entrySet()) {
      Curve curve = entry.getValue();
      DoubleArray sensitivity = DoubleArray.of(curve.getParameterCount(), i -> {
        Curve dscUp = curve.withParameter(i, curve.getParameter(i) + EPS);
        Curve dscDw = curve.withParameter(i, curve.getParameter(i) - EPS);
        HashMap<T, Curve> mapUp = new HashMap<>(baseCurves);
        HashMap<T, Curve> mapDw = new HashMap<>(baseCurves);
        mapUp.put(entry.getKey(), dscUp);
        mapDw.put(entry.getKey(), dscDw);
        ImmutableRatesProvider providerUp = storeBumpedFn.apply(provider, mapUp);
        ImmutableRatesProvider providerDw = storeBumpedFn.apply(provider, mapDw);
        return (valueFn.apply(providerUp).getAmount() + valueFn.apply(providerDw).getAmount() - 2d * valueInit.getAmount()) /
            EPS / EPS;
      });
      result = result.combinedWith(curve.createParameterSensitivity(valueInit.getCurrency(), sensitivity));
    }
    return result;
  }

}
