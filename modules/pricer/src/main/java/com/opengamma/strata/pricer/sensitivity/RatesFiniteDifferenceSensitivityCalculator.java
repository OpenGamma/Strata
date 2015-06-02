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

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.sensitivity.CurveParameterSensitivities;
import com.opengamma.strata.market.sensitivity.NameCurrencySensitivityKey;
import com.opengamma.strata.market.sensitivity.SensitivityKey;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Computes the curve parameter sensitivity by finite difference.
 * <p>
 * This is based on an {@link ImmutableRatesProvider}, and calculates the sensitivity by finite difference.
 * The curves underlying the rates provider must be of type {@link NodalCurve}.
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
   * The curves underlying the rates provider must be of type {@link NodalCurve}.
   * The finite difference is computed by forward type. 
   * The function should return a value in the same currency for any rate provider.
   * 
   * @param provider  the rates provider
   * @param valueFn  the function from a rate provider to a currency amount for which the sensitivity should be computed
   * @return the sensitivity with the {@link SensitivityKey} containing the curves names.
   */
  public CurveParameterSensitivities sensitivity(
      ImmutableRatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn) {

    CurrencyAmount valueInit = valueFn.apply(provider);
    CurveParameterSensitivities discounting = sensitivity(
        provider, valueFn, ImmutableRatesProvider.meta().discountCurves(), valueInit);
    CurveParameterSensitivities forward = sensitivity(
        provider, valueFn, ImmutableRatesProvider.meta().indexCurves(), valueInit);
    return discounting.combinedWith(forward);
  }

  // computes the sensitivity with respect to the curves
  private <T> CurveParameterSensitivities sensitivity(
      ImmutableRatesProvider provider,
      Function<ImmutableRatesProvider, CurrencyAmount> valueFn,
      MetaProperty<? extends Map<T, Curve>> metaProperty,
      CurrencyAmount valueInit) {

    Map<T, Curve> baseCurves = metaProperty.get(provider);
    CurveParameterSensitivities result = CurveParameterSensitivities.empty();
    for (Entry<T, Curve> entry : baseCurves.entrySet()) {
      NodalCurve curveInt = checkNodal(entry.getValue());
      int nbNodePoint = curveInt.getXValues().length;
      double[] sensitivity = new double[nbNodePoint];
      for (int i = 0; i < nbNodePoint; i++) {
        Curve dscBumped = bumpedCurve(curveInt, i);
        Map<T, Curve> mapBumped = new HashMap<>(baseCurves);
        mapBumped.put(entry.getKey(), dscBumped);
        ImmutableRatesProvider providerDscBumped = provider.toBuilder().set(metaProperty, mapBumped).build();
        sensitivity[i] = (valueFn.apply(providerDscBumped).getAmount() - valueInit.getAmount()) / shift;
      }
      CurveName name = entry.getValue().getName();
      result = result.combinedWith(NameCurrencySensitivityKey.of(name, valueInit.getCurrency()), sensitivity);
    }
    return result;
  }

  // check that the curve is a NodalCurve
  private NodalCurve checkNodal(Curve curve) {
    ArgChecker.isTrue(curve instanceof NodalCurve, "Curve must be a NodalCurve");
    return (NodalCurve) curve;
  }

  // create new curve by bumping the existing curve at a given parameter
  private NodalCurve bumpedCurve(NodalCurve curveInt, int loopnode) {
    double[] yieldBumped = curveInt.getYValues();
    yieldBumped[loopnode] += shift;
    return curveInt.withYValues(yieldBumped);
  }

}
