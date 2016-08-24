/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.IntStream;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndex;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.param.CrossGammaParameterSensitivities;
import com.opengamma.strata.market.param.CrossGammaParameterSensitivity;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.ParameterMetadata;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;

/**
 * Computes the cross-gamma to the curve parameters for multi-curve with all the curves in the same currency and instruments.
 * <p>
 * Reference: Interest Rate Cross-gamma for Single and Multiple Curves. OpenGamma Analysis 1, August 14
 */
public class MultiCurveCrossGammaCalculator {

  /**
   * Default implementation. Finite difference is forward and the shift is one basis point (0.0001).
   */
  public static final MultiCurveCrossGammaCalculator DEFAULT =
      new MultiCurveCrossGammaCalculator(FiniteDifferenceType.CENTRAL, 1.0E-6);

  /**
   * The first order finite difference calculator.
   */
  private final VectorFieldFirstOrderDifferentiator fd;

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance of the finite difference calculator using forward differencing.
   * 
   * @param shift  the shift to be applied to the curves
   * @return the calculator
   */
  public static MultiCurveCrossGammaCalculator ofForwardDifference(double shift) {
    return new MultiCurveCrossGammaCalculator(FiniteDifferenceType.FORWARD, shift);
  }

  /**
   * Obtains an instance of the finite difference calculator using central differencing.
   * 
   * @param shift  the shift to be applied to the curves
   * @return the calculator
   */
  public static MultiCurveCrossGammaCalculator ofCentralDifference(double shift) {
    return new MultiCurveCrossGammaCalculator(FiniteDifferenceType.CENTRAL, shift);
  }

  /**
   * Obtains an instance of the finite difference calculator using backward differencing.
   * 
   * @param shift  the shift to be applied to the curves
   * @return the calculator
   */
  public static MultiCurveCrossGammaCalculator ofBackwardDifference(double shift) {
    return new MultiCurveCrossGammaCalculator(FiniteDifferenceType.BACKWARD, shift);
  }

  //-------------------------------------------------------------------------
  /**
   * Create an instance of the finite difference calculator.
   * 
   * @param fdType  the finite difference type
   * @param shift  the shift to be applied to the curves
   */
  private MultiCurveCrossGammaCalculator(FiniteDifferenceType fdType, double shift) {
    this.fd = new VectorFieldFirstOrderDifferentiator(fdType, shift);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes intra-curve cross gamma by applying finite difference method to curve delta.
   * <p>
   * This computes the intra-curve cross gamma, i.e., the second order sensitivities to individual curves. 
   * Thus the sensitivity of a curve delta to another curve is not produced.
   * <p>
   * The sensitivities are computed for discount curves, and forward curves for {@code RateIndex} and {@code PriceIndex}. 
   * This implementation works only for single currency assets. 
   * 
   * @param ratesProvider  the rates provider
   * @param sensitivitiesFn  the sensitivity function
   * @return the cross gamma
   */
  public CrossGammaParameterSensitivities calculateCrossGammaIntraCurve(
      ImmutableRatesProvider ratesProvider,
      Function<ImmutableRatesProvider, CurrencyParameterSensitivities> sensitivitiesFn) {
    
    CurrencyParameterSensitivities baseDelta = sensitivitiesFn.apply(ratesProvider); // used to check target sensitivity exits
    CrossGammaParameterSensitivities result = CrossGammaParameterSensitivities.empty();
    // discount curve
    for (Entry<Currency, Curve> entry : ratesProvider.getDiscountCurves().entrySet()) {
      Currency currency = entry.getKey();
      Curve curve = entry.getValue();
      if (baseDelta.findSensitivity(curve.getName(), currency).isPresent()) {
        NodalCurve nodalCurve = getNodalCurve(curve);
        CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
            nodalCurve, currency, c -> ratesProvider.toBuilder().discountCurve(currency, c).build(), sensitivitiesFn);
        result = result.combinedWith(gammaSingle);
      }
    }
    // forward curve
    for (Entry<Index, Curve> entry : ratesProvider.getIndexCurves().entrySet()) {
      Index index = entry.getKey();
      if (index instanceof RateIndex || index instanceof PriceIndex) {
        Currency currency = getCurrency(index);
        Curve curve = entry.getValue();
        if (baseDelta.findSensitivity(curve.getName(), currency).isPresent()) {
          NodalCurve nodalCurve = getNodalCurve(curve);
          CrossGammaParameterSensitivity gammaSingle = computeGammaForCurve(
              nodalCurve, currency, c -> ratesProvider.toBuilder().indexCurve(index, c).build(), sensitivitiesFn);
          result = result.combinedWith(gammaSingle);
        }
      }
    }
    return result;
  }

  //-------------------------------------------------------------------------
  private NodalCurve getNodalCurve(Curve curve) {
    ArgChecker.isTrue(curve instanceof NodalCurve, "underlying curve must be NodalCurve");
    return (NodalCurve) curve;
  }

  private Currency getCurrency(Index index){
    if (index instanceof RateIndex){
      return ((RateIndex) index).getCurrency();
    } else if (index instanceof PriceIndex) {
      return ((PriceIndex) index).getCurrency();
    } 
    throw new IllegalArgumentException("unsupported index");
  }

  CrossGammaParameterSensitivity computeGammaForCurve(
      NodalCurve nodalCurve,
      Currency sensitivityCurrency,
      Function<Curve, ImmutableRatesProvider> ratesProviderFn,
      Function<ImmutableRatesProvider, CurrencyParameterSensitivities> sensitivitiesFn) {

    Function<DoubleArray, DoubleArray> function = new Function<DoubleArray, DoubleArray>() {
      @Override
      public DoubleArray apply(DoubleArray t) {
        NodalCurve newCurve = nodalCurve.withYValues(t);
        ImmutableRatesProvider newRates = ratesProviderFn.apply(newCurve);
        CurrencyParameterSensitivities sensiMulti = sensitivitiesFn.apply(newRates);
        return sensiMulti.getSensitivity(newCurve.getName(), sensitivityCurrency).getSensitivity();
      }
    };
    DoubleMatrix sensi = fd.differentiate(function).apply(nodalCurve.getYValues());
    List<ParameterMetadata> metadata = IntStream.range(0, nodalCurve.getParameterCount())
        .mapToObj(i -> nodalCurve.getParameterMetadata(i))
        .collect(toImmutableList());
    return CrossGammaParameterSensitivity.of(nodalCurve.getName(), metadata, sensitivityCurrency, sensi);
  }

}
