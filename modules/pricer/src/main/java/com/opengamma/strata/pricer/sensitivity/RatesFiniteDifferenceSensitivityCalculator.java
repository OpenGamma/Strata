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

import org.joda.beans.MetaProperty;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldAndDiscountCurve;
import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivity;
import com.opengamma.strata.market.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.SensitivityKey;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

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
   * 
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
    CurveParameterSensitivity discounting = sensitivity(
        provider, valueFn, ImmutableRatesProvider.meta().discountCurves(), valueInit);
    CurveParameterSensitivity forward = sensitivity(
        provider, valueFn, ImmutableRatesProvider.meta().indexCurves(), valueInit);
    return discounting.combinedWith(forward);
  }

  // computes the sensitivity with respect to the curves
  private <T> CurveParameterSensitivity sensitivity(
      ImmutableRatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn,
      MetaProperty<? extends Map<T, YieldAndDiscountCurve>> metaProperty,
      CurrencyAmount valueInit) {

    Map<T, YieldAndDiscountCurve> baseCurves = metaProperty.get(provider);
    CurveParameterSensitivity result = CurveParameterSensitivity.empty();
    for (Entry<T, YieldAndDiscountCurve> entry : baseCurves.entrySet()) {
      InterpolatedDoublesCurve curveInt = checkInterpolated(entry.getValue());
      int nbNodePoint = curveInt.getXDataAsPrimitive().length;
      double[] sensitivity = new double[nbNodePoint];
      for (int i = 0; i < nbNodePoint; i++) {
        YieldAndDiscountCurve dscBumped = bumpedCurve(curveInt, i);
        Map<T, YieldAndDiscountCurve> mapBumped = new HashMap<>(baseCurves);
        mapBumped.put(entry.getKey(), dscBumped);
        ImmutableRatesProvider providerDscBumped = provider.toBuilder().set(metaProperty, mapBumped).build();
        sensitivity[i] = (valueFn.apply(providerDscBumped).getAmount() - valueInit.getAmount()) / shift;
      }
      String name = entry.getValue().getName();
      result = result.combinedWith(NameCurrencySensitivityKey.of(name, valueInit.getCurrency()), sensitivity);
    }
    return result;
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

}
