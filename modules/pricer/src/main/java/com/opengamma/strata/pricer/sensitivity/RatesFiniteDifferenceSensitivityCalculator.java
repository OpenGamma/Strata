/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.pricer.ImmutableRatesProvider;

/**
 * Computes the curve parameter sensitivity by finite difference.
 * <p>
 * This is based on an {@link ImmutableRatesProvider}, and calculates the sensitivity by finite difference.
 * The curves underlying the rates provider should be {@link YieldCurve} based on {@link InterpolatedDoublesCurve}.
 */
public class RatesFiniteDifferenceSensitivityCalculator {

  /**
   * Default implementation. The shift is one basis point (0.0001).
   */
  public static final RatesFiniteDifferenceSensitivityCalculator DEFAULT =
      new RatesFiniteDifferenceSensitivityCalculator(1.0E-4);

  /**
   * The shift used for finite difference.
   */
  private final double shift;

  /**
   * Create an instance of the finite difference calculator.
   * @param shift  the shift used in the finite difference computation
   */
  public RatesFiniteDifferenceSensitivityCalculator(double shift) {
    this.shift = shift;
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the first order sensitivities of a function of a RatesProvider to a double by finite difference.
   * <p>
   * The curves underlying the rates provider should be {@link YieldCurve} based on {@link InterpolatedDoublesCurve}.
   * The finite difference is computed by forward type. 
   * The function should return a value in the same currency for any rate provider.
   * 
   * @param provider  the rates provider
   * @param valueFn  the function from a rate provider to a currency amount for which the sensitivity should be computed
   * @return the sensitivity with the {@link SensitivityKey} containing the curves names.
   */
  public CurveParameterSensitivity sensitivity(
      ImmutableRatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn) {

    CurrencyAmount valueInit = valueFn.apply(provider);
    CurveParameterSensitivity result = sensitivityDiscounting(provider, valueFn, valueInit);
    return result.combinedWith(sensitivityForward(provider, valueFn, valueInit));
  }

  // check that the curve is yield curve and the underlying is an InterpolatedDoublesCurve and returns the last
  private InterpolatedDoublesCurve checkInterpolated(YieldAndDiscountCurve curve) {
    ArgChecker.isTrue(curve instanceof YieldCurve, "Curve should be a YieldCurve");
    YieldCurve curveYield = (YieldCurve) curve;
    ArgChecker.isTrue(curveYield.getCurve() instanceof InterpolatedDoublesCurve,
        "Yield curve should be based on InterpolatedDoublesCurve");
    return (InterpolatedDoublesCurve) curveYield.getCurve();
  }

  // create a YieldCurve by bumping an InterpolatedDoublesCurve at a given parameter
  private YieldCurve bumpedCurve(InterpolatedDoublesCurve curveInt, int loopnode) {
    double[] yieldBumped = curveInt.getYDataAsPrimitive().clone();
    yieldBumped[loopnode] += shift;
    return new YieldCurve(curveInt.getName(),
        new InterpolatedDoublesCurve(curveInt.getXDataAsPrimitive(), yieldBumped, curveInt.getInterpolator(), true));
  }

  // computes the sensitivity with respect to the discounting curves
  private CurveParameterSensitivity sensitivityDiscounting(
      ImmutableRatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn,
      CurrencyAmount valueInit) {

    CurveParameterSensitivity result = CurveParameterSensitivity.empty();
    ImmutableMap<Currency, YieldAndDiscountCurve> mapCurrency = provider.getDiscountCurves();
    for (Entry<Currency, YieldAndDiscountCurve> entry : mapCurrency.entrySet()) {
      InterpolatedDoublesCurve curveInt = checkInterpolated(entry.getValue());
      int nbNodePoint = curveInt.getXDataAsPrimitive().length;
      double[] sensitivity = new double[nbNodePoint];
      for (int loopnode = 0; loopnode < nbNodePoint; loopnode++) {
        YieldAndDiscountCurve dscBumped = bumpedCurve(curveInt, loopnode);
        Map<Currency, YieldAndDiscountCurve> mapBumped = new HashMap<>(mapCurrency);
        mapBumped.put(entry.getKey(), dscBumped);
        ImmutableRatesProvider providerDscBumped = provider.toBuilder().discountCurves(mapBumped).build();
        sensitivity[loopnode] = (valueFn.apply(providerDscBumped).getAmount() - valueInit.getAmount()) / shift;
      }
      String name = entry.getValue().getName();
      result = result.combinedWith(NameCurrencySensitivityKey.of(name, valueInit.getCurrency()), sensitivity);
    }
    return result;
  }

  // computes the sensitivity with respect to the forward curves
  private CurveParameterSensitivity sensitivityForward(
      ImmutableRatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn,
      CurrencyAmount valueInit) {

    CurveParameterSensitivity result = CurveParameterSensitivity.empty();
    ImmutableMap<Index, YieldAndDiscountCurve> mapIndex = provider.getIndexCurves();
    for (Entry<Index, YieldAndDiscountCurve> entry : mapIndex.entrySet()) {
      InterpolatedDoublesCurve curveInt = checkInterpolated(entry.getValue());
      int nbNodePoint = curveInt.getXDataAsPrimitive().length;
      double[] sensitivity = new double[nbNodePoint];
      for (int loopnode = 0; loopnode < nbNodePoint; loopnode++) {
        YieldAndDiscountCurve indexBumped = bumpedCurve(curveInt, loopnode);
        Map<Index, YieldAndDiscountCurve> mapBumped = new HashMap<>(mapIndex);
        mapBumped.put(entry.getKey(), indexBumped);
        ImmutableRatesProvider providerFwdBumped = provider.toBuilder().indexCurves(mapBumped).build();
        sensitivity[loopnode] = (valueFn.apply(providerFwdBumped).getAmount() - valueInit.getAmount()) / shift;
      }
      String name = entry.getValue().getName();
      result = result.combinedWith(NameCurrencySensitivityKey.of(name, valueInit.getCurrency()), sensitivity);
    }
    return result;
  }

}
