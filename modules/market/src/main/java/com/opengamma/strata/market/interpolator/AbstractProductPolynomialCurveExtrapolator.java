/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import java.io.Serializable;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.ProductPiecewisePolynomialInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle;

/**
 * Abstract extrapolator implementation that uses a polynomial function.
 */
abstract class AbstractProductPolynomialCurveExtrapolator
    implements CurveExtrapolator, Serializable {

  /**
   * The serialization version id.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The small value.
   */
  private static final double SMALL = 1e-14;

  /**
   * The function.
   * Subclasses must use {@code readResolve()} to restore this value.
   */
  private transient final PiecewisePolynomialFunction1D function;

  /**
   * Creates an instance.
   * 
   * @param function  the polynomial function
   */
  AbstractProductPolynomialCurveExtrapolator(PiecewisePolynomialFunction1D function) {
    this.function = function;
  }

  //-------------------------------------------------------------------------
  @Override
  public BoundCurveExtrapolator bind(DoubleArray xValues, DoubleArray yValues, BoundCurveInterpolator interpolator) {
    ArgChecker.isTrue(interpolator instanceof StandardBoundCurveInterpolator,
        "Interpolator must be StandardBoundCurveInterpolator");
    Interpolator1D interp = ((StandardBoundCurveInterpolator) interpolator).getInterpolator();
    Interpolator1DDataBundle data = ((StandardBoundCurveInterpolator) interpolator).getDataBundle();
    ArgChecker.isTrue(interp instanceof ProductPiecewisePolynomialInterpolator1D,
        "Interpolator must be ProductPiecewisePolynomialInterpolator1D");
    ArgChecker.isTrue(data instanceof Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle,
        "Interpolator data must be Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle");
    return new Bound(
        function,
        (ProductPiecewisePolynomialInterpolator1D) interp,
        (Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle) data);
  }

  //-------------------------------------------------------------------------
  /**
   * Bound extrapolator.
   */
  static class Bound implements BoundCurveExtrapolator {
    private final PiecewisePolynomialFunction1D function;
    private final ProductPiecewisePolynomialInterpolator1D underlyingInterpolator;
    private final Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle underlyingData;

    Bound(
        PiecewisePolynomialFunction1D function,
        ProductPiecewisePolynomialInterpolator1D interpolator,
        Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle data) {
      this.function = function;
      this.underlyingInterpolator = interpolator;
      this.underlyingData = data;
    }

    //-------------------------------------------------------------------------
    @Override
    public double leftExtrapolate(double xValue) {
      return underlyingInterpolator.interpolate(underlyingData, xValue, function, SMALL);
    }

    @Override
    public double leftExtrapolateFirstDerivative(double xValue) {
      return underlyingInterpolator.firstDerivative(underlyingData, xValue, function, SMALL);
    }

    @Override
    public DoubleArray leftExtrapolateParameterSensitivity(double xValue) {
      double[] sens = underlyingInterpolator.getNodeSensitivitiesForValue(underlyingData, xValue, function, SMALL);
      return DoubleArray.ofUnsafe(sens);
    }

    //-------------------------------------------------------------------------
    @Override
    public double rightExtrapolate(double xValue) {
      return underlyingInterpolator.interpolate(underlyingData, xValue, function, SMALL);
    }

    @Override
    public double rightExtrapolateFirstDerivative(double xValue) {
      return underlyingInterpolator.firstDerivative(underlyingData, xValue, function, SMALL);
    }

    @Override
    public DoubleArray rightExtrapolateParameterSensitivity(double xValue) {
      double[] sens = underlyingInterpolator.getNodeSensitivitiesForValue(underlyingData, xValue, function, SMALL);
      return DoubleArray.ofUnsafe(sens);
    }
  }

}
