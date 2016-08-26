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
 * Test {@link MultiCurveCrossGammaCalculator}.
 */
@Test
public class MultiCurveCrossGammaCalculatorTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double EPS = 1.0e-6;
  private static final MultiCurveCrossGammaCalculator FORWARD =
      MultiCurveCrossGammaCalculator.ofForwardDifference(EPS * 0.1);
  private static final MultiCurveCrossGammaCalculator CENTRAL =
      MultiCurveCrossGammaCalculator.ofCentralDifference(EPS);
  private static final MultiCurveCrossGammaCalculator BACKWARD =
      MultiCurveCrossGammaCalculator.ofBackwardDifference(EPS * 0.1);

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
          double expected = 8d * times.get(i) * times.get(j);
          assertEquals(s.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS);
        }
      }
    }
  }

  public void sensitivity_multi_curve() {
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
        double expected = 4d * times1.get(i) * times1.get(j);
        assertEquals(s1.get(i, j), expected, Math.max(Math.abs(expected), 1d) * EPS);
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
  }

  //-------------------------------------------------------------------------
  private CurrencyParameterSensitivities sensiFn(ImmutableRatesProvider provider) {
    CurrencyParameterSensitivities sensi = CurrencyParameterSensitivities.empty();
    // Currency
    ImmutableMap<Currency, Curve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, Curve> entry : mapCurrency.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      double sumSqrt = sum(curveInt);
      sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curveInt.getName(), USD,
          DoubleArray.of(curveInt.getParameterCount(), i -> 2d * sumSqrt * curveInt.getXValues().get(i))));
    }
    // Index
    ImmutableMap<Index, Curve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, Curve> entry : mapIndex.entrySet()) {
      InterpolatedNodalCurve curveInt = checkInterpolated(entry.getValue());
      double sumSqrt = sum(curveInt);
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
        double sumSqrt = sum(curveInt);
        sensi = sensi.combinedWith(CurrencyParameterSensitivity.of(curveInt.getName(), USD,
            DoubleArray.of(curveInt.getParameterCount(), i -> 2d * sumSqrt * curveInt.getXValues().get(i))));
      }
    }
    return sensi;
  }

  private double sum(NodalCurve curveInt) {
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
