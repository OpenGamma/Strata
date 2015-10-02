/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;
import com.opengamma.strata.math.impl.interpolation.data.ArrayInterpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;

/**
 * Wrapping {@link ProductPiecewisePolynomialInterpolator}
 */
public class ProductPiecewisePolynomialInterpolator1D extends Interpolator1D {
  private static final long serialVersionUID = 1L;
  private static final PiecewisePolynomialFunction1D FUNC = new PiecewisePolynomialFunction1D();
  private final ProductPiecewisePolynomialInterpolator _interp;
  private static final double SMALL = 1e-14;

  /**
   * Construct {@link ProductPiecewisePolynomialInterpolator}
   * @param baseInterpolator The base interpolator
   */
  public ProductPiecewisePolynomialInterpolator1D(PiecewisePolynomialInterpolator baseInterpolator) {
    _interp = new ProductPiecewisePolynomialInterpolator(baseInterpolator);
  }

  /**
   * Construct {@link ProductPiecewisePolynomialInterpolator}
   * @param baseInterpolator The base interpolator
   * @param xValuesClamped X values of the clamped points
   * @param yValuesClamped Y values of the clamped points
   */
  public ProductPiecewisePolynomialInterpolator1D(PiecewisePolynomialInterpolator baseInterpolator,
      double[] xValuesClamped, double[] yValuesClamped) {
    _interp = new ProductPiecewisePolynomialInterpolator(baseInterpolator, xValuesClamped, yValuesClamped);
  }

  /**
   * {@inheritDoc}
   * For small Math.abs(value), this method returns the exact value if clamped at (0,0), 
   * otherwise this returns a reference value
   */
  @Override
  public Double interpolate(Interpolator1DDataBundle data, Double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle,
        "data should be instance of Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle");
    Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle polyData = (Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle) data;
    return interpolate(polyData, value, FUNC, SMALL);
  }

  /**
   * {@inheritDoc}
   * For small Math.abs(value), this method returns the exact value if clamped at (0,0), 
   * otherwise this returns a reference value
   */
  @Override
  public double firstDerivative(final Interpolator1DDataBundle data, final Double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle,
        "data should be instance of Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle");
    Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle polyData = (Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle) data;
    return firstDerivative(polyData, value, FUNC, SMALL);
  }

  /**
   * {@inheritDoc}
   * For small Math.abs(value), this method returns the exact value if clamped at (0,0), 
   * otherwise this returns a reference value
   */
  @Override
  public double[] getNodeSensitivitiesForValue(final Interpolator1DDataBundle data, final Double value) {
    ArgChecker.notNull(value, "value");
    ArgChecker.notNull(data, "data bundle");
    ArgChecker.isTrue(data instanceof Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle,
        "data should be instance of Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle");
    Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle polyData = (Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle) data;
    return getNodeSensitivitiesForValue(polyData, value, FUNC, SMALL);
  }

  /**
   * Compute interpolation for product piecewise polynomials
   * @param data Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle
   * @param value The key value
   * @param function The piecewise polynomial function
   * @param small Threshold around the origin
   * @return The interpolation
   */
  Double interpolate(Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle data, Double value,
      PiecewisePolynomialFunction1D function, double small) {
    if (Math.abs(value) < small) {
      return function.differentiate(data.getPiecewisePolynomialResult(), value).getEntry(0);
    }
    DoubleMatrix1D res = function.evaluate(data.getPiecewisePolynomialResult(), value);
    return res.getEntry(0) / value;
  }

  /**
   * Compute first derivative value for product piecewise polynomials
   * @param data Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle
   * @param value The key value
   * @param function The piecewise polynomial function
   * @param small Threshold around the origin
   * @return The first derivative value
   */
  double firstDerivative(Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle data, Double value,
      PiecewisePolynomialFunction1D function, double small) {
    if (Math.abs(value) < small) {
      return 0.5 * function.differentiateTwice(data.getPiecewisePolynomialResult(), value).getEntry(0);
    }
    DoubleMatrix1D resValue = function.evaluate(data.getPiecewisePolynomialResult(), value);
    DoubleMatrix1D resDerivative = function.differentiate(data.getPiecewisePolynomialResult(), value);
    return resDerivative.getEntry(0) / value - resValue.getEntry(0) / value / value;
  }

  /**
   * Compute node sensitivities for product piecewise polynomials
   * @param data Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle
   * @param value The key value
   * @param function The piecewise polynomial function
   * @param small Threshold around the origin
   * @return The node sensitivities
   */
  double[] getNodeSensitivitiesForValue(Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle data, Double value,
      PiecewisePolynomialFunction1D function, double small) {
    int nData = data.size();
    double[] res = new double[nData];
    double eps = data.getEps();
    double smallDiff = data.getSmall();
    if (Math.abs(value) < small) {
      for (int i = 0; i < nData; ++i) {
        double den = Math.abs(data.getValues()[i]) < smallDiff ? eps : data.getValues()[i] * eps;
        double up = function.differentiate(data.getPiecewisePolynomialResultUp()[i], value).getData()[0];
        double dw = function.differentiate(data.getPiecewisePolynomialResultDw()[i], value).getData()[0];
        res[i] = 0.5 * (up - dw) / den;
      }
    } else {
      for (int i = 0; i < nData; ++i) {
        double den = Math.abs(data.getValues()[i]) < smallDiff ? eps : data.getValues()[i] * eps;
        double up = function.evaluate(data.getPiecewisePolynomialResultUp()[i], value).getData()[0];
        double dw = function.evaluate(data.getPiecewisePolynomialResultDw()[i], value).getData()[0];
        res[i] = 0.5 * (up - dw) / den / value;
      }
    }
    return res;
  }

  @Override
  protected double[] getFiniteDifferenceSensitivities(final Interpolator1DDataBundle data, final Double value) {
    throw new IllegalArgumentException("Use the method, getNodeSensitivitiesForValue");
  }

  @Override
  public Interpolator1DDataBundle getDataBundle(final double[] x, final double[] y) {
    return new Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle(new ArrayInterpolator1DDataBundle(x, y, false),
        _interp);
  }

  @Override
  public Interpolator1DDataBundle getDataBundleFromSortedArrays(final double[] x, final double[] y) {
    return new Interpolator1DPiecewisePoynomialWithExtraKnotsDataBundle(new ArrayInterpolator1DDataBundle(x, y, true),
        _interp);
  }

}
