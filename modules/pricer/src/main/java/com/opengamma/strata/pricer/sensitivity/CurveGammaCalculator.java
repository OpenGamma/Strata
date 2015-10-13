/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.sensitivity;

import java.util.function.Function;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.market.curve.NodalCurve;
import com.opengamma.strata.market.sensitivity.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;
import com.opengamma.strata.math.impl.differentiation.VectorFieldFirstOrderDifferentiator;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Computes the cross-gamma and related figures to the rate curves parameters for rates provider.
 * <p>
 * This implementation supports a single {@link NodalCurve} on the zero-coupon rates.
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

  /**
   * Create an instance of the finite difference calculator.
   * 
   * @param fdType  the finite difference type
   * @param shift  the shift to be applied to the curves
   */
  public CurveGammaCalculator(FiniteDifferenceType fdType, double shift) {
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
  public CurveCurrencyParameterSensitivity calculateSemiParallelGamma(
      NodalCurve curve,
      Currency curveCurrency,
      Function<NodalCurve, CurveCurrencyParameterSensitivity> sensitivitiesFn) {

    Delta deltaShift = new Delta(curve, sensitivitiesFn);
    Function1D<DoubleMatrix1D, DoubleMatrix2D> gammaFn = fd.differentiate(deltaShift);
    double[][] gamma2 = gammaFn.evaluate(new DoubleMatrix1D(new double[1])).getData();
    double[] gamma = new double[gamma2.length];
    for (int i = 0; i < gamma2.length; i++) {
      gamma[i] = gamma2[i][0];
    }
    return CurveCurrencyParameterSensitivity.of(curve.getMetadata(), curveCurrency, gamma);
  }

  //-------------------------------------------------------------------------
  /**
   * Inner class to compute the delta for a given parallel shift of the curve.
   */
  static class Delta extends Function1D<DoubleMatrix1D, DoubleMatrix1D> {
    private final NodalCurve curve;
    private final Function<NodalCurve, CurveCurrencyParameterSensitivity> sensitivitiesFn;

    Delta(NodalCurve curve, Function<NodalCurve, CurveCurrencyParameterSensitivity> sensitivitiesFn) {
      this.curve = curve;
      this.sensitivitiesFn = sensitivitiesFn;
    }

    @Override
    public DoubleMatrix1D evaluate(DoubleMatrix1D s) {
      double shift = s.get(0);
      double[] yieldBumped = curve.getYValues();
      for (int loopnode = 0; loopnode < curve.getParameterCount(); loopnode++) {
        yieldBumped[loopnode] += shift;
      }
      NodalCurve curveBumped = curve.withYValues(yieldBumped);
      CurveCurrencyParameterSensitivity pts = sensitivitiesFn.apply(curveBumped);
      return new DoubleMatrix1D(pts.getSensitivity());
    }
  }

}
