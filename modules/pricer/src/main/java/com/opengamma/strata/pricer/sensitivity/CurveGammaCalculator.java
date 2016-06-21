/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.util.function.Function;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.ParallelShiftedCurve;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;

/**
 * Computes the cross-gamma and related figures to the rate curves parameters for rates provider.
 * <p>
 * This implementation supports a single {@link Curve} on the zero-coupon rates.
 * By default the gamma is computed using a one basis-point shift and a forward finite difference.
 * The results themselves are not scaled (they represent the second order derivative).
 * <p>
 * Reference: Interest Rate Cross-gamma for Single and Multiple Curves. OpenGamma quantitative research 15, July 14
 */
public class CurveGammaCalculator {

  /**
   * Default implementation. Finite difference is forward and the shift is one basis point (0.0001).
   */
  public static final CurveGammaCalculator DEFAULT = new CurveGammaCalculator(FiniteDifferenceType.FORWARD, 1.0E-4);

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
  public static CurveGammaCalculator ofForwardDifference(double shift) {
    return new CurveGammaCalculator(FiniteDifferenceType.FORWARD, shift);
  }

  /**
   * Obtains an instance of the finite difference calculator using central differencing.
   * 
   * @param shift  the shift to be applied to the curves
   * @return the calculator
   */
  public static CurveGammaCalculator ofCentralDifference(double shift) {
    return new CurveGammaCalculator(FiniteDifferenceType.CENTRAL, shift);
  }

  /**
   * Obtains an instance of the finite difference calculator using backward differencing.
   * 
   * @param shift  the shift to be applied to the curves
   * @return the calculator
   */
  public static CurveGammaCalculator ofBackwardDifference(double shift) {
    return new CurveGammaCalculator(FiniteDifferenceType.BACKWARD, shift);
  }

  //-------------------------------------------------------------------------
  /**
   * Create an instance of the finite difference calculator.
   * 
   * @param fdType  the finite difference type
   * @param shift  the shift to be applied to the curves
   */
  private CurveGammaCalculator(FiniteDifferenceType fdType, double shift) {
    this.fd = new VectorFieldFirstOrderDifferentiator(fdType, shift);
  }

  //-------------------------------------------------------------------------
  /**
   * Computes the "sum-of-column gamma" or "semi-parallel gamma" for a sensitivity function.
   * <p>
   * See the class-level documentation for the definition.
   * <p>
   * This implementation only supports a single curve.
   * 
   * @param curve  the single curve to be bumped
   * @param curveCurrency  the currency of the curve and resulting sensitivity
   * @param sensitivitiesFn  the function to convert the bumped curve to parameter sensitivities
   * @return the "sum-of-columns" or "semi-parallel" gamma vector
   */
  public CurrencyParameterSensitivity calculateSemiParallelGamma(
      Curve curve,
      Currency curveCurrency,
      Function<Curve, CurrencyParameterSensitivity> sensitivitiesFn) {

    Delta deltaShift = new Delta(curve, sensitivitiesFn);
    Function<DoubleArray, DoubleMatrix> gammaFn = fd.differentiate(deltaShift);
    DoubleArray gamma = gammaFn.apply(DoubleArray.filled(1)).column(0);
    return curve.createParameterSensitivity(curveCurrency, gamma);
  }

  //-------------------------------------------------------------------------
  /**
   * Inner class to compute the delta for a given parallel shift of the curve.
   */
  static class Delta implements Function<DoubleArray, DoubleArray> {
    private final Curve curve;
    private final Function<Curve, CurrencyParameterSensitivity> sensitivitiesFn;

    Delta(Curve curve, Function<Curve, CurrencyParameterSensitivity> sensitivitiesFn) {
      this.curve = curve;
      this.sensitivitiesFn = sensitivitiesFn;
    }

    @Override
    public DoubleArray apply(DoubleArray s) {
      double shift = s.get(0);
      Curve curveBumped = ParallelShiftedCurve.absolute(curve, shift);
      CurrencyParameterSensitivity pts = sensitivitiesFn.apply(curveBumped);
      return pts.getSensitivity();
    }
  }

}
